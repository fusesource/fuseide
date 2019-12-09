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

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.fusesource.ide.camel.model.service.core.catalog.Parameter;
import org.fusesource.ide.camel.model.service.core.catalog.eips.Eip;
import org.fusesource.ide.camel.model.service.core.model.AbstractCamelModelElement;
import org.fusesource.ide.camel.validation.model.NumberValidator;

/**
 * @author Aurelien Pupier
 *
 */
public abstract class AbstractNumberParameterPropertyUICreator extends AbstractTextFieldParameterPropertyUICreator {

	public AbstractNumberParameterPropertyUICreator(DataBindingContext dbc, IObservableMap modelMap, Eip eip, AbstractCamelModelElement camelModelElement, Parameter parameter,
			Composite parent, TabbedPropertySheetWidgetFactory widgetFactory, ModifyListener modifyListener) {
		super(dbc, modelMap, eip, camelModelElement, parameter, parent, widgetFactory, modifyListener);
	}

	@Override
	protected IValidator createValidator() {
		final IValidator superValidator = super.createValidator();
		return value -> {
			IStatus superValidation = superValidator.validate(value);
			if (!superValidation.isOK()) {
				return superValidation;
			}
			return new NumberValidator(parameter).validate(value);
		};
	}

	@Override
	protected int createTextStyle() {
		return SWT.SINGLE | SWT.BORDER | SWT.RIGHT;
	}

}
