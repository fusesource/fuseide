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

package org.fusesource.ide.projecttemplates.adopters.configurators;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.fusesource.ide.camel.editor.utils.BuildAndRefreshJobWaiterUtil;
import org.fusesource.ide.projecttemplates.internal.Messages;
import org.fusesource.ide.projecttemplates.internal.ProjectTemplatesActivator;
import org.fusesource.ide.projecttemplates.util.NewProjectMetaData;
import org.fusesource.ide.projecttemplates.util.maven.MavenUtils;

/**
 * this configurator provides helper methods for maven configuration
 * 
 * @author lhein
 */
public class MavenTemplateConfigurator extends DefaultTemplateConfigurator {
	
	@Override
	public boolean configure(IProject project, NewProjectMetaData metadata, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.MavenTemplateConfigurator_ConfiguringTemplatesMonitorMessage, 3);
		boolean ok = super.configure(project, metadata, subMonitor.newChild(1));

		if (ok) {
			// by default add the maven nature
			ok = configureMavenNature(project, subMonitor.newChild(1));
		}
		
		if (ok) {
			// by default configure the version of camel used in the pom.xml
			ok = MavenUtils.configureCamelVersionForProject(project, metadata.getCamelVersion(), subMonitor.newChild(1));
		}
		
		return ok;
	}
	
	/**
	 * configures the maven nature for the given project
	 * 
	 * @param project	the project to enable maven nature
	 * @param monitor	the progress monitor
	 * @return	true on success
	 */
	protected boolean configureMavenNature(IProject project, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor,Messages.MavenTemplateConfigurator_ConfiguringMavenNatureMonitorMessage, 4);
		try {
			ResolverConfiguration configuration = new ResolverConfiguration();
			configuration.setResolveWorkspaceProjects(true);
			configuration.setSelectedProfiles(""); //$NON-NLS-1$
			new BuildAndRefreshJobWaiterUtil().waitJob(subMonitor.newChild(1));
			IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();
			configurationManager.enableMavenNature(project, configuration, subMonitor.newChild(1));
			configurationManager.updateProjectConfiguration(project, subMonitor.newChild(1));
			new BuildAndRefreshJobWaiterUtil().waitJob(subMonitor.newChild(1));
        } catch(CoreException ex) {
        	ProjectTemplatesActivator.pluginLog().logError(ex.getMessage(), ex);
        	return false;
        }
		return true;
	}
}
