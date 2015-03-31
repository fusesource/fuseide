/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.fuse.transformation.editor.internal.wizards;

import java.util.Iterator;
import java.util.List;

import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.dataformat.DataFormatsDefinition;
import org.apache.camel.spring.CamelContextFactoryBean;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.ObservablesManager;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.progress.UIJob;
import org.fusesource.ide.camel.model.RouteContainer;
import org.jboss.mapper.camel.CamelBlueprintBuilder;
import org.jboss.mapper.camel.CamelConfigBuilder;
import org.jboss.mapper.camel.CamelSpringBuilder;
import org.jboss.mapper.model.ModelBuilder;
import org.jboss.tools.fuse.transformation.editor.Activator;
import org.jboss.tools.fuse.transformation.editor.internal.ModelViewer;
import org.jboss.tools.fuse.transformation.editor.wizards.NewTransformationWizard;

/**
 * @author brianf
 *
 */
@SuppressWarnings("restriction")
public class OtherPage extends XformWizardPage implements TransformationTypePage {

    final DataBindingContext context = new DataBindingContext(
            SWTObservables.getRealm(Display.getCurrent()));
    final ObservablesManager observablesManager = new ObservablesManager();
    private Composite _page;
    private boolean isSource = true;
    private Text _javaClassText;
    private ComboViewer _dataFormatIdCombo;
    private ModelBuilder _builder;
    private org.jboss.mapper.model.Model _javaModel = null;
    private ModelViewer _modelViewer;
    private Label _dfErrorLabel;

    /**
     * @param model
     */
    public OtherPage(String pageName, final Model model, boolean isSource) {
        super(pageName, model);
        setTitle("Other Page");
        setImageDescriptor(Activator.imageDescriptor("transform.png"));
        this.isSource = isSource;
        observablesManager.addObservablesFromContext(context, true, true);
        _builder = new ModelBuilder();
    }

    @Override
    public void createControl(final Composite parent) {
        if (this.isSource) {
            setTitle("Source Type (Other)");
            setDescription("Specify details for the source data format and Java class for this transformation.");
        } else {
            setTitle("Target Type (Java)");
            setDescription("Specify details for the target data format and Java class for this transformation.");
        }
        observablesManager.runAndCollect(new Runnable() {

            @Override
            public void run() {
                createPage(parent);
            }
        });

        WizardPageSupport.create(this, context);
        setErrorMessage(null);

    }

