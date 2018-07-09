/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.fusesource.ide.camel.editor.restconfiguration.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.fusesource.ide.camel.editor.internal.UIMessages;
import org.fusesource.ide.camel.editor.restconfiguration.RestConfigConstants;
import org.fusesource.ide.camel.editor.restconfiguration.RestConfigEditor;

/**
 * @author lheinema
 */
public class DeleteRestConfigurationAction extends RestEditorAction {
	
	/**
	 * 
	 * @param imageReg
	 */
	public DeleteRestConfigurationAction(RestConfigEditor parent, ImageRegistry imageReg) {
		super(parent, imageReg);
	}
	
	@Override
	public void run() {
		if (MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), UIMessages.restEditorDeleteRestConfigurationActionDialogTitle, UIMessages.restEditorDeleteRestConfigurationActionDialogMessage)) {
			parent.removeRestConfigurationElement();
		}
	}
	
	@Override
	public String getToolTipText() {
		return UIMessages.restEditorDeleteRestConfigurationActionButtonTooltip;
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		return mImageRegistry.getDescriptor(RestConfigConstants.IMG_DESC_DELETE);
	}
}
