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

import static org.jboss.tools.fuse.reddeer.editor.CamelEditor.SOURCE_TAB;
import static org.jboss.tools.fuse.reddeer.editor.CamelEditor.DESIGN_TAB;
import static org.jboss.tools.fuse.reddeer.wizard.NewFuseIntegrationProjectWizardDeploymentType.STANDALONE;
import static org.jboss.tools.fuse.reddeer.wizard.NewFuseIntegrationProjectWizardRuntimeType.SPRINGBOOT;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.cleanworkspace.CleanWorkspaceRequirement;
import org.jboss.tools.fuse.reddeer.ProjectTemplate;
import org.jboss.tools.fuse.reddeer.editor.CamelEditor;
import org.jboss.tools.fuse.reddeer.editor.SourceEditor;
import org.jboss.tools.fuse.reddeer.utils.ProjectFactory;
import org.jboss.tools.fuse.reddeer.view.FusePropertiesView;
import org.jboss.tools.fuse.reddeer.view.FusePropertiesView.DetailsProperty;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author fpospisi
 */
@RunWith(RedDeerSuite.class)
public class PropertiesDropDownChoicesTest {
	public static final String PROJECT_NAME = "propertiesDropDown";
	public static String CAMEL_CONTEXT = "camel-context.xml";
	public static String COMPONENT = "direct:name";

	/*
	 * Create an integration project with an XML DSL
	 */
	@BeforeClass
	public static void setWorkspace() {
		ProjectFactory.newProject(PROJECT_NAME).deploymentType(STANDALONE).runtimeType(SPRINGBOOT)
				.template(ProjectTemplate.SPRINGBOOT).create();
	}

	@AfterClass
	public static void cleanWorkspace() {
		new CleanWorkspaceRequirement().fulfill();
	}

	/**
	 * <p>
	 * Removing Pattern attribute from XML representation of Component via Property View Test.
	 * </p>
	 * <b>Steps</b>
	 * <ol>
	 * <li>Create an integration project with an XML DSL.</li>
	 * <li>Add a component to the Route Canvas.</li>
	 * <li>Open the properties view and select the component.</li>
	 * <li>Select the details tab.</li>
	 * <li>Add an exchange pattern.</li>
	 * <li>Review the XML; pattern should be populated correctly.</li>
	 * <li>Go back to the design tab in the editor.</li>
	 * <li>Select the same component.</li>
	 * <li>Select the Properties View.</li>
	 * <li>Select the Design Tab.</li>
	 * <li>Select the empty value in the Exchange Pattern drop down.</li>
	 * <li>Save.</li>
	 * <li>Review the XML.</li>
	 * <li>Attribute should be gone.</li>
	 * </ol>
	 */
	@Test
	public void testPropertiesDropDownChoices() {
		/*
		 * Add a component to the Route Canvas. 
		 * Open the properties view and select the component. 
		 * Select the details tab. 
		 * Add an exchange pattern.
		 */
		CamelEditor.switchTab(DESIGN_TAB);
		CamelEditor editor = new CamelEditor();
		editor.activate();
		editor.addComponent("Direct", "Log");
		FusePropertiesView properties = new FusePropertiesView();
		properties.setDetailsProperty(DetailsProperty.PATTERN, "InOnly");
		editor.save();

		/*
		 * Review the XML; pattern should be populated correctly.
		 */
		assertTrue("XML should contain pattern attribute.", containsPatterns(getLineWithComponent()));

		/*
		 * Go back to the design tab in the editor. 
		 * Select the same component. 
		 * Select the Properties View. 
		 * Select the empty value in the Exchange Pattern drop down. 
		 * Save.
		 */
		CamelEditor.switchTab(DESIGN_TAB);
		editor.activate();
		editor.clickOnEditPart(COMPONENT);
		FusePropertiesView properties2 = new FusePropertiesView();
		properties2.setDetailsProperty(DetailsProperty.PATTERN, "");
		editor.save();

		/*
		 * Review the XML; Component shouldn't contain pattern attribute.
		 */
		assertTrue("XML shouldn't contain pattern attribute.", !containsPatterns(getLineWithComponent()));
	}

	/**
	 * Finds line with requested component in camel-context.xml.
	 * 
	 * @return String containing requested component.
	 */
	private String getLineWithComponent() {
		CamelEditor.switchTab(SOURCE_TAB);
		SourceEditor sEditor = new SourceEditor();
		Pattern pattern = Pattern.compile("<.*" + COMPONENT + ".*/>", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(sEditor.getText());
		boolean matchFound = matcher.find();
		String founded = matcher.group();
		if (!matchFound) {
			return null;
		} else {
			return founded;
		}
	}

	/**
	 * Checks if input Component (represented by XML line) contains Pattern attribute.
	 *
	 * @param input
	 *                  XML representation of Component.
	 * @return true/false
	 */
	private boolean containsPatterns(String input) {
		Pattern pattern = Pattern.compile("pattern", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(input);
		if (matcher.find()) {
			return true;
		} else {
			return false;
		}
	}
}
