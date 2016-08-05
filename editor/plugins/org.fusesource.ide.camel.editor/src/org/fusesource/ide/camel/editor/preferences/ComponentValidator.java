/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.fusesource.ide.camel.editor.preferences;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.dialogs.IInputValidator;
import org.fusesource.ide.camel.editor.internal.UIMessages;

/**
 * 
 * @author Andrej Podhradsky (apodhrad@redhat.com)
 *
 */
public class ComponentValidator implements IInputValidator {

	private List<String> components;

	public ComponentValidator(List<String> components) {
		this.components = components;
	}

	public ComponentValidator(String... components) {
		this(Arrays.asList(components));
	}

	@Override
	public String isValid(String newText) {
		if (newText.isEmpty()) {
			return UIMessages.preferredLabels_errorMessageEmptyComponent;
		}
		if (newText.matches(".*[\\W&&[^-]].*")) {
			return UIMessages.preferredLabels_errorMessageWrongCharacter;
		}
		if (isDuplicate(newText)) {
			return UIMessages.preferredLabels_errorMessageDuplicateComponent;
		}
		return null;
	}

	private boolean isDuplicate(String component) {
		return components.contains(component);
	}

}
