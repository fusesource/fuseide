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
package org.fusesource.ide.projecttemplates.impl.simple;

import org.fusesource.ide.projecttemplates.adopters.AbstractProjectTemplate;
import org.fusesource.ide.projecttemplates.adopters.creators.DSLDependentUnzipStreamCreator;
import org.fusesource.ide.projecttemplates.adopters.util.CamelDSLType;
import org.fusesource.ide.projecttemplates.wizards.pages.model.EnvironmentData;
import org.fusesource.ide.projecttemplates.wizards.pages.model.FuseDeploymentPlatform;
import org.fusesource.ide.projecttemplates.wizards.pages.model.FuseRuntimeKind;

public abstract class AbstractCBRTemplate extends AbstractProjectTemplate {

	public AbstractCBRTemplate() {
		super();
	}
	
	@Override
	public boolean isCompatible(EnvironmentData environment) {
		return super.isCompatible(environment)
				&& FuseDeploymentPlatform.STANDALONE.equals(environment.getDeploymentPlatform())
				&& FuseRuntimeKind.KARAF.equals(environment.getFuseRuntime());
	}

	@Override
	public boolean supportsDSL(CamelDSLType type) {
		switch (type) {
			case BLUEPRINT:	return true;
			case SPRING:	return true;
			case JAVA:		return true;
			default:		return false;
		}	
	}

	/**
	 * creator class for the CBR simple template 
	 */
	protected class CBRUnzipTemplateCreator extends DSLDependentUnzipStreamCreator {
		
		private static final String TEMPLATE_BLUEPRINT = "template-simple-cbr-blueprint-fuse";
		private static final String TEMPLATE_SPRING = "template-simple-cbr-spring-fuse";
		private static final String TEMPLATE_JAVA = "template-simple-cbr-java-fuse";

		public CBRUnzipTemplateCreator(String suffix) {
			super(TEMPLATE_BLUEPRINT, TEMPLATE_SPRING, TEMPLATE_JAVA, suffix);
		}

	}
	
	@Override
	public boolean isDefault(EnvironmentData environment, CamelDSLType dslType) {
		return isCompatible(environment) && CamelDSLType.BLUEPRINT.equals(dslType);
	}

}
