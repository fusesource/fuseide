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
import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.jboss.tools.fuse.transformation.MappingOperation;
import org.jboss.tools.fuse.transformation.Variable;
import org.jboss.tools.fuse.transformation.camel.CamelEndpoint;
import org.jboss.tools.fuse.transformation.model.Model;
import org.jboss.tools.fuse.transformation.editor.internal.MappingDetailViewer;
import org.jboss.tools.fuse.transformation.editor.internal.MappingsViewer;
import org.jboss.tools.fuse.transformation.editor.internal.ModelTabFolder;
import org.jboss.tools.fuse.transformation.editor.internal.ModelViewer;
import org.jboss.tools.fuse.transformation.editor.internal.PotentialDropTarget;
import org.jboss.tools.fuse.transformation.editor.internal.VariablesViewer;
import org.jboss.tools.fuse.transformation.editor.internal.util.JavaUtil;
import org.jboss.tools.fuse.transformation.editor.internal.util.TransformationConfig;
import org.jboss.tools.fuse.transformation.editor.internal.util.Util;
import org.jboss.tools.fuse.transformation.editor.internal.util.Util.Images;

/**
 *
 */
// TODO save preferences for toggle buttons
// TODO search fields in model viewers
// TODO search in mappings viewer
// TODO support change variable name
public class TransformationEditor extends EditorPart implements ISaveablePart2 {

    private static final int SASH_COLOR = SWT.COLOR_DARK_GRAY;
    private static final int SASH_WIDTH = 3;

    TransformationConfig config;
    URLClassLoader loader;
    File camelConfigFile;
    CamelEndpoint camelEndpoint;

    MappingsViewer mappingsViewer;
    Text helpText;
    ModelTabFolder sourceModelTabFolder, targetModelTabFolder;
    MappingDetailViewer mappingDetailViewer;

