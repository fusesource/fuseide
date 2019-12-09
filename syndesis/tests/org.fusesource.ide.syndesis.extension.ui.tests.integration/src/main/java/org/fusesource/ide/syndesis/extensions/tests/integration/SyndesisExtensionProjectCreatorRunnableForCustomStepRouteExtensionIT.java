/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.fusesource.ide.syndesis.extensions.tests.integration;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.management.MalformedObjectNameException;

import org.eclipse.core.runtime.CoreException;
import org.fusesource.ide.projecttemplates.adopters.AbstractProjectTemplate;
import org.fusesource.ide.syndesis.extensions.ui.templates.CustomStepAsCamelRouteProjectTemplate;
import org.junit.Ignore;
import org.junit.Test;

public class SyndesisExtensionProjectCreatorRunnableForCustomStepRouteExtensionIT extends SyndesisExtensionProjectCreatorRunnableIT {
	
	@Test
	@Ignore("No more working by default as template not compatible with new version of Maven embedded 3.6.1")
	public void testCustomStepCamelRouteProjectCreation() throws CoreException, IOException, MalformedObjectNameException, InvocationTargetException, InterruptedException {
		testProjectCreation();
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.syndesis.extensions.tests.integration.wizards.SyndesisExtensionProjectCreatorRunnableIT#getTemplate()
	 */
	@Override
	protected AbstractProjectTemplate getTemplate() {
		return new CustomStepAsCamelRouteProjectTemplate();
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.syndesis.extensions.tests.integration.wizards.SyndesisExtensionProjectCreatorRunnableIT#hasCamelRoute()
	 */
	@Override
	protected boolean hasCamelRoute() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.syndesis.extensions.tests.integration.SyndesisExtensionProjectCreatorRunnableIT#getCamelResourcePath()
	 */
	@Override
	protected String getCamelResourcePath() {
		return "src/main/resources/META-INF/syndesis/extensions/log-body-action.xml";
	}
}
