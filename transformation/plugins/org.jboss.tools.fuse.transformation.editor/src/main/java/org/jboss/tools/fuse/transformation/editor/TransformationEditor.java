/******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: JBoss by Red Hat - Initial implementation.
 *****************************************************************************/
package org.jboss.tools.fuse.transformation.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.model.Resource;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.fusesource.ide.camel.model.catalog.CamelModelFactory;
import org.jboss.tools.fuse.transformation.MappingOperation;
import org.jboss.tools.fuse.transformation.camel.CamelEndpoint;
import org.jboss.tools.fuse.transformation.editor.function.Function;
import org.jboss.tools.fuse.transformation.editor.internal.MappingDetailViewer;
import org.jboss.tools.fuse.transformation.editor.internal.MappingsViewer;
import org.jboss.tools.fuse.transformation.editor.internal.PotentialDropTarget;
import org.jboss.tools.fuse.transformation.editor.internal.SourceTabFolder;
import org.jboss.tools.fuse.transformation.editor.internal.TargetTabFolder;
import org.jboss.tools.fuse.transformation.editor.internal.util.JavaUtil;
import org.jboss.tools.fuse.transformation.editor.internal.util.TransformationConfig;
import org.jboss.tools.fuse.transformation.editor.internal.util.Util;
import org.jboss.tools.fuse.transformation.editor.internal.util.Util.Colors;
import org.jboss.tools.fuse.transformation.editor.internal.util.Util.Images;

/**
 *
 */
public class TransformationEditor extends EditorPart implements ISaveablePart2, IResourceChangeListener {

    private static final int SASH_WIDTH = 3;

    private static final String PREFERENCE_PREFIX = TransformationEditor.class.getName() + ".";

    private static final String SOURCE_VIEWER_PREFERENCE = PREFERENCE_PREFIX + "sourceViewer";
    private static final String TARGET_VIEWER_PREFERENCE = PREFERENCE_PREFIX + "targetViewer";

    private static final String HORIZONTAL_SPLITTER_PREFIX = PREFERENCE_PREFIX + "horizontalSplitterWeight.";
    private static final String VERTICAL_SPLITTER_PREFIX = PREFERENCE_PREFIX + "verticalSplitterWeight.";
    private static final String HORIZONTAL_SPLITTER_WEIGHT_LEFT_PREFERENCE = HORIZONTAL_SPLITTER_PREFIX + "left";
    private static final String HORIZONTAL_SPLITTER_WEIGHT_CENTER_PREFERENCE = HORIZONTAL_SPLITTER_PREFIX + "center";
    private static final String HORIZONTAL_SPLITTER_WEIGHT_RIGHT_PREFERENCE = HORIZONTAL_SPLITTER_PREFIX + "right";
    private static final String VERTICAL_SPLITTER_WEIGHT_TOP_PREFERENCE = VERTICAL_SPLITTER_PREFIX + "top";
    private static final String VERTICAL_SPLITTER_WEIGHT_BOTTOM_PREFERENCE = VERTICAL_SPLITTER_PREFIX + "bottom";

    private static final String VERSION_PREFERENCE = PREFERENCE_PREFIX + "version";

    TransformationConfig config;
    URLClassLoader loader;
    File camelConfigFile;
    CamelEndpoint camelEndpoint;

    MappingsViewer mappingsViewer;
    Text helpText;
    SourceTabFolder sourceTabFolder;
    TargetTabFolder targetTabFolder;
    MappingDetailViewer mappingDetailViewer;
    ToolItem sourceViewerButton;
    ToolItem targetViewerButton;

    final List<PotentialDropTarget> potentialDropTargets = new ArrayList<>();