    final List<PotentialDropTarget> potentialDropTargets = new ArrayList<>();

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
        final SashForm verticalSplitter = new SashForm(parent, SWT.VERTICAL);
        verticalSplitter.setBackground(parent.getDisplay().getSystemColor(SASH_COLOR));
        verticalSplitter.setSashWidth(SASH_WIDTH);
        final Composite pane = new Composite(verticalSplitter, SWT.NONE);
        pane.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).create());
        pane.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        // Create source model toggle button
        ToolBar toolBar = new ToolBar(pane, SWT.NONE);
        toolBar.setLayoutData(GridDataFactory.swtDefaults()
                                             .align(SWT.BEGINNING, SWT.BOTTOM)
                                             .create());
        final ToolItem sourceViewerButton = new ToolItem(toolBar, SWT.CHECK);
        sourceViewerButton.setImage(Images.TREE);
        sourceViewerButton.setSelection(true);
        // Create help text
        helpText = new Text(pane, SWT.MULTI | SWT.WRAP);
        helpText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        helpText.setEditable(false);
        updateHelpText(helpText);
        // Create target model toggle button
        toolBar = new ToolBar(pane, SWT.NONE);
        toolBar.setLayoutData(GridDataFactory.swtDefaults()
                                             .align(SWT.END, SWT.BOTTOM)
                                             .create());
        final ToolItem targetViewerButton = new ToolItem(toolBar, SWT.CHECK);
        targetViewerButton.setImage(Images.TREE);
        targetViewerButton.setSelection(true);
        // Create splitter between mappings viewer and model viewers
        final SashForm horizontalSplitter = new SashForm(pane, SWT.HORIZONTAL);
        horizontalSplitter.setLayoutData(GridDataFactory.fillDefaults()
                                                        .span(3, 1)
                                                        .grab(true, true)
                                                        .create());
        horizontalSplitter.setBackground(parent.getDisplay().getSystemColor(SASH_COLOR));
        horizontalSplitter.setSashWidth(SASH_WIDTH);
        // Create source model tab folder
        sourceModelTabFolder = new SourceModelTabFolder(horizontalSplitter);
        // Create transformation viewer
        mappingsViewer = new MappingsViewer(config, this, horizontalSplitter, potentialDropTargets);
        // Create target model tab folder
        targetModelTabFolder = new TargetModelTabFolder(horizontalSplitter);
        // Create detail area
        mappingDetailViewer =
            new MappingDetailViewer(config, verticalSplitter, potentialDropTargets);
        // Configure size of components in vertical splitter
        verticalSplitter.setWeights(new int[] {75, 25});

        // Set weights so mappings view is at preferred with
        horizontalSplitter.addControlListener(new ControlAdapter() {

            @Override
            public void controlResized(final ControlEvent event) {
                final double middle = mappingsViewer.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
                final double total = horizontalSplitter.getSize().x;
                final int middleWeight = (int) (middle / total * 100.0);
                final int sideWeight = (100 - middleWeight) / 2;
                if (sideWeight < 0) return; // Happens occasionally when first displayed
                horizontalSplitter.setWeights(new int[] {sideWeight, middleWeight, sideWeight});
                horizontalSplitter.removeControlListener(this);
            }
        });
        // Wire tree buttons to toggle model viewers between visible and hidden
        sourceViewerButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent event) {
                sourceModelTabFolder.setVisible(sourceViewerButton.getSelection());
                horizontalSplitter.layout();
            }
        });
        targetViewerButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent event) {
                targetModelTabFolder.setVisible(targetViewerButton.getSelection());
                horizontalSplitter.layout();
            }
        });
        config.addListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                configEvent();
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        if (loader != null)
            try {
                loader.close();
            } catch (final IOException e) {
                Activator.error(e);
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
        if (!contentType.isAssociatedWith(input.getName()))
            throw new PartInitException("The Fuse Transformation editor can only be opened with a"
                                        + " Dozer configuration file.");
        setSite(site);
        setInput(input);
        setPartName(input.getName());

        final IFile configFile = ((FileEditorInput) getEditorInput()).getFile();
        final IJavaProject javaProject = JavaCore.create(configFile.getProject());
        try {
            loader = (URLClassLoader) JavaUtil.getProjectClassLoader(javaProject,
                                                                     getClass().getClassLoader());
            config = new TransformationConfig(configFile, loader);
        } catch (final Exception e) {
            throw new PartInitException("Unable to load transformation configuration file", e);
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

    /**
     * @param mapping
     */
    public void selected(final MappingOperation<?, ?> mapping) {
        sourceModelTabFolder.select(mapping.getSource());
        targetModelTabFolder.select(mapping.getTarget());
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

    void updateHelpText(final Text helpText) {
        helpText.setText("Create a new mapping below by dragging a field from source "
                + config.getSourceModel().getName() + " on the left to target "
                + config.getTargetModel().getName() + " on the right.");
    }

    class SourceModelTabFolder extends ModelTabFolder {

        private final VariablesViewer variablesViewer;

        SourceModelTabFolder(final Composite parent) {
            super(config, parent, "Source", config.getSourceModel(), potentialDropTargets);

            // Create variables tab
            final CTabItem variablesTab = new CTabItem(this, SWT.NONE);
            variablesTab.setText("Variables");
            variablesViewer = new VariablesViewer(config, this);
            variablesTab.setControl(variablesViewer);
            variablesTab.setImage(Images.VARIABLE);
        }
    }

    class TargetModelTabFolder extends ModelTabFolder {

        TargetModelTabFolder(final Composite parent) {
            super(config, parent, "Target", config.getTargetModel(), potentialDropTargets);
        }

        @Override
        protected ModelViewer constructModelViewer(final TransformationConfig config,
                                                   final List<PotentialDropTarget> potentialDropTargets) {
            final class TargetModelViewer extends ModelViewer {

                TargetModelViewer(final Composite parent) {
                    super(config, parent, config.getTargetModel(), potentialDropTargets);
                    treeViewer.addDropSupport(DND.DROP_MOVE,
                                              new Transfer[] {LocalSelectionTransfer.getTransfer()},
                                              new ViewerDropAdapter(treeViewer) {

                        @Override
                        public boolean performDrop(final Object data) {
                            try {
                              final Object source =
                                  ((IStructuredSelection) LocalSelectionTransfer.getTransfer()
                                                                                .getSelection())
                                                                                .getFirstElement();
                              if (source instanceof Model)
                                  config.mapField((Model) source, (Model) getCurrentTarget());
                              else config.mapVariable((Variable) source, (Model) getCurrentTarget());
                              config.save();
                              return true;
                            } catch (final Exception e) {
                              Activator.error(e);
                              return false;
                            }
                        }

                        @Override
                        public boolean validateDrop(final Object target, final int operation,
                                final TransferData transferType) {
                            final Object source = ((IStructuredSelection) LocalSelectionTransfer.getTransfer()
                                    .getSelection()).getFirstElement();

                            if (source instanceof Model && target instanceof Model) {
                                return getCurrentLocation() == ViewerDropAdapter.LOCATION_ON
                                        && Util.draggingFromValidSource(config)
                                        && (Util.dragDropComboIsValid((Model) source, (Model) target) == null);
                            } else {
                                return getCurrentLocation() == ViewerDropAdapter.LOCATION_ON
                                        && Util.draggingFromValidSource(config);
                            }
                        }
                    });
                    potentialDropTargets.add(new PotentialDropTarget(treeViewer.getTree()) {

                        @Override
                        public boolean valid() {
                            return Util.draggingFromValidSource(config);
                        }
                    });
                }
            }
            return new TargetModelViewer(this);
        }
    }
}
