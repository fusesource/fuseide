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
package org.fusesource.ide.camel.editor.provider;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ActiveMQPaletteEntryDependenciesManagerTest {

	@Test
	public void testUpdateDependenciesFor2151_621084Branded() throws Exception {
		List<Dependency> currentDependencies = createDependenciesListWithActiveMQ();
		new ActiveMQPaletteEntryDependenciesManager().updateDependencies(currentDependencies , "2.15.1.redhat-621084");
		assertThat(currentDependencies.get(0).getVersion()).isEqualTo(ActiveMQPaletteEntryDependenciesManager.camelToAMQVersionMapping.get("2.15.1")+".redhat-621084");
	}

	@Test
	public void testUpdateDependenciesFor2151_621117Branded() throws Exception {
		List<Dependency> currentDependencies = createDependenciesListWithActiveMQ();
		new ActiveMQPaletteEntryDependenciesManager().updateDependencies(currentDependencies , "2.15.1.redhat-621117");
		assertThat(currentDependencies.get(0).getVersion()).isEqualTo(ActiveMQPaletteEntryDependenciesManager.camelToAMQVersionMapping.get("2.15.1")+".redhat-621117");
	}

	@Test
	public void testUpdateDependenciesFor2170_630175Branded() throws Exception {
		List<Dependency> currentDependencies = createDependenciesListWithActiveMQ();
		new ActiveMQPaletteEntryDependenciesManager().updateDependencies(currentDependencies , "2.17.0.redhat-630175");
		assertThat(currentDependencies.get(0).getVersion()).isEqualTo(ActiveMQPaletteEntryDependenciesManager.camelToAMQVersionMapping.get("2.17.0")+".redhat-630175");
	}

	@Test
	public void testUpdateDependenciesFor2173Community() throws Exception {
		List<Dependency> currentDependencies = createDependenciesListWithActiveMQ();
		new ActiveMQPaletteEntryDependenciesManager().updateDependencies(currentDependencies , "2.17.3");
		assertThat(currentDependencies.get(0).getVersion()).isEqualTo(ActiveMQPaletteEntryDependenciesManager.camelToAMQVersionMapping.get("2.17.3"));
	}
	
	private List<Dependency> createDependenciesListWithActiveMQ() {
		List<Dependency> currentDependencies = new ArrayList<>();
		Dependency dep = new Dependency();
		dep.setGroupId(ActiveMQPaletteEntryDependenciesManager.ORG_APACHE_ACTIVEMQ);
		dep.setArtifactId(ActiveMQPaletteEntryDependenciesManager.ACTIVEMQ_CAMEL);
		dep.setVersion("15");
		currentDependencies.add(dep);
		return currentDependencies;
	}

}
