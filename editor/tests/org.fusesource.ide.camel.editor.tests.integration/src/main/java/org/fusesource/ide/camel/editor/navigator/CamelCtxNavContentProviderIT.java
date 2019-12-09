/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.fusesource.ide.camel.editor.navigator;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.fusesource.ide.camel.model.service.core.model.CamelFile;
import org.fusesource.ide.camel.model.service.core.tests.integration.core.io.FuseProject;
import org.junit.Rule;
import org.junit.Test;

public class CamelCtxNavContentProviderIT {

	@Rule
	public FuseProject fuseProject = new FuseProject(getClass().getSimpleName());
	
	@Test
	public void testRouteLoadedWithOneRoute() throws Exception {
		Display display = PlatformUI.getWorkbench().getDisplay();
		TreeViewer treeViewer = new TreeViewer(display.getActiveShell());
		treeViewer.setContentProvider(new CamelCtxNavContentProvider());
		treeViewer.setLabelProvider(new CamelCtxNavLabelProvider());
		CamelFile camelFileWithOneRoute = fuseProject.createEmptyCamelFile();
		treeViewer.setInput(camelFileWithOneRoute.getResource());
		treeViewer.refresh();
		Job.getJobManager().join(CamelCtxNavContentProvider.JOB_FAMILY, new NullProgressMonitor());
		while(display.readAndDispatch()) { }
		TreeItem[] items = treeViewer.getTree().getItems();
		assertThat(items).hasSize(1);
		assertThat(items[0].getText()).isEqualTo("Route route3");
	}
	
}
