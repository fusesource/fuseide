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
package org.jboss.tools.fuse.reddeer.condition;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.uiforms.impl.hyperlink.DefaultHyperlink;

/**
 * @author fpospisi
 */
public class HyperLinkHasText extends AbstractWaitCondition {

	private DefaultHyperlink link;
	private String actualText;
	private String expectedText;

	public HyperLinkHasText(DefaultHyperlink link, String expectedText) {
		this.link = link;
		this.expectedText = expectedText;
	}

	@Override
	public boolean test() {
		actualText = link.getText();
		return actualText.contains(expectedText);
	}

	@Override
	public String description() {
		return "Expected HyperLink text: '" + expectedText + "' but '" + actualText + "' was given.";
	}
}
