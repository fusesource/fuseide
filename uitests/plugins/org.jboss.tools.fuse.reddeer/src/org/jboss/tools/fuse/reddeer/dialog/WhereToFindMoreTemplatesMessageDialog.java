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
package org.jboss.tools.fuse.reddeer.dialog;

import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.styledtext.DefaultStyledText;

public class WhereToFindMoreTemplatesMessageDialog extends DefaultShell {
	
	public WhereToFindMoreTemplatesMessageDialog() {
		super("Where can you find more examples to use as templates?");
	}

	public String getMessage() {
		// StyledText message is the first Widget
		return new DefaultStyledText(this, 0).getText();
	}

}
