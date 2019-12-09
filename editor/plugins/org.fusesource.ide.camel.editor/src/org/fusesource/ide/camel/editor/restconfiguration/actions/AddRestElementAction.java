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
package org.fusesource.ide.camel.editor.restconfiguration.actions;

import org.eclipse.jface.resource.ImageRegistry;
import org.fusesource.ide.camel.editor.internal.UIMessages;
import org.fusesource.ide.camel.editor.restconfiguration.RestConfigConstants;
import org.fusesource.ide.camel.editor.restconfiguration.RestConfigEditor;

/**
 * @author lheinema
 */
public class AddRestElementAction extends RestEditorAction {
	
	/**
	 * @param parent
	 * @param imageReg
	 */
	public AddRestElementAction(RestConfigEditor parent, ImageRegistry imageReg) {
		super(parent, imageReg);
	}
	
	@Override
	public void run() {
		parent.addRestElement();
	}
	
	@Override
	public String getToolTipText() {
		return UIMessages.addRestElementActionTooltip;
	}
	
	@Override
	protected String getImageName() {
		return RestConfigConstants.IMG_DESC_ADD;
	}
}
