/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.fuse.reddeer.wizard;

import java.util.List;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.menu.ShellMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.workbench.impl.shell.WorkbenchShell;

/**
 * Represents Cheat Sheets wizard
 *
 * @author fpospisi
 */
public class CheatSheetsWizard {

	public static final String SHELL_NAME = "Cheat Sheet Selection";
	public static final String[] PATH = { "Help", "Cheat Sheets..." };

	private ShellMenuItem menuItem;
	private DefaultShell shell;

	public CheatSheetsWizard() {
		menuItem = new ShellMenuItem(new WorkbenchShell(), PATH);
	}

	public void open() {
		menuItem.select();
		new WaitUntil(new ShellIsAvailable(SHELL_NAME), TimePeriod.DEFAULT);
		shell = new DefaultShell(SHELL_NAME);
	}

	public void selectCheatSheet(String... path) {
		DefaultTree tree = new DefaultTree(shell);
		TreeItem item = tree.getItem(path[0]);
		item.expand();
		List<TreeItem> items = item.getItems();
		for (TreeItem treeItem : items) {
			if(treeItem.getText().equals(path[1])) {
				treeItem.select();
			}
		}
	}

	public void finish() {
		new OkButton().click();
	}

}
