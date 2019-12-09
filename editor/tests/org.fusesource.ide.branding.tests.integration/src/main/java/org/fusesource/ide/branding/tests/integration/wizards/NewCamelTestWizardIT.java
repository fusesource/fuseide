/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.fusesource.ide.branding.tests.integration.wizards;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.fusesource.ide.branding.wizards.NewCamelTestWizard;
import org.fusesource.ide.camel.model.service.core.model.CamelFile;
import org.fusesource.ide.camel.model.service.core.tests.integration.core.io.FuseProject;
import org.junit.Rule;
import org.junit.Test;

public class NewCamelTestWizardIT {
	
	@Rule
	public FuseProject fuseProject = new FuseProject(NewCamelTestWizardIT.class.getSimpleName());
	
	@Test
	public void testCreateTestForRouteUsingCamelContext() throws Exception {
		createTestFor(fuseProject.createEmptyCamelFile());
	}
	
	@Test
	public void testCreateTestForRouteUsingRoutesForContainer() throws Exception {
		createTestFor(fuseProject.createEmptyCamelFileWithRoutes());
	}

	@SuppressWarnings("restriction")
	protected void createTestFor(CamelFile camelFile) throws CoreException {
		IProject project = fuseProject.getProject();
		IProjectDescription description = project.getDescription();
		description.setNatureIds(new String[] { JavaCore.NATURE_ID });
		project.setDescription(description, null);
		
		IJavaProject javaProject = JavaCore.create(project);
		javaProject.open(new NullProgressMonitor());
		NewCamelTestWizard newCamelTestWizard = new NewCamelTestWizard();
		newCamelTestWizard.init(PlatformUI.getWorkbench(), new StructuredSelection(camelFile.getResource()));
		WizardDialog wizardDialog = new WizardDialog(Display.getDefault().getActiveShell(), newCamelTestWizard);
		wizardDialog.setBlockOnOpen(false);
		wizardDialog.open();
		NewTypeWizardPage newTypeWizardPage = (NewTypeWizardPage)newCamelTestWizard.getStartingPage();
		newTypeWizardPage.setTypeName("UniqueTestClassNameTest", false);
		assertThat(newCamelTestWizard.performFinish()).isTrue();
		
		assertThat(project.getFile(new Path("src/test/java/UniqueTestClassNameTest.java")).getLocation().toFile()).exists();
	}

}
