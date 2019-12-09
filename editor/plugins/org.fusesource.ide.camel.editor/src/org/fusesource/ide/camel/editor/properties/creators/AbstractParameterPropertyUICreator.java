/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at https://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.fusesource.ide.camel.editor.properties.creators;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.Observables;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.fusesource.ide.camel.model.service.core.catalog.Parameter;
import org.fusesource.ide.camel.model.service.core.catalog.components.Component;
import org.fusesource.ide.camel.model.service.core.catalog.eips.Eip;
import org.fusesource.ide.camel.model.service.core.model.AbstractCamelModelElement;
import org.fusesource.ide.camel.model.service.core.util.PropertiesUtils;
import org.fusesource.ide.foundation.ui.util.ControlDecorationHelper;

/**
 * @author Aurelien Pupier
 *
 */
public abstract class AbstractParameterPropertyUICreator {

	private Control control = null;
	private IValidator validator = null;
	private ISWTObservableValue uiObservable = null;
	private TabbedPropertySheetWidgetFactory widgetFactory;
	protected AbstractCamelModelElement camelModelElement;
	protected Eip eip;
	protected Parameter parameter;
	private DataBindingContext dbc;
	protected IObservableMap modelMap;
	protected Component component = null;
	private Composite parent;
	private int columnSpan = 3;
	private Binding bindValue;

	public AbstractParameterPropertyUICreator(DataBindingContext dbc, IObservableMap modelMap, Eip eip, AbstractCamelModelElement camelModelElement, Parameter parameter,
			Composite parent, TabbedPropertySheetWidgetFactory widgetFactory) {
		this.dbc = dbc;
		this.modelMap = modelMap;
		this.widgetFactory = widgetFactory;
		this.eip = eip;
		this.camelModelElement = camelModelElement;
		this.parameter = parameter;
		if (camelModelElement.isEndpointElement()) {
			this.component = PropertiesUtils.getComponentFor(camelModelElement);
		}
		this.parent = parent;
	}

	public void create() {
		initAndBind(parent);
	}

	private void initAndBind(Composite parent) {
		init(parent);
		bind();
		createHelpDecoration(parameter, getControl());
	}

	private void bind() {
		modelMap.put(parameter.getName(), getInitialValue());

		// create observables for the Map entries
		IObservableValue modelObservable = Observables.observeMapEntry(modelMap, parameter.getName());

		// create UpdateValueStrategy and assign to the binding
		UpdateValueStrategy strategy = new UpdateValueStrategy();
		strategy.setBeforeSetValidator(validator);

		bindValue = dbc.bindValue(uiObservable, modelObservable, strategy, null);

		ControlDecorationSupport.create(bindValue, SWT.TOP | SWT.LEFT);
	}

	protected abstract void init(Composite parent);

	public abstract Object getInitialValue();

	public Binding getBinding() {
		return bindValue;
	}
	
	public void setBinding(Binding binding) {
		this.bindValue = binding;
	}
	
	public Control getControl() {
		return control;
	}

	public IValidator getValidator() {
		return validator;
	}

	public ISWTObservableValue getUiObservable() {
		return uiObservable;
	}

	protected TabbedPropertySheetWidgetFactory getWidgetFactory() {
		return widgetFactory;
	}

	protected void setUiObservable(ISWTObservableValue uiObservable) {
		this.uiObservable = uiObservable;
	}

	protected void setControl(Control control) {
		this.control = control;
	}

	public void setValidator(IValidator validator) {
		this.validator = validator;
	}

	protected GridData createPropertyFieldLayoutData() {
		return GridDataFactory.fillDefaults().indent(5, 0).span(getColumnSpan(), 1).grab(true, false).create();
	}

	protected void createHelpDecoration(Parameter parameter, Control control) {
		String description = parameter.getDescription();
		if (description != null) {
			new ControlDecorationHelper().addInformationOnFocus(control, description);
		}
	}

	public void setColumnSpan(int count) {
		this.columnSpan = count;
	}
	
	protected int getColumnSpan() {
		return this.columnSpan;
	}
}
