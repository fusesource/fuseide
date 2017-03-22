/******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: JBoss by Red Hat - Initial implementation.
 *****************************************************************************/
package org.jboss.tools.fuse.transformation.editor.internal.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.fusesource.ide.camel.editor.utils.CamelUtils;
import org.fusesource.ide.camel.editor.utils.MavenUtils;
import org.fusesource.ide.camel.model.service.core.catalog.Dependency;
import org.jboss.tools.fuse.transformation.core.camel.CamelConfigBuilder;
import org.jboss.tools.fuse.transformation.editor.Activator;
import org.jboss.tools.fuse.transformation.editor.internal.l10n.Messages;
import org.jboss.tools.fuse.transformation.editor.internal.util.CamelFileTypeHelper;
import org.jboss.tools.fuse.transformation.editor.internal.util.JavaUtil;
import org.jboss.tools.fuse.transformation.editor.internal.util.TestGenerator;
import org.jboss.tools.fuse.transformation.editor.internal.util.Util;

/**
 *
 *
 */
public class TransformTestWizardPage extends NewTypeWizardPage {

    protected static final String CAMEL_FILE_PATH = "TransformTestWizardPage.CamelFilePath"; //$NON-NLS-1$
    protected static final String ENDPOINT = "TransformTestWizardPage.Endpoint"; //$NON-NLS-1$

    private ComboViewer transformationIDViewer;
    private Text _camelFilePathText;

    private IProject _project;
    private IJavaProject _javaProject;
    private IFile _camelConfigFile;
    private CamelConfigBuilder _builder = null;
    private String _transformID = null;
    private String _camelFilePath = null;
    private IResource _generatedClassResource;

    private IStatus _camelFileSelectedStatus = Status.OK_STATUS;
    private IStatus _camelEndpointSelectedStatus = Status.OK_STATUS;

    public TransformTestWizardPage() {
        super(true, TransformTestWizardPage.class.getSimpleName());
        setImageDescriptor(Activator.imageDescriptor("transform.png")); //$NON-NLS-1$
        setTitle(Messages.TransformTestWizardPage_title);
		setDescription(Messages.TransformTestWizardPage_description);
    }

