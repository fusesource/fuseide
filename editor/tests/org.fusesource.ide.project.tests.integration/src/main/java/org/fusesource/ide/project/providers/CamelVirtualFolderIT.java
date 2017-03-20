/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.fusesource.ide.project.providers;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.fusesource.ide.camel.model.service.core.model.CamelFile;
import org.fusesource.ide.camel.model.service.core.tests.integration.core.io.FuseProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CamelVirtualFolderIT {

	@Rule
	public FuseProject fuseProject = new FuseProject(CamelVirtualFolderIT.class.getName());
	
	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();
	
	@Test
	public void testCamelVirtualFolderFindCamelFile() throws Exception {
		fuseProject.createEmptyCamelFile();
		
		CamelVirtualFolder camelVirtualFolder = initializeCamelVirtualFolder();
		
		assertThat(camelVirtualFolder.getCamelFiles()).hasSize(1);
	}
	
	@Test
	public void testCamelVirtualFolderDetectRemovedCamelFile() throws Exception {
		CamelFile camelFile = fuseProject.createEmptyCamelFile();
		CamelVirtualFolder camelVirtualFolder = initializeCamelVirtualFolder();
		assertThat(camelVirtualFolder.getCamelFiles()).hasSize(1);
		
		camelFile.getResource().delete(true, new NullProgressMonitor());
		
		assertThat(camelVirtualFolder.getCamelFiles()).hasSize(0);
	}
	
	@Test
	public void testCamelVirtualFolderIgnoresCamelFilesInTargetFolder() throws Exception {
		IFolder targetFolder = fuseProject.getProject().getFolder("target");
		targetFolder.create(true, true, new NullProgressMonitor());
		IFile camelFileInsideTargetFolder = targetFolder.getFile("emptyCamelFileInTargetFolder.xml");
		fuseProject.createEmptyCamelFile(camelFileInsideTargetFolder);
		
		CamelVirtualFolder camelVirtualFolder = initializeCamelVirtualFolder();
		
		assertThat(camelVirtualFolder.getCamelFiles()).hasSize(0);
	}
	
	private CamelVirtualFolder initializeCamelVirtualFolder() {
		CamelVirtualFolder camelVirtualFolder = new CamelVirtualFolder(fuseProject.getProject());
		camelVirtualFolder.populateChildren();
		return camelVirtualFolder;
	}
}
