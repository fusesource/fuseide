/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at https://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.fusesource.ide.wsdl2rest.ui.wizard.pages;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.fusesource.ide.wsdl2rest.ui.internal.UIMessages;

/**
 * Second page of the wsdl2rest wizard that collects secondary details.
 * @author brianf
 */
public class Wsdl2RestWizardSecondPage extends Wsdl2RestWizardBasePage {

	private Binding javaPathBinding;
	private Binding camelPathBinding;

	public Wsdl2RestWizardSecondPage(String title) {
		super(title, title, null);
		setMessage(UIMessages.wsdl2RestWizardSecondPagePageTwoDescription);
	}

	@Override
	public void createControl(Composite parent) {
		WizardPageSupport.create(this, dbc);
		setDescription(UIMessages.wsdl2RestWizardSecondPagePageTwoDescription);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(GridLayoutFactory.swtDefaults().numColumns(4).create());
		composite.setLayoutData(GridDataFactory.fillDefaults().grab(false, false).create());

		Text javaPathTextControl = createDestinationJavaFolderControl(composite);
		Text camelPathTextControl = createDestinationCamelFolderControl(composite);
		Text targetAddressText = createLabelAndText(composite, UIMessages.wsdl2RestWizardSecondPageTargetServiceAddressLabel, 3);
		Text targetRestAddressText = createLabelAndText(composite, UIMessages.wsdl2RestWizardSecondPageTargetRESTServiceAddressLabel, 3);

		createDataBindings(javaPathTextControl, camelPathTextControl, targetAddressText, targetRestAddressText);

		// set initial values
		initIfNotEmpty(javaPathTextControl, getOptionsFromWizard().getDestinationJava());
		initIfNotEmpty(camelPathTextControl, getOptionsFromWizard().getDestinationCamel());
		initIfNotEmpty(targetAddressText, getOptionsFromWizard().getTargetServiceAddress());
		initIfNotEmpty(targetRestAddressText, getOptionsFromWizard().getTargetRestServiceAddress());

		setControl(composite);
		setPageComplete(isPageComplete());
		setErrorMessage(null); // clear any error messages at first
	}

	private void createDataBindings(Text javaPathTextControl, Text camelPathTextControl, Text targetAddressText, Text targetRestAddressText) {
		javaPathBinding = createBinding(javaPathTextControl, "destinationJava", new PathValidator());
		ControlDecorationSupport.create(javaPathBinding, SWT.LEFT | SWT.TOP);

		camelPathBinding = createBinding(camelPathTextControl, "destinationCamel", new PathValidator());
		ControlDecorationSupport.create(camelPathBinding, SWT.LEFT | SWT.TOP);

		Binding targetAddressBinding = createBinding(targetAddressText, "targetServiceAddress", new TargetURLValidator()); //$NON-NLS-1$
		ControlDecorationSupport.create(targetAddressBinding, SWT.LEFT | SWT.TOP);

		Binding targetRestAddressBinding = createBinding(targetRestAddressText, "targetRestServiceAddress", new TargetURLValidator()); //$NON-NLS-1$
		ControlDecorationSupport.create(targetRestAddressBinding, SWT.LEFT | SWT.TOP);
	}

	private Text createDestinationCamelFolderControl(Composite composite) {
		Text camelPathTextControl = createLabelAndText(composite, UIMessages.wsdl2RestWizardSecondPageCamelFolderLabel, 2);
		Button outPathBrowseButton = createButton(composite, "..."); //$NON-NLS-1$
		outPathBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// browse
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(getOptionsFromWizard().getProjectName());
				String path = selectFolder(project);
				if (path != null) {
					getOptionsFromWizard().setDestinationCamel(path);
					camelPathTextControl.notifyListeners(SWT.Modify, new Event());
					camelPathBinding.updateModelToTarget();
				}
			}
		});
		return camelPathTextControl;
	}

	private Text createDestinationJavaFolderControl(Composite composite) {
		Text javaPathTextControl = createLabelAndText(composite, UIMessages.wsdl2RestWizardSecondPageJavaFolderLabel, 2);
		Button javaPathBrowseBtn = createButton(composite, "..."); //$NON-NLS-1$
		javaPathBrowseBtn.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				// browse
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(getOptionsFromWizard().getProjectName());
				String path = selectFolder(project);
				if (path != null) {
					getOptionsFromWizard().setDestinationJava(path);
					javaPathTextControl.notifyListeners(SWT.Modify, new Event());
					javaPathBinding.updateModelToTarget();
				}
			}
		});
		return javaPathTextControl;
	}
	
	protected void updateFieldsForProjectSelection() {
		javaPathBinding.updateTargetToModel();
		camelPathBinding.updateTargetToModel();
	}
}