    /**
     * creates a new editor instance
     */
    public TransformationEditor() {
        super();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    protected void closeEditorsWithoutValidInput() {
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                // close all editors without valid input
                IEditorReference[] refs = getSite().getPage().getEditorReferences();
                for (IEditorReference ref : refs) {
                    IEditorPart editor = ref.getEditor(false);
                    if (editor != null) {
                        IEditorInput editorInput = editor.getEditorInput();
                        if (editorInput instanceof FileEditorInput
                            && !((FileEditorInput)editorInput).getFile().exists()) {
                            getSite().getPage().closeEditor(editor, false);
                            editor.dispose();
                        }
                    }
                }
            }
        });
    }

    void configEvent() {
        firePropertyChange(IEditorPart.PROP_DIRTY);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(final Composite parent) {
        final IPreferenceStore prefs = Activator.plugin().getPreferenceStore();
        prefs.setDefault(SOURCE_VIEWER_PREFERENCE, true);
        prefs.setDefault(TARGET_VIEWER_PREFERENCE, true);
        prefs.setDefault(HORIZONTAL_SPLITTER_WEIGHT_LEFT_PREFERENCE, 33);
        prefs.setDefault(HORIZONTAL_SPLITTER_WEIGHT_CENTER_PREFERENCE, 34);
        prefs.setDefault(HORIZONTAL_SPLITTER_WEIGHT_RIGHT_PREFERENCE, 33);
        prefs.setDefault(VERTICAL_SPLITTER_WEIGHT_TOP_PREFERENCE, 75);
        prefs.setDefault(VERTICAL_SPLITTER_WEIGHT_BOTTOM_PREFERENCE, 25);

        final SashForm verticalSplitter = new SashForm(parent, SWT.VERTICAL);
        verticalSplitter.setBackground(Colors.SASH);
        verticalSplitter.setSashWidth(SASH_WIDTH);
        final Composite pane = new Composite(verticalSplitter, SWT.NONE);
        pane.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).create());
        pane.setBackground(Colors.BACKGROUND);
        // Create source model toggle button
        ToolBar toolBar = new ToolBar(pane, SWT.NONE);
        toolBar.setLayoutData(GridDataFactory.swtDefaults()
                                             .align(SWT.BEGINNING, SWT.BOTTOM)
                                             .create());
        sourceViewerButton = new ToolItem(toolBar, SWT.CHECK);
        sourceViewerButton.setImage(Images.TREE);
        // Create help text
        helpText = new Text(pane, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
        helpText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        helpText.setBackground(pane.getBackground());
        // Create target model toggle button
        toolBar = new ToolBar(pane, SWT.NONE);
        toolBar.setLayoutData(GridDataFactory.swtDefaults()
                                             .align(SWT.END, SWT.BOTTOM)
                                             .create());
        targetViewerButton = new ToolItem(toolBar, SWT.CHECK);
        targetViewerButton.setImage(Images.TREE);
        // Create splitter between mappings viewer and model viewers
        final SashForm horizontalSplitter = new SashForm(pane, SWT.HORIZONTAL);
        horizontalSplitter.setLayoutData(GridDataFactory.fillDefaults()
                                                        .span(3, 1)
                                                        .grab(true, true)
                                                        .create());
        horizontalSplitter.setBackground(Colors.SASH);
        horizontalSplitter.setSashWidth(SASH_WIDTH);
        // Create source tab folder
        sourceTabFolder = new SourceTabFolder(config, horizontalSplitter, potentialDropTargets);
        sourceTabFolder.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent event) {
                updateHelpText();
            }
        });
        // Create transformation viewer
        mappingsViewer = new MappingsViewer(config, this, horizontalSplitter, potentialDropTargets);
        // Create target tab folder
        targetTabFolder = new TargetTabFolder(config, horizontalSplitter, potentialDropTargets);
        // Create detail area
        mappingDetailViewer =
            new MappingDetailViewer(config, verticalSplitter, potentialDropTargets);
        // Configure size of components in splitters
        verticalSplitter.setWeights(new int[] {prefs.getInt(VERTICAL_SPLITTER_WEIGHT_TOP_PREFERENCE),
            prefs.getInt(VERTICAL_SPLITTER_WEIGHT_BOTTOM_PREFERENCE)});
        horizontalSplitter.setWeights(new int[] {prefs.getInt(HORIZONTAL_SPLITTER_WEIGHT_LEFT_PREFERENCE),
            prefs.getInt(HORIZONTAL_SPLITTER_WEIGHT_CENTER_PREFERENCE),
            prefs.getInt(HORIZONTAL_SPLITTER_WEIGHT_RIGHT_PREFERENCE)});
        mappingsViewer.addControlListener(new ControlAdapter() {

            @Override
            public void controlResized(ControlEvent event) {
                int[] weights = horizontalSplitter.getWeights();
                prefs.setValue(HORIZONTAL_SPLITTER_WEIGHT_LEFT_PREFERENCE, weights[0]);
                prefs.setValue(HORIZONTAL_SPLITTER_WEIGHT_CENTER_PREFERENCE, weights[1]);
                prefs.setValue(HORIZONTAL_SPLITTER_WEIGHT_RIGHT_PREFERENCE, weights[2]);
                weights = verticalSplitter.getWeights();
                prefs.setValue(VERTICAL_SPLITTER_WEIGHT_TOP_PREFERENCE, weights[0]);
                prefs.setValue(VERTICAL_SPLITTER_WEIGHT_BOTTOM_PREFERENCE, weights[1]);
            }
        });
        // Wire tree buttons to toggle model viewers between visible and hidden
        sourceViewerButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent event) {
                toggleSourceViewer(horizontalSplitter);
                prefs.setValue(SOURCE_VIEWER_PREFERENCE, sourceViewerButton.getSelection());
            }
        });
        sourceViewerButton.setSelection(prefs.getBoolean(SOURCE_VIEWER_PREFERENCE));
        if (!sourceViewerButton.getSelection()) toggleSourceViewer(horizontalSplitter);
        targetViewerButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent event) {
                toggleTargetViewer(horizontalSplitter);
                prefs.setValue(TARGET_VIEWER_PREFERENCE, targetViewerButton.getSelection());
            }
        });
        targetViewerButton.setSelection(prefs.getBoolean(TARGET_VIEWER_PREFERENCE));
        if (!targetViewerButton.getSelection()) toggleTargetViewer(horizontalSplitter);
        config.addListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                configEvent();
            }
        });
        updateHelpText();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        if (loader != null) {
            try {
                loader.close();
            } catch (final IOException e) {
                Activator.error(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave(final IProgressMonitor monitor) {}

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs() {}

    /**
     * {@inheritDoc}
     *
     * @see EditorPart#init(IEditorSite, IEditorInput)
     */
    @Override
    public void init(final IEditorSite site,
                     final IEditorInput input) throws PartInitException {
        final IContentType contentType =
            Platform.getContentTypeManager().getContentType(DozerConfigContentTypeDescriber.ID);
        if (!contentType.isAssociatedWith(input.getName())) {
            throw new PartInitException("The Fuse Transformation editor can only be opened with a"
                                        + " Dozer configuration file.");
        }
        setSite(site);
        setInput(input);
        setPartName(input.getName());

        final IFile configFile = ((FileEditorInput)getEditorInput()).getFile();
        final IJavaProject javaProject = JavaCore.create(configFile.getProject());
        try {
            loader = (URLClassLoader)JavaUtil.getProjectClassLoader(javaProject,
                                                                    getClass().getClassLoader());
            config = new TransformationConfig(configFile, loader);
            CamelModelFactory.initializeModels();
            // Add contributed functions if missing or a different version
            String version = Activator.plugin().getBundle().getVersion().toString();
            IPreferenceStore prefs = Activator.plugin().getPreferenceStore();
            boolean latestVersion = version.equals(prefs.getString(VERSION_PREFERENCE));
            copySourceToProject(Util.RESOURCES_PATH + Function.class.getName().replace('.', '/') + ".java",
                                Function.class,
                                latestVersion);
            for (IConfigurationElement element : Platform.getExtensionRegistry().getConfigurationElementsFor(Activator.FUNCTION_EXTENSION_POINT)) {
                copySourceToProject(element.getAttribute("source"),
                                    element.createExecutableExtension("class").getClass(),
                                    latestVersion);
            }
            if (!latestVersion) prefs.setValue(VERSION_PREFERENCE, version);
            // Ensure Maven will compile functions folder
            File pomFile = config.project().getLocation().append("pom.xml").toFile();
            org.apache.maven.model.Model pomModel = MavenPlugin.getMaven().readModel(pomFile);
            List<Resource> resources = pomModel.getBuild().getResources();
            boolean exists = false;
            for (Resource resource : resources) {
                if (resource.getDirectory().endsWith(Util.FUNCTIONS_FOLDER)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                Resource resource = new Resource();
                resource.setDirectory(Util.FUNCTIONS_FOLDER);
                pomModel.getBuild().addResource(resource);
                try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(pomFile))) {
                    MavenPlugin.getMaven().writeModel(pomModel, stream);
                }
            }
            // Ensure Java project source classpath entry exists for functions folder
            exists = false;
            IClasspathEntry[] entries = javaProject.getRawClasspath();
            IPath path = javaProject.getPath().append(Util.FUNCTIONS_FOLDER);
            for (IClasspathEntry entry : entries) {
                if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE && entry.getPath().equals(path)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                IClasspathEntry[] newEntries = Arrays.copyOf(entries, entries.length + 1);
                newEntries[entries.length] = JavaCore.newSourceEntry(path);
                javaProject.setRawClasspath(newEntries, null);
            }
            config.project().refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (final Exception e) {
            throw new PartInitException("Error initializing editor", e);
        }
    }

    void copySourceToProject(String sourcePath,
                             Class<?> sourceClass,
                             boolean latestVersion) throws FileNotFoundException, IOException {
        IPath path = config.project().getLocation().append(Util.FUNCTIONS_FOLDER);
        File file = path.append(sourceClass.getPackage().getName().replace('.', '/')).toFile();
        if (!file.exists()) file.mkdirs();
        file = new File(file, sourceClass.getSimpleName() + ".java");
        if (file.exists() && latestVersion) return;
        try (InputStream in = sourceClass.getClassLoader().getResourceAsStream(sourcePath)) {
            byte[] buf = new byte[4096];
            try (OutputStream out = new FileOutputStream(file)) {
                for (int len = in.read(buf); len > 0; len = in.read(buf)) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.ui.part.EditorPart#isDirty()
     */
    @Override
    public boolean isDirty() {
        return config.hasMappingPlaceholders();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.ui.ISaveablePart2#promptToSaveOnClose()
     */
    @Override
    public int promptToSaveOnClose() {
        return config.hasMappingPlaceholders()
               && !MessageDialog.openConfirm(mappingsViewer.getShell(), "Confirm",
                                             "Are you sure?\n\n"
                                                 + "All incomplete mappings will be lost when the "
                                                 + "editor is closed.")
            ? CANCEL : NO;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org
     * .eclipse.core.resources.IResourceChangeEvent)
     */
    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
        switch (event.getType()) {
            case IResourceChangeEvent.POST_CHANGE:
                // file has been deleted...
                closeEditorsWithoutValidInput();
                break;
            case IResourceChangeEvent.PRE_CLOSE:
                Display.getDefault().asyncExec(new Runnable() {

                    /*
                     * (non-Javadoc)
                     *
                     * @see java.lang.Runnable#run()
                     */
                    @Override
                    public void run() {
                        IWorkbenchPage[] pages = getSite().getWorkbenchWindow()
                                                          .getPages();
                        for (int i = 0; i < pages.length; i++) {
                            IEditorInput editorInput = getEditorInput();
                            if (editorInput instanceof FileEditorInput && ((FileEditorInput)editorInput)
                                                                                                        .getFile().getProject()
                                                                                                        .equals(event.getResource())) {
                                IWorkbenchPage page = pages[i];
                                IEditorPart editorPart = page.findEditor(editorInput);
                                page.closeEditor(editorPart, true);
                            }
                        }
                    }
                });
                break;
            default:
                // do nothing
        }
    }

    /**
     * @param mapping
     */
    public void selected(final MappingOperation<?, ?> mapping) {
        sourceTabFolder.select(mapping.getSource());
        targetTabFolder.select(mapping.getTarget());
        mappingDetailViewer.update(mapping);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
        mappingsViewer.setFocus();
    }

    void toggleSourceViewer(SashForm horizontalSplitter) {
        sourceTabFolder.setVisible(sourceViewerButton.getSelection());
        horizontalSplitter.layout();
        sourceViewerButton.setToolTipText(sourceViewerButton.getSelection()
            ? "Hide the source/variables viewers"
            : "Show the source/variables viewers");
        updateHelpText();
    }

    void toggleTargetViewer(SashForm horizontalSplitter) {
        targetTabFolder.setVisible(targetViewerButton.getSelection());
        horizontalSplitter.layout();
        targetViewerButton.setToolTipText(targetViewerButton.getSelection()
            ? "Hide the target viewer"
            : "Show the target viewer");
        updateHelpText();
    }

    void updateHelpText() {
        if (sourceViewerButton.getSelection() && targetViewerButton.getSelection()) {
            if (sourceTabFolder.getSelectionIndex() == 0) {
                helpText.setText("Create a new mapping below by dragging a property in source "
                                 + config.getSourceModel().getName()
                                 + " on the left to a property in target "
                                 + config.getTargetModel().getName() + " on the right.");
            } else {
                helpText.setText("Create a new mapping below by dragging a variable from the list"
                                 + " of variables on the left to a property in target "
                                 + config.getTargetModel().getName() + " on the right.");
            }
        } else {
            helpText.setText("");
        }
        helpText.getParent().layout();
    }
}