    private void createCamelSpecificControls(Composite composite, int nColumns) {
        // Create camel file path widgets
        Label label = new Label(composite, SWT.NONE);
        label.setText(Messages.TransformTestWizardPage_labelCamelFilePath);
        label.setToolTipText(Messages.TransformTestWizardPage_tootlipCamelFilePath);

        _camelFilePathText = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
        _camelFilePathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        _camelFilePathText.setToolTipText(label.getToolTipText());
        _camelFilePathText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                handleFieldChanged(CAMEL_FILE_PATH);
            }
        });

        final Button camelPathButton = new Button(composite, SWT.NONE);
        camelPathButton.setText(Messages.TransformTestWizardPage_Browse);
        camelPathButton.setToolTipText(Messages.TransformTestWizardPage_BrowseTooltip);
        camelPathButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        camelPathButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent event) {
                _builder = null;
                final IResource res = Util.selectCamelResourceFromWorkspace(getShell(), _project);
                if (res != null) {
                    String path = ""; //$NON-NLS-1$
                    try {
                        IPath respath = JavaUtil.getJavaPathForResource(res);
                        if (_project == null) {
                            _project = res.getProject();
                            _javaProject = JavaCore.create(_project);
                        }
                        if (_javaProject != null) {
                            IFolder srcFolder = _javaProject.getProject().getFolder("src/test/java"); //$NON-NLS-1$
                            IPackageFragmentRoot root = _javaProject.getPackageFragmentRoot(srcFolder);
                            initContainerPage(root);
                        }
                        IFile camelConfigFile = (IFile) _project.findMember(respath);
                        if (camelConfigFile == null) {
                            IPath newrespath = new Path("src/main/resources/").append(respath); //$NON-NLS-1$
                            camelConfigFile = (IFile) _project.findMember(newrespath);
                        }
                        if (camelConfigFile != null) {
                            path = respath.makeRelative().toString();
                            _camelFilePath = camelConfigFile.getProjectRelativePath().toPortableString();
                            File file = new File(camelConfigFile.getLocationURI());
                            boolean isValid = CamelFileTypeHelper
                                    .isSupportedCamelFile(_project, _camelFilePath);
                            if (isValid) {
                                _builder = new CamelConfigBuilder(file);
                                _camelFileSelectedStatus = Status.OK_STATUS;
                            } else {
                                _builder = null;
                                _camelFileSelectedStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                                        Messages.TransformTestWizardPage_errorWrongFileSelected
                                        + Messages.TransformTestWizardPage_errorMessagePleaseSelectAntherFile);
                            }
                            if (_builder != null) {
                                transformationIDViewer.getCombo().removeAll();
                                transformationIDViewer.add(_builder.getTransformEndpointIds().toArray());
                            }
                        }
                    } catch (Exception e) {
                        Activator.error(e);
                    }
                    if (_builder == null || _builder.getTransformEndpointIds().isEmpty()) {
                        transformationIDViewer.getCombo().removeAll();
                        transformationIDViewer.getCombo().setToolTipText(Messages.TransformTestWizardPage_tooltipNoTransformationEndpointsAvailable);
                        _camelEndpointSelectedStatus = new Status(IStatus.WARNING, Activator.PLUGIN_ID,
                                Messages.TransformTestWizardPage_errorMessageNoTransformationEndpointsAvailable);
                    } else {
                        _camelEndpointSelectedStatus = new Status(IStatus.INFO, Activator.PLUGIN_ID,
                                Messages.TransformTestWizardPage_selectFormTheListOFAvailableEndpoints);
                        transformationIDViewer.getCombo().setToolTipText(
                                Messages.TransformTestWizardPage_selectFormTheListOFAvailableEndpoints);
                    }
                    _camelFilePathText.setText(path);
                    handleFieldChanged(CAMEL_FILE_PATH);
                }
            }
        });

        label = new Label(composite, SWT.NONE);
        label.setText(Messages.TransformTestWizardPage_transformationIDLabel);
        label.setToolTipText(Messages.TransformTestWizardPage_transformationIDTooltip);
        transformationIDViewer = new ComboViewer(new Combo(composite, SWT.READ_ONLY));
        transformationIDViewer.getCombo().setLayoutData(
                GridDataFactory.swtDefaults().grab(true, false).span(2, 1).align(SWT.FILL, SWT.CENTER).create());

        transformationIDViewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(final Object element) {
                return ((String) element);
            }
        });
        if (_camelFilePath != null) {
            _camelFilePathText.setText(_camelFilePath);
        }
        boolean noEndpoints = true;
        transformationIDViewer.getCombo().removeAll();
        if (_builder != null) {
            transformationIDViewer.add(_builder.getTransformEndpointIds().toArray());
            noEndpoints = _builder.getTransformEndpointIds().isEmpty();
        }
        if (!noEndpoints) {
            transformationIDViewer.getCombo().setToolTipText(
                Messages.TransformTestWizardPage_selectFormTheListOFAvailableEndpoints);
            _camelEndpointSelectedStatus = new Status(IStatus.INFO, Activator.PLUGIN_ID,
                    Messages.TransformTestWizardPage_selectFormTheListOFAvailableEndpoints);
        } else {
            transformationIDViewer.getCombo().setToolTipText(Messages.TransformTestWizardPage_tooltipNoTransformationEndpointsAvailable);
            _camelEndpointSelectedStatus = new Status(IStatus.WARNING, Activator.PLUGIN_ID,
                    Messages.TransformTestWizardPage_errorMessageNoTransformationEndpointsAvailable);
        }

        transformationIDViewer.getCombo().addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent event) {
                _transformID = (String) ((IStructuredSelection) transformationIDViewer.getSelection())
                        .getFirstElement();
                _camelEndpointSelectedStatus = Status.OK_STATUS;
                handleFieldChanged(ENDPOINT);
            }
        });
        doStatusUpdate();
        setErrorMessage(null);
    }

    private List<Dependency> getRequiredBlueprintTestDependencies(IProject project) {
        List<Dependency> deps = new ArrayList<>();
        Dependency dep = new Dependency();
        dep.setGroupId("org.apache.camel"); //$NON-NLS-1$
        dep.setArtifactId("camel-test-blueprint"); //$NON-NLS-1$
        dep.setVersion(CamelUtils.getCurrentProjectCamelVersion());
        deps.add(dep);
        return deps;
    }

    private List<Dependency> getRequiredSpringTestDependencies(IProject project) {
        List<Dependency> deps = new ArrayList<>();
        Dependency dep = new Dependency();
        dep.setGroupId("org.apache.camel"); //$NON-NLS-1$
        dep.setArtifactId("camel-test-spring"); //$NON-NLS-1$
        dep.setVersion(CamelUtils.getCurrentProjectCamelVersion());
        deps.add(dep);
        return deps;
    }

    private ICompilationUnit createJavaClass(String packageName,
            String className, IJavaProject project) {
        try {
            boolean isSpring = CamelFileTypeHelper
                    .isSpringFile(project.getProject(), _camelFilePath);
            boolean isBlueprint = CamelFileTypeHelper
                    .isBlueprintFile(project.getProject(),_camelFilePath);

            if (!isSpring && !isBlueprint) {
                // obviously we're not dealing with a camel file here
                return null;
            }

            if (isBlueprint) updateMavenDependencies(getRequiredBlueprintTestDependencies(project.getProject()));
            else updateMavenDependencies(getRequiredSpringTestDependencies(project.getProject()));

            // refresh the project in case we added dependencies
            project.getProject().refreshLocal(IProject.DEPTH_INFINITE, null);
            // Ensure build of Java classes has completed
            try {
                Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
            } catch (final InterruptedException ignored) {
            }

            IPath srcPath = null;
            if (getPackageFragmentRoot() != null) {
                srcPath = getPackageFragmentRoot().getPath().makeAbsolute();
                srcPath = srcPath.removeFirstSegments(1);
                IFolder folder = _javaProject.getProject().getFolder(srcPath);
                if (!JavaUtil.findFolderOnProjectClasspath(_javaProject, folder)) {
                    JavaUtil.addFolderToProjectClasspath(_javaProject, folder);
                }
                if (!folder.exists()) {
                    try {
                        folder.refreshLocal(IResource.DEPTH_INFINITE, null);
                    } catch (CoreException e) {
                        e.printStackTrace();
                    }
                }

            } else {
                IFolder folder = _javaProject.getProject().getFolder("src/test/java"); //$NON-NLS-1$
                if (!folder.exists()) {
                    IFolder srcFolder = _javaProject.getProject().getFolder("src"); //$NON-NLS-1$
                    if (!srcFolder.exists()) {
                        srcFolder.create(true,  true,  null);
                    }
                    IFolder testFolder = srcFolder.getFolder("test"); //$NON-NLS-1$
                    if (!testFolder.exists()) {
                        testFolder.create(true,  true,  null);
                    }
                    IFolder javaFolder = testFolder.getFolder("java"); //$NON-NLS-1$
                    if (!javaFolder.exists()) {
                        javaFolder.create(true,  true,  null);
                    }
                }
                if (!JavaUtil.findFolderOnProjectClasspath(_javaProject, folder)) {
                    JavaUtil.addFolderToProjectClasspath(_javaProject, folder);
                }
                srcPath = folder.getProjectRelativePath();
            }

            IFolder srcFolder = project.getProject().getFolder(srcPath);
            if (srcFolder == null || !srcFolder.exists()) {
                srcPath = project.getPath().append(
                        srcPath.makeRelativeTo(project.getProject()
                                .getLocation()));
                srcFolder = project.getProject().getFolder(srcPath);
            }
            IPackageFragmentRoot root = project.getPackageFragmentRoot(srcFolder);
            if (packageName == null) {
                packageName = ""; //$NON-NLS-1$
            }
            if (root != null) {
                IPackageFragment pkg = root.createPackageFragment(packageName,
                        false, null);

                StringBuffer clsContent = new StringBuffer();

                String filePath = getCamelFilePath();
                IResource res = project.getProject().findMember(filePath);
                IPath respath = JavaUtil.getJavaPathForResource(res);
                filePath = respath.makeRelative().toString();

                if (isSpring || isBlueprint) {
                    String codeTemplate =
                        TestGenerator.createTransformTestText(
                                _transformID, packageName, className, filePath, isSpring);

                    if (codeTemplate != null) {
                        clsContent.append(codeTemplate);
                    }
                    ICompilationUnit wrapperCls = pkg.createCompilationUnit(className
                        + ".java", clsContent.toString(), true, null); //$NON-NLS-1$
                    wrapperCls.save(null, true);
                    return wrapperCls;
                }
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks if we need to add a maven dependency for the chosen component and inserts it into the pom.xml if needed.
     *
     * @param compDeps
     * @throws CoreException
     */
    public void updateMavenDependencies(List<Dependency> compDeps) throws CoreException {
    	new MavenUtils().updateMavenDependencies(compDeps);
    }

    @Override
    public void createType(IProgressMonitor monitor) {
        ICompilationUnit createdClass =
                createJavaClass(getPackageText(), getTypeName(), _javaProject);
        if (createdClass != null) {
            _generatedClassResource = createdClass.getResource();
        }
    }

    public IResource getGeneratedResource() {
        return this._generatedClassResource;
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());

        int nColumns = 4;

        GridLayout layout = new GridLayout();
        layout.numColumns = nColumns;
        composite.setLayout(layout);

        // pick & choose the wanted UI components

        createContainerControls(composite, nColumns);
        createPackageControls(composite, nColumns);

        createSeparator(composite, nColumns);

        createTypeNameControls(composite, nColumns);

        createSeparator(composite, nColumns);

        createCamelSpecificControls(composite, nColumns);

        setControl(composite);

        Dialog.applyDialogFont(composite);

        doStatusUpdate();

        updateStatus(new Status(IStatus.OK, Activator.PLUGIN_ID,
				Messages.TransformTestWizardPage_description));
        setErrorMessage(null);
    }

    @Override
    public boolean isPageComplete() {
        // having an endpoint selected implies that we also have a camel file selected
        boolean endpointSelected = (getTransformID() != null && !getTransformID().trim().isEmpty());
        boolean sourceFolderSpecified = (getPackageFragmentRoot() != null)
                || (getPackageFragmentRootText() != null
                && getPackageFragmentRootText().endsWith("src/test/java")); //$NON-NLS-1$
        boolean classNameSpecified = (getTypeName() != null);
        boolean superComplete = super.isPageComplete();
        if (superComplete && endpointSelected && sourceFolderSpecified && classNameSpecified) {
            return true;
        }
        return false;
    }

    public IProject getProject() {
        return _project;
    }

    public void setProject(IProject project) {
        this._project = project;
    }

    @Override
    public IJavaProject getJavaProject() {
        return _javaProject;
    }

    public void setJavaProject(IJavaProject javaProject) {
        this._javaProject = javaProject;
    }

    public IFile getDozerConfigFile() {
        return _camelConfigFile;
    }

    public void setCamelConfigFile(IFile dozerConfigFile) {
        this._camelConfigFile = dozerConfigFile;
        if (dozerConfigFile != null) {
            this._camelFilePath = this._camelConfigFile.getProjectRelativePath().toPortableString();
        }
    }

    public CamelConfigBuilder getBuilder() {
        return _builder;
    }

    public void setBuilder(CamelConfigBuilder builder) {
        this._builder = builder;
    }

    public String getTransformID() {
        return _transformID;
    }

    public void setTransformID(String transformID) {
        this._transformID = transformID;
    }

    public String getCamelFilePath() {
        return _camelFilePath;
    }

    public void setCamelFilePath(String path) {
        this._camelFilePath = path;
    }

    private void doStatusUpdate() {
        if (fContainerStatus.getMessage() != null && fContainerStatus.getMessage().endsWith("does not exist.")) { //$NON-NLS-1$
            if (getPackageFragmentRootText().endsWith("src/test/java")) { //$NON-NLS-1$
                // override this particular case, since we'll create it
                fContainerStatus = new StatusInfo(NONE, null);
            }
        }

        if (fPackageStatus.getCode() == StatusInfo.ERROR && (fPackageStatus.getMessage() == null
                || fPackageStatus.getMessage().trim().isEmpty())) {
            // override this particular case, since the default package is ok, though not great
            fPackageStatus = new StatusInfo(NONE, null);
        } else if (fPackageStatus.getCode() == StatusInfo.WARNING && fPackageStatus.getMessage() != null
                && fPackageStatus.getMessage().contains("default package is discouraged")) { //$NON-NLS-1$
            // override this particular case, since the default package is ok, though not great
            fPackageStatus = new StatusInfo(NONE, null);
        }

        // all used component status
        IStatus[] status = new IStatus[] {
            fContainerStatus,
            fPackageStatus,
            fTypeNameStatus,
            _camelFileSelectedStatus,
            _camelEndpointSelectedStatus
        };

        // the mode severe status will be displayed and the OK button enabled/disabled.
        updateStatus(status);

        IStatus currStatus = StatusUtil.getMostSevere(status);
        setPageComplete(currStatus.isOK());
    }

    /*
     * @see NewContainerWizardPage#handleFieldChanged
     */
    @Override
    protected void handleFieldChanged(String fieldName) {
        super.handleFieldChanged(fieldName);
        doStatusUpdate();
    }

    /**
     * The wizard owning this page is responsible for calling this method with the
     * current selection. The selection is used to initialize the fields of the wizard
     * page.
     *
     * @param selection used to initialize the fields
     */
    public void init(IStructuredSelection selection) {
        IJavaElement jelem = getInitialJavaElement(selection);
        initContainerPage(jelem);
        initTypePage(jelem);
        setTypeName("TransformationTest", true); //$NON-NLS-1$
        doStatusUpdate();
    }
}
