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
package org.jboss.tools.fuse.reddeer.utils;

import org.eclipse.reddeer.common.condition.WaitCondition;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.fuse.reddeer.preference.InstalledJREs;

/**
 * Checks, if JDK8 is available.
 * 
 * @author djelinek, fpospisi
 */
public class JDK8Check {

	public static final String JDK_WARNING_MESSAGE = "No Strictly compliant JRE detected";

	/*
	 * Checks, if JDK8 is available.
	 */
	public static boolean isJava8Available() {
		WorkbenchPreferenceDialog prefs = new WorkbenchPreferenceDialog();
		InstalledJREs jres = new InstalledJREs(prefs);
		prefs.open();
		prefs.select(jres);
		boolean hasJava8 = jres.containsJreWithName(".*(jdk8|jdk-1.8|1.8.0).*");
		prefs.ok();
		return hasJava8;
	}

	/*
	 * Handles missing JDK8 while creating new project.
	 */
	public static void handleMissingJava8() {
		DefaultShell warningMessage = new DefaultShell(JDK_WARNING_MESSAGE);
		WaitCondition wait = new ShellIsAvailable(warningMessage);
		new WaitUntil(wait, TimePeriod.getCustom(900), false);
		if (wait.getResult() != null) {
			new OkButton(warningMessage).click();
		}
	}

}
