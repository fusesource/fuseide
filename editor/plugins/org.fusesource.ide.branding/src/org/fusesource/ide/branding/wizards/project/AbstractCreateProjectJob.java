/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.fusesource.ide.branding.wizards.project;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IWorkingSet;


public abstract class AbstractCreateProjectJob extends WorkspaceJob {

  private final List<IWorkingSet> workingSets;

  public AbstractCreateProjectJob(String name, List<IWorkingSet> workingSets) {
    super(name);
    this.workingSets = workingSets;
  }

  @Override
  public final IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    //setProperty(IProgressConstants.ACTION_PROPERTY, new OpenMavenConsoleAction());
    List<IProject> projects = doCreateMavenProjects(monitor);
    if(projects != null) {
      for(IProject project : projects) {
        addToWorkingSets(project, workingSets);
      }
    }
    return Status.OK_STATUS;
  }

  protected abstract List<IProject> doCreateMavenProjects(IProgressMonitor monitor) throws CoreException;

  // PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets(project, new IWorkingSet[] {workingSet});
  public static void addToWorkingSets(IProject project, List<IWorkingSet> workingSets) {
    if(workingSets != null && !workingSets.isEmpty()) {
      // IAdaptable[] elements = workingSet.adaptElements(new IAdaptable[] {project});
      // if(elements.length == 1) {
      for(IWorkingSet workingSet : workingSets) {
        if (workingSet!=null) {
          IAdaptable[] oldElements = workingSet.getElements();
          IAdaptable[] newElements = new IAdaptable[oldElements.length + 1];
          System.arraycopy(oldElements, 0, newElements, 0, oldElements.length);
          newElements[oldElements.length] = project;
          workingSet.setElements(newElements);
        }
      }
    }
  }

  /*
  protected static ArrayList<IProject> toProjects(List<IMavenProjectImportResult> results) {
    ArrayList<IProject> projects = new ArrayList<IProject>();
    for (IMavenProjectImportResult result : results) {
      if (result.getProject()!=null) {
        projects.add(result.getProject());
      }
    }
    return projects;
  }
  */
}
