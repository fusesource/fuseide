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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.fuse.transformation.editor.Activator;
import org.jboss.tools.fuse.transformation.editor.internal.util.ClasspathResourceSelectionDialog;
import org.jboss.tools.fuse.transformation.model.xml.XmlModelGenerator;

/**
 * @author brianf
 *
 */
public class XMLPage extends XformWizardPage implements TransformationTypePage {

    private Composite _page;
    private boolean isSource = true;
    private Text _xmlFileText;
    private Button _xmlSchemaOption;
    private Button _xmlInstanceOption;
    private Text _xmlPreviewText;
    private ComboViewer _xmlRootsCombo;
    private Binding _binding;
    private Binding _binding2;

    /**
     * @param model
     */
    public XMLPage(String pageName, final Model model, boolean isSource) {
        super(pageName, model);
        setTitle("XML Page");
        setImageDescriptor(Activator.imageDescriptor("transform.png"));
        this.isSource = isSource;
        observablesManager.addObservablesFromContext(context, true, true);
    }

    @Override
    public void createControl(final Composite parent) {
        if (this.isSource) {
            setTitle("Source Type (XML)");
            setDescription("Specify details for the source XML for this transformation.");
        } else {
            setTitle("Target Type (XML)");
            setDescription("Specify details for the target XML for this transformation.");
        }
        observablesManager.runAndCollect(new Runnable() {

            @Override
            public void run() {
                createPage(parent);
            }
        });

        WizardPageSupport wps = WizardPageSupport.create(this, context);
        wps.setValidationMessageProvider(new WizardValidationMessageProvider());
        setErrorMessage(null); // clear any error messages at first
        setMessage(null); // now that we're using info messages, we must reset
                          // this too
    }

