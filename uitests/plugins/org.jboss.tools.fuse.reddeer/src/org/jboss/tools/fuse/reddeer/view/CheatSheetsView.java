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

package org.jboss.tools.fuse.reddeer.view;

import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.uiforms.impl.formtext.DefaultFormText;
import org.eclipse.reddeer.uiforms.impl.hyperlink.DefaultHyperlink;
import org.eclipse.reddeer.uiforms.impl.section.DefaultSection;
import org.eclipse.reddeer.workbench.impl.view.WorkbenchView;
import org.jboss.tools.fuse.reddeer.condition.HyperLinkHasText;

/**
 * Represents Cheat Sheets... View from Help
 * 
 * @author fpospisi
 */
public class CheatSheetsView extends WorkbenchView {

	public static final String TITLE = "Cheat Sheets";

	private DefaultHyperlink selectedHyperlink;

	public CheatSheetsView() {
		super(TITLE);
	}

	/**
	 * Checks if section contains expectedText or not.
	 * 
	 * @param section
	 *                         section of Cheat Sheets
	 * @param expectedText
	 *                         expected text in section
	 * @return true/false
	 */
	public boolean sectionHasText(String section, String expectedText) {
		if (new DefaultFormText(new DefaultSection(section)).getText().equals(expectedText)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Selects Hyperlink, sets as selectedHyperlink and activates.
	 * 
	 * @param name
	 *                 name of hyperlink for activation
	 */
	public void selectHyperlink(String name) {
		selectedHyperlink = new DefaultHyperlink(this, name);
		selectedHyperlink.activate();
	}

	/**
	 * Checks for text change in selected Hyperlink.
	 * 
	 * @param expectedText
	 *                         expected new text for selected (last used) hyperlink
	 * @return true/false
	 */
	public boolean hyperlinkTextChange(String expectedText) {
		new WaitUntil(new HyperLinkHasText(selectedHyperlink, expectedText), TimePeriod.DEFAULT);
		if (selectedHyperlink.getText().equals(expectedText)) {
			return true;
		} else {
			return false;
		}
	}
}
