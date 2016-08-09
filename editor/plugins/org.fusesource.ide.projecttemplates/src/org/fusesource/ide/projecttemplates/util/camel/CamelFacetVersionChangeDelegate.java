/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.fusesource.ide.projecttemplates.util.camel;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.fusesource.ide.camel.model.service.core.catalog.CamelModelFactory;
import org.fusesource.ide.projecttemplates.internal.ProjectTemplatesActivator;
import org.fusesource.ide.projecttemplates.util.maven.MavenUtils;

public class CamelFacetVersionChangeDelegate implements IDelegate {

	// TODO: remove me after release of 8.0.0 or reuse me for another new unreleased camel version
	private static final String CAMEL_STAGING_REPO_URI = "https://repository.jboss.org/nexus/content/repositories/fusesource_releases_external-2384";
	
	@Override
	public void execute(IProject project, IProjectFacetVersion fv, Object config, IProgressMonitor monitor) throws CoreException {
		String newVersion = getCamelVersionForFacetVersion(fv);
		updateCamelVersion(project, newVersion);
	}
	
	private String getCamelVersionForFacetVersion(IProjectFacetVersion fv) {
		String facetVersion = fv.getVersionString();
		String camelVersion = CamelModelFactory.getCamelVersionFor(facetVersion);
		if (camelVersion == null) {
			camelVersion = facetVersion + ".0";
		}
		return camelVersion;
	}
	
	/**
	 * @param project
	 * @param pomFile
	 * @param model
	 */
	private void updateCamelVersion(IProject project, String camelVersion) throws CoreException {
		File pomFile = new File(project.getFile("pom.xml").getLocation().toOSString());
		Model m2m = MavenPlugin.getMaven().readModel(pomFile);

		if (m2m.getDependencyManagement() != null) {
			final List<Dependency> dependencies = m2m.getDependencyManagement().getDependencies();
			MavenUtils.updateCamelVersionDependencies(dependencies, camelVersion);
			MavenUtils.updateContributedDependencies(dependencies, camelVersion);
		}
		MavenUtils.updateCamelVersionDependencies(m2m.getDependencies(), camelVersion);
		MavenUtils.updateContributedDependencies(m2m.getDependencies(), camelVersion);
		final Build m2Build = m2m.getBuild();
		if(m2Build != null){
			final PluginManagement pluginManagement = m2Build.getPluginManagement();
			if (pluginManagement != null) {
				final List<Plugin> pluginManagementPlugins = pluginManagement.getPlugins();
				MavenUtils.updateCamelVersionPlugins(pluginManagementPlugins, camelVersion);
				MavenUtils.updateContributedPlugins(pluginManagementPlugins, camelVersion);
			}
			MavenUtils.updateCamelVersionPlugins(m2Build.getPlugins(), camelVersion);
			MavenUtils.updateContributedPlugins(m2Build.getPlugins(), camelVersion);
		}
		
		// TODO: this block ensures that we have the staging repo for camel 2.17 in our pom.xml
		// so we can find that unreleased camel version. this becomes obsolete once the camel version 
		// has been released and can be disabled / removed / used for a new unreleased camel version 
		MavenUtils.ensureRepositoryExists(m2m.getRepositories(), CAMEL_STAGING_REPO_URI, "camelStaging");
		MavenUtils.ensureRepositoryExists(m2m.getPluginRepositories(), CAMEL_STAGING_REPO_URI, "camelStaging");
		// END OF TODO block
		
		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(pomFile))){
		    MavenPlugin.getMaven().writeModel(m2m, os);
		} catch (Exception ex) {
			ProjectTemplatesActivator.pluginLog().logError(ex);
		} finally {
			IFile pomIFile2 = project.getProject().getFile("pom.xml");
			if (pomIFile2 != null) {
				pomIFile2.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		    }
		}
	}
}