    private void createPage(Composite parent) {
        _page = new Composite(parent, SWT.NONE);
        setControl(_page);

        GridLayout layout = new GridLayout(3, false);
        layout.marginRight = 5;
        layout.horizontalSpacing = 10;
        _page.setLayout(layout);

        Group group = new Group(_page, SWT.SHADOW_ETCHED_IN);
        group.setText("XML Type Definition");
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 2));

        _xmlSchemaOption = new Button(group, SWT.RADIO);
        _xmlSchemaOption.setText("XML Schema");
        _xmlSchemaOption.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        _xmlSchemaOption.setSelection(true);

        _xmlInstanceOption = new Button(group, SWT.RADIO);
        _xmlInstanceOption.setText("XML Instance Document");
        _xmlInstanceOption.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        _xmlSchemaOption.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                if (isSourcePage()) {
                    model.setSourceType(ModelType.XSD);
                    model.setSourceFilePath("");
                } else {
                    model.setTargetType(ModelType.XSD);
                    model.setTargetFilePath("");
                }
                _xmlPreviewText.setText("");
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                // empty
            }
        });

        _xmlInstanceOption.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                if (isSourcePage()) {
                    model.setSourceType(ModelType.XML);
                    model.setSourceFilePath("");
                } else {
                    model.setTargetType(ModelType.XML);
                    model.setTargetFilePath("");
                }
                _xmlPreviewText.setText("");
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                // empty
            }
        });

        // Create file path widgets
        Label label;
        if (isSourcePage()) {
            label = createLabel(_page, "Source File:", "The source XML file for the transformation.");
        } else {
            label = createLabel(_page, "Target File:", "The target XML file for the transformation.");
        }

        _xmlFileText = new Text(_page, SWT.BORDER | SWT.READ_ONLY);
        _xmlFileText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        _xmlFileText.setToolTipText(label.getToolTipText());

        final Button xmlFileBrowseButton = new Button(_page, SWT.NONE);
        xmlFileBrowseButton.setLayoutData(new GridData());
        xmlFileBrowseButton.setText("...");
        xmlFileBrowseButton.setToolTipText("Browse to specify the XML file.");

        xmlFileBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
                String extension = "xml";
                boolean isXML = true;
                if (_xmlInstanceOption.getSelection()) {
                    extension = "xml";
                    isXML = true;
                } else if (_xmlSchemaOption.getSelection()) {
                    extension = "xsd";
                    isXML = false;
                }
                String path = selectResourceFromWorkspace(_page.getShell(), extension);
                if (path != null) {
                    if (isSourcePage()) {
                        if (isXML) {
                            model.setSourceType(ModelType.XML);
                        } else {
                            model.setSourceType(ModelType.XSD);
                        }
                        model.setSourceFilePath(path);
                    } else {
                        if (isXML) {
                            model.setTargetType(ModelType.XML);
                        } else {
                            model.setTargetType(ModelType.XSD);
                        }
                        model.setTargetFilePath(path);
                    }
                    _xmlFileText.setText(path);

                    IPath tempPath = new Path(path);
                    IFile xmlFile = model.getProject().getFile(tempPath);
                    if (xmlFile != null) {
                        try (InputStream istream = xmlFile.getContents()) {
                            StringBuffer buffer = new StringBuffer();
                            try (BufferedReader in = new BufferedReader(new InputStreamReader(istream))) {
                                String inputLine;
                                while ((inputLine = in.readLine()) != null) {
                                    buffer.append(inputLine + "\n");
                                }
                            }
                            _xmlPreviewText.setText(buffer.toString());
                        } catch (CoreException e1) {
                            e1.printStackTrace();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                    _xmlFileText.notifyListeners(SWT.Modify, null);
                }
            }
        });

        label = createLabel(_page, "Element Root:", "Element root to use for the root of the transformation object.");

        _xmlRootsCombo = new ComboViewer(_page, SWT.DROP_DOWN | SWT.READ_ONLY);
        _xmlRootsCombo.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        _xmlRootsCombo.getCombo().setToolTipText(label.getToolTipText());
        _xmlRootsCombo.setContentProvider(new ObservableListContentProvider());
        _xmlRootsCombo.setLabelProvider(new QNameLabelProvider());
        _xmlRootsCombo.getCombo().setEnabled(false);
        _xmlRootsCombo.getCombo().setToolTipText("This list will be populated as soon as an XML file is selected.");

        Group group2 = new Group(_page, SWT.SHADOW_ETCHED_IN);
        group2.setText("XML Structure Preview");
        group2.setLayout(new FillLayout());
        group2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 3));

        _xmlPreviewText = new Text(group2, SWT.V_SCROLL | SWT.READ_ONLY);
        _xmlPreviewText.setBackground(_page.getBackground());

        bindControls();
        validatePage();
    }

    class QNameLabelProvider extends LabelProvider {

        @Override
        public String getText(Object element) {
            if (element instanceof QName) {
                QName qname = (QName) element;
                return qname.getLocalPart();
            }
            return super.getText(element);
        }

    }

    @Override
    public boolean isSourcePage() {
        return isSource;
    }

    @Override
    public boolean isTargetPage() {
        return !isSource;
    }

    private String selectResourceFromWorkspace(Shell shell, final String extension) {
        IJavaProject javaProject = null;
        if (getModel() != null) {
            if (getModel().getProject() != null) {
                javaProject = JavaCore.create(getModel().getProject());
            }
        }
        ClasspathResourceSelectionDialog dialog = null;
        if (javaProject == null) {
            dialog = new ClasspathResourceSelectionDialog(shell, ResourcesPlugin.getWorkspace().getRoot(), extension);
        } else {
            dialog = new ClasspathResourceSelectionDialog(shell, javaProject.getProject(), extension);
        }
        dialog.setTitle("Select " + extension.toUpperCase() + " From Project");
        dialog.setInitialPattern("*." + extension); //$NON-NLS-1$
        dialog.open();
        Object[] result = dialog.getResult();
        if (result == null || result.length == 0 || !(result[0] instanceof IResource)) {
            return null;
        }
        return ((IResource) result[0]).getProjectRelativePath().toPortableString();
    }

    private void bindControls() {

        // Bind source file path widget to UI model
        IObservableValue widgetValue = WidgetProperties.text(SWT.Modify).observe(_xmlFileText);
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
                    return ValidationStatus.error("A source file path must be supplied for the transformation.");
                }
                if (model.getProject().findMember(path) == null) {
                    return ValidationStatus.error("Unable to find a file with the supplied path");
                }
                return ValidationStatus.ok();
            }
        });
        _binding = context.bindValue(widgetValue, modelValue, strategy, null);
        ControlDecorationSupport.create(_binding, decoratorPosition, _xmlFileText.getParent(),
                new WizardControlDecorationUpdater());

        IObservableValue comboWidgetValue = ViewerProperties.singleSelection().observe(_xmlRootsCombo);
        IObservableValue comboModelValue = null;
        if (isSourcePage()) {
            comboModelValue = BeanProperties.value(Model.class, "sourceClassName").observe(model);
        } else {
            comboModelValue = BeanProperties.value(Model.class, "targetClassName").observe(model);
        }

        UpdateValueStrategy combostrategy = new UpdateValueStrategy();
        combostrategy.setBeforeSetValidator(new IValidator() {

            @Override
            public IStatus validate(final Object value) {
                final String name = value == null ? null : value.toString().trim();
                if (name == null || name.isEmpty()) {
                    return ValidationStatus.error("A root element name must be supplied for the transformation.");
                }
                return ValidationStatus.ok();
            }
        });
        _binding2 = context.bindValue(comboWidgetValue, comboModelValue, combostrategy, null);
        ControlDecorationSupport.create(_binding2, decoratorPosition, _xmlRootsCombo.getControl().getParent(),
                new WizardControlDecorationUpdater());

        modelValue.addValueChangeListener(new IValueChangeListener() {

            @Override
            public void handleValueChange(ValueChangeEvent event) {
                Object value = event.diff.getNewValue();
                String path = null;
                if (value != null && !value.toString().trim().isEmpty()) {
                    path = value.toString().trim();
                }
                if (path == null) {
                    return;
                }
                XmlModelGenerator modelGen = new XmlModelGenerator();
                List<QName> elements = null;
                IPath filePath = model.getProject().getLocation().makeAbsolute().append(path);
                path = filePath.makeAbsolute().toPortableString();
                if (isSourcePage()) {
                    if (model.getSourceType().equals(ModelType.XSD)) {
                        try {
                            elements = modelGen.getElementsFromSchema(new File(path));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (model.getSourceType().equals(ModelType.XML)) {
                        try {
                            QName element = modelGen.getRootElementName(new File(path));
                            if (element != null) {
                                elements = new ArrayList<QName>();
                                elements.add(element);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (model.getTargetType().equals(ModelType.XSD)) {
                        try {
                            elements = modelGen.getElementsFromSchema(new File(path));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (model.getTargetType().equals(ModelType.XML)) {
                        try {
                            QName element = modelGen.getRootElementName(new File(path));
                            if (element != null) {
                                elements = new ArrayList<QName>();
                                elements.add(element);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                WritableList elementList = new WritableList();
                if (elements != null && !elements.isEmpty()) {
                    Iterator<QName> iter = elements.iterator();
                    while (iter.hasNext()) {
                        QName qname = iter.next();
                        elementList.add(qname.getLocalPart());
                        _xmlRootsCombo.setData(qname.getLocalPart(), qname.getNamespaceURI());
                    }
                }
                _xmlRootsCombo.setInput(elementList);
                if (elementList != null && !elementList.isEmpty()) {
                    _xmlRootsCombo.setSelection(new StructuredSelection(elementList.get(0)));
                    String elementName = (String) elementList.get(0);
                    if (isSourcePage()) {
                        model.setSourceClassName(elementName);
                    } else {
                        model.setTargetClassName(elementName);
                    }
                    _xmlRootsCombo.getCombo().setEnabled(true);
                    if (elementList.size() == 1) {
                        _xmlRootsCombo.getCombo().setToolTipText("Only one root element found.");
                    } else {
                        _xmlRootsCombo.getCombo().setToolTipText("Select from the available list of root elements.");
                    }
                }
            }
        });

        listenForValidationChanges();
    }

    @Override
    public void notifyListeners() {
        if (_xmlFileText != null && !_xmlFileText.isDisposed()) {
            _xmlFileText.notifyListeners(SWT.Modify, new Event());
            _xmlRootsCombo.getCombo().notifyListeners(SWT.Modify, new Event());
        }
    }

    @Override
    public void clearControls() {
        if (_xmlFileText != null && !_xmlFileText.isDisposed()) {
            _xmlFileText.setText("");
            _xmlRootsCombo.setSelection(null);
            _xmlPreviewText.setText("");
        }
        notifyListeners();
    }

    @Override
    public void pingBinding() {
        if (_binding != null) {
            _binding.validateTargetToModel();
        }
        if (_binding2 != null) {
            _binding2.validateTargetToModel();
        }
    }
}
