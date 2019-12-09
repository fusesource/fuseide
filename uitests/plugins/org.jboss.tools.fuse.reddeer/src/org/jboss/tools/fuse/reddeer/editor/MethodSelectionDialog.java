/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at https://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.fuse.reddeer.editor;

import java.util.List;

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.condition.TreeHasChildren;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;

/**
 * 
 * @author apodhrad
 *
 */
public class MethodSelectionDialog extends DefaultShell {

	public static final String TITLE = "Method Selection";

	public MethodSelectionDialog() {
		super(TITLE);
	}

	public MethodSelectionDialog setText(String text) {
		new DefaultText(this).setText(text);
		return this;
	}

	public List<TreeItem> getItems() {
		return new DefaultTree(this).getItems();
	}

	public MethodSelectionDialog waitForItems() {
		new WaitUntil(new TreeHasChildren(new DefaultTree(this)), TimePeriod.LONG, false);
		return this;
	}

	public MethodSelectionDialog selectItem(String text) {
		new DefaultTree(this).getItem(text).select();
		return this;
	}

	public void ok() {
		List<TreeItem> selectedItems = new DefaultTree(this).getSelectedItems();
		if (selectedItems.isEmpty()) {
			throw new RedDeerException("No item is selected");
		}
		new OkButton(this).click();
		new WaitWhile(new ShellIsAvailable(TITLE));
	}

	public void cancel() {
		new CancelButton(this).click();
		new WaitWhile(new ShellIsAvailable(TITLE));
	}
}
