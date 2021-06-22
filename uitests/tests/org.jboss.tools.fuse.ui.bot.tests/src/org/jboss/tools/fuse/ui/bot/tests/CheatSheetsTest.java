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
package org.jboss.tools.fuse.ui.bot.tests;

import static org.jboss.tools.fuse.reddeer.wizard.NewFuseIntegrationProjectWizardDeploymentType.STANDALONE;
import static org.junit.Assert.assertTrue;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.cleanworkspace.CleanWorkspaceRequirement;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.fuse.reddeer.view.CheatSheetsView;
import org.jboss.tools.fuse.reddeer.wizard.CheatSheetsWizard;
import org.jboss.tools.fuse.reddeer.wizard.NewFuseIntegrationProjectWizard;
import org.jboss.tools.fuse.reddeer.wizard.NewFuseIntegrationProjectWizardFirstPage;
import org.jboss.tools.fuse.reddeer.wizard.NewFuseIntegrationProjectWizardRuntimePage;
import org.jboss.tools.fuse.reddeer.utils.JDK8Check;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author fpospisi, djelinek
 */
@RunWith(RedDeerSuite.class)
public class CheatSheetsTest {

	public static final String INTRODUCTION = "This cheat sheet walks you through the steps "
			+ "to create a new Fuse Integration project based on Spring Boot.";

	public static final String PERSPECTIVE = "Select Window > Open Perspective > Fuse Integration "
			+ "in the menu bar at the top of the workbench. This step changes the perspective "
			+ "to set up the Eclipse workbench for development of your Apache Camel/Red Hat Fuse " + "routes.";

	public static final String WIZARD = "To create your new project you now need to "
			+ "open the New Integration Project wizard. You can do that by selecting "
			+ "File > New > Fuse Integration Project from the top menu bar.Once it "
			+ "opened, you can specify the location your project should live in. On "
			+ "the next page, you can select specify the target environment for deploying "
			+ "your new project and choose a specific Camel version. On the last page, you "
			+ "can select from a list of available templates. Please select the template "
			+ "Simple log using Spring Boot - Spring DSL from the list.Click on Finish to "
			+ "finally create your new project.";

	@AfterClass
	public static void cleanWorkspace() {
		new CleanWorkspaceRequirement().fulfill();
	}

	/**
	 * <p>
	 * Fuse Integration Project cheat sheet test
	 * </p>
	 * <b>Steps</b>
	 * <ol>
	 * <li>Open Help -> Cheat Sheets -> Red Hat Fuse -> Create a Fuse integration project</li>
	 * <li>Check if texts in all sections are actual.</li>
	 * <li>Activate available HyperLinks and wait for action.</li>
	 * <li>Check according actions.</li>
	 * <li>Go through New Project Wizard.</li>
	 * </ol>
	 */
	@Test
	public void testCheatSheets() {

		/*
		 * For JRE8 check.
		 */
		boolean hasJava8 = JDK8Check.isJava8Available();

		/*
		 * Open Help -> Cheat Sheets -> Red Hat Fuse -> Create a Fuse...
		 */
		CheatSheetsWizard wizard = new CheatSheetsWizard();
		wizard.open();
		wizard.selectCheatSheet("Red Hat Fuse", "Create a Fuse Integration Project based on Camel");
		wizard.finish();

		/*
		 * Open new Workbench View for Cheat Sheets.
		 */
		CheatSheetsView cheatsheets = new CheatSheetsView();
		cheatsheets.open();

		/*
		 * Introduction part After activating 'Click to Begin' introductionLink text changes to 'Click to Restart'.
		 */
		assertTrue("Introduction text not matching.", cheatsheets.sectionHasText("Introduction", INTRODUCTION));
		cheatsheets.selectHyperlink("Click to Begin");
		assertTrue("Introduction hyperlink was not fulfilled correctly. Link text should be changed to Click to Restart.",
				cheatsheets.hyperlinkTextChange("Click to Restart"));

		/*
		 * Open the Fuse Integration Perspective part After activating 'Click to perform' perspectiveLink text changes
		 * to 'Click to redo'.
		 */
		assertTrue("Perspective text not matching.",
				cheatsheets.sectionHasText("Open the Fuse Integration Perspective", PERSPECTIVE));
		cheatsheets.selectHyperlink("Click to perform");
		assertTrue("Perspective hyperlink was not fulfilled correctly. Link text should be changed to Click to redo.",
				cheatsheets.hyperlinkTextChange("Click to redo"));

		/*
		 * Open the New Fuse Project wizard part After activating 'Click to perform' wizardLink text changes to 'Click
		 * to redo'.
		 */
		assertTrue("Wizard text not matching.", cheatsheets.sectionHasText("Open the New Fuse Project wizard", WIZARD));
		cheatsheets.selectHyperlink("Click to perform");

		/*
		 * Wait until Wizard Shell is available. Go through wizard. Check for JRE8.
		 */
		new WaitUntil(new ShellIsAvailable("New Fuse Integration Project"), TimePeriod.DEFAULT);
		NewFuseIntegrationProjectWizard wiz = new NewFuseIntegrationProjectWizard();
		wiz.open();
		NewFuseIntegrationProjectWizardFirstPage firstPage = new NewFuseIntegrationProjectWizardFirstPage(wiz);
		firstPage.setProjectName("CheatSheetsTest");
		wiz.next();
		NewFuseIntegrationProjectWizardRuntimePage secondPage = new NewFuseIntegrationProjectWizardRuntimePage(wiz);
		secondPage.setDeploymentType(STANDALONE);
		wiz.next();
		new FinishButton(wiz).click();

		/*
		 * JRE8 missing handle.
		 */
		if (!hasJava8) {
			JDK8Check.handleMissingJava8();
		}
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		new WaitWhile(new ShellIsAvailable("New Fuse Integration Project"), TimePeriod.getCustom(900));
	}
}