    private void createPage(Composite parent) {
        _page = new Composite(parent, SWT.NONE);
        _page.setLayout(GridLayoutFactory.swtDefaults().spacing(0, 5).numColumns(3).create());
        setControl(_page);

        // Create file path widgets
        Label label;
        if (isSourcePage()) {
            label = createLabel(_page, "Source Class:", "The source Java class for the transformation.");
        } else {
            label = createLabel(_page, "Target Class:", "The target Java class for the transformation.");
        }

        _javaClassText = new Text(_page, SWT.BORDER | SWT.READ_ONLY);
        _javaClassText.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        _javaClassText.setToolTipText(label.getToolTipText());

        final Button javaClassBrowseButton = new Button(_page, SWT.NONE);
        javaClassBrowseButton.setLayoutData(new GridData());
        javaClassBrowseButton.setText("...");
        javaClassBrowseButton.setToolTipText("Browse to specify the selected class.");

        javaClassBrowseButton.addSelectionListener(new SelectionAdapter() {

            @SuppressWarnings("static-access")
            @Override
            public void widgetSelected(final SelectionEvent event) {
                try {
                    final IType selected = selectType(_page.getShell(), "java.lang.Object", null); //$NON-NLS-1$
                    if (selected != null) {
                        _javaClassText.setText(selected.getFullyQualifiedName());
                        if (isSourcePage()) {
                            model.setSourceType(ModelType.CLASS);
                            model.setSourceFilePath(selected.getFullyQualifiedName());
                        } else {
                            model.setTargetType(ModelType.CLASS);
                            model.setTargetFilePath(selected.getFullyQualifiedName());
                        }

                        UIJob uiJob = new UIJob("open error") {
                            @Override
                            public IStatus runInUIThread(IProgressMonitor monitor) {
                                NewTransformationWizard wizard = (NewTransformationWizard) getWizard();
                                try {
                                    Class<?> tempClass = wizard.getLoader().loadClass(selected.getFullyQualifiedName());
                                    _javaModel = _builder.fromJavaClass(tempClass);
                                    _modelViewer.setModel(_javaModel);
                                } catch (ClassNotFoundException e) {
                                    e.printStackTrace();
                                }
                                return Status.OK_STATUS;
                            }
                        };
                        uiJob.setSystem(true);
                        uiJob.schedule();
                        _javaClassText.notifyListeners(SWT.Modify, new Event());
                    }
                } catch (JavaModelException e1) {
                    e1.printStackTrace();
                }
            }
        });

        label = createLabel(_page, "Data Format ID:", "Unique ID for the data format.");

        _dataFormatIdCombo = new ComboViewer(_page, SWT.DROP_DOWN | SWT.READ_ONLY);
        _dataFormatIdCombo.getCombo().setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        _dataFormatIdCombo.getCombo().setToolTipText(label.getToolTipText());
        _dataFormatIdCombo.setContentProvider(new ObservableListContentProvider());

        label = createLabel(_page, "", ""); // spacer
        _dfErrorLabel = createLabel(_page, "", "");
        _dfErrorLabel.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        Group group = new Group(_page, SWT.SHADOW_ETCHED_IN);
        group.setText("Class Structure Preview");
        group.setLayout(new GridLayout(3, false));
        group.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, true, 3, 3));

        _modelViewer = new ModelViewer(null, group, _javaModel, null);
        _modelViewer.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        _modelViewer.layout();

        bindControls();
        validatePage();

    }

    private void bindControls() {

        // Bind source file path widget to UI model
        IObservableValue widgetValue = WidgetProperties.text(SWT.Modify).observe(_javaClassText);
        IObservableValue modelValue = null;
        if (isSourcePage()) {
            modelValue = BeanProperties.value(Model.class, "sourceFilePath").observe(model);
        } else {
            modelValue = BeanProperties.value(Model.class, "targetFilePath").observe(model);
        }
        UpdateValueStrategy strategy = new UpdateValueStrategy();
        strategy.setBeforeSetValidator(new IValidator() {

            @Override
            public IStatus validate(final Object value) {
                final String path = value == null ? null : value.toString().trim();
                if (path == null || path.isEmpty()) {
                    return ValidationStatus
                            .error("A source file path must be supplied for the transformation.");
                }
                if (model.getProject().findMember(path) == null) {
                    return ValidationStatus
                            .error("Unable to find a source file with the supplied path");
                }
                return ValidationStatus.ok();
            }
        });
        ControlDecorationSupport.create(context.bindValue(widgetValue, modelValue, strategy, null),
                SWT.LEFT);

    }
    
    public void initialize() {
        
        // Bind id widget to UI model
        IObservableValue widgetValue = ViewerProperties.singleSelection().observe(_dataFormatIdCombo);
        IObservableValue modelValue = null;
        
        WritableList dfList = new WritableList();
        NewTransformationWizard transformWizard = (NewTransformationWizard) getWizard();
        List<DataFormatDefinition> dataFormats = null;
        if (transformWizard.getModel() != null) {
            CamelConfigBuilder builder = transformWizard.getModel().camelConfig.getConfigBuilder();
            if (builder instanceof CamelSpringBuilder) {
                CamelSpringBuilder springBuilder = (CamelSpringBuilder) builder;
                dataFormats = springBuilder.getDataFormats();
            } else if (builder instanceof CamelBlueprintBuilder) {
                CamelBlueprintBuilder blueprintBuilder = (CamelBlueprintBuilder) builder;
                dataFormats = blueprintBuilder.getDataFormats();
            }
            if (dataFormats != null) {
                for (Iterator<DataFormatDefinition> iterator = dataFormats.iterator(); iterator.hasNext();) {
                    DataFormatDefinition df = iterator.next();
                    dfList.add(df.getId());
                }
            }
        } else if (org.fusesource.ide.camel.editor.Activator.getDiagramEditor() != null) {
            RouteContainer routeContainer = org.fusesource.ide.camel.editor.Activator.
                    getDiagramEditor().getModel();
            CamelContextFactoryBean camelContext =
                    routeContainer.getModel().getContextElement();
            DataFormatsDefinition beandataFormats = camelContext.getDataFormats();
            if (beandataFormats != null && beandataFormats.getDataFormats() != null) {
                for (Iterator<DataFormatDefinition> iterator = beandataFormats.getDataFormats().iterator(); iterator.hasNext();) {
                    DataFormatDefinition df = iterator.next();
                    dfList.add(df.getId());
                }
            }
        }
        if (dfList.isEmpty()) {
            _dfErrorLabel.setText("No available data format definitions in the selected Camel configuration.");
            _dfErrorLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
            _dataFormatIdCombo.getCombo().setEnabled(false);
        } else {
            _dfErrorLabel.setText("");
            _dataFormatIdCombo.getCombo().setEnabled(true);
        }
        _dataFormatIdCombo.setInput(dfList);
        if (isSourcePage()) {
            modelValue = BeanProperties.value(Model.class, "sourceDataFormatid").observe(model);
        } else {
            modelValue = BeanProperties.value(Model.class, "targetDataFormatid").observe(model);
        }
        UpdateValueStrategy strategy = new UpdateValueStrategy();
        strategy.setBeforeSetValidator(new IValidator() {

            @Override
            public IStatus validate(final Object value) {
                final String path = value == null ? null : value.toString().trim();
                if (path == null || path.isEmpty()) {
                    return ValidationStatus
                            .error("A data format id must be supplied for the transformation.");
                }
                return ValidationStatus.ok();
            }
        });
        ControlDecorationSupport.create(context.bindValue(widgetValue, modelValue, strategy, null),
                SWT.TOP | SWT.LEFT);
    }

    @Override
    public boolean isSourcePage() {
        return isSource;
    }

    @Override
    public boolean isTargetPage() {
        return !isSource;
    }

    /**
     * @param shell Shell for the window
     * @param superTypeName supertype to search for
     * @param project project to look in
     * @return IType the type created
     * @throws JavaModelException exception thrown
     */
    public IType selectType(Shell shell, String superTypeName, IProject project) throws JavaModelException {
        IJavaSearchScope searchScope = null;
        if (project == null) {
            ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService()
                    .getSelection();
            IStructuredSelection selectionToPass = StructuredSelection.EMPTY;
            if (selection instanceof IStructuredSelection) {
                selectionToPass = (IStructuredSelection) selection;
                if (selectionToPass.getFirstElement() instanceof IFile) {
                    project = ((IFile) selectionToPass.getFirstElement()).getProject();
                }
            }
        }
        if (superTypeName != null && !superTypeName.equals("java.lang.Object")) { //$NON-NLS-1$
            if (project == null) {
                project = model.getProject();
            }
            IJavaProject javaProject = JavaCore.create(project);
            IType superType = javaProject.findType(superTypeName);
            if (superType != null) {
                searchScope = SearchEngine.createStrictHierarchyScope(javaProject, superType, true, false, null);
            }
        } else {
            searchScope = SearchEngine.createWorkspaceScope();
        }
        SelectionDialog dialog = JavaUI.createTypeDialog(shell, new ProgressMonitorDialog(shell), searchScope,
                IJavaElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES, false);
        dialog.setTitle("Select Class");
        dialog.setMessage("Matching items");
        if (dialog.open() == IDialogConstants.CANCEL_ID) {
            return null;
        }
        Object[] types = dialog.getResult();
        if (types == null || types.length == 0) {
            return null;
        }
        return (IType) types[0];
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            initialize();
        }
    }

}
