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
package org.fusesource.ide.projecttemplates.tests.integration.wizards;

import static org.junit.Assume.assumeFalse;

import java.util.List;

import org.fusesource.ide.camel.model.service.core.catalog.CamelModelFactory;
import org.fusesource.ide.projecttemplates.adopters.util.CamelDSLType;
import org.fusesource.ide.projecttemplates.impl.simple.AMQTemplate;
import org.fusesource.ide.projecttemplates.util.NewProjectMetaData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class FuseIntegrationProjectCreatorRunnableForAMQIT extends FuseIntegrationProjectCreatorRunnableIT {

	@Parameters(name = "{0}")
	public static List<String> parameters(){
		return CamelModelFactory.getSupportedCamelVersions(); 
	}
	
	public FuseIntegrationProjectCreatorRunnableForAMQIT(String version) {
		super();
		camelVersion = version;
	}
	
	@Test
	public void testAMQBlueprintProjectCreation() throws Exception {
		//TODO: Known limitations see https://issues.jboss.org/browse/FUSETOOLS-1986
		assumeFalse("Blueprint with 2.15 redhat version is not working, see https://issues.jboss.org/browse/FUSETOOLS-1986", camelVersion.startsWith("2.15"));
		
		testProjectCreation("-AMQBlueprintProject", CamelDSLType.BLUEPRINT, "src/main/resources/OSGI-INF/blueprint/camel-blueprint.xml", null);
	}

	@Test
	public void testAMQSpringProjectCreation() throws Exception {
		testProjectCreation("-AMQSpringProject", CamelDSLType.SPRING, "src/main/resources/META-INF/spring/camel-context.xml", null);
	}
	
	@Override
	protected NewProjectMetaData createDefaultNewProjectMetadata(CamelDSLType dsl, String projectName) {
		NewProjectMetaData newProjectMetadata = super.createDefaultNewProjectMetadata(dsl, projectName);
		newProjectMetadata.setTemplate(new AMQTemplate());
		newProjectMetadata.setBlankProject(false);
		return newProjectMetadata;
	}
	
}
