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

package org.fusesource.ide.camel.editor.globalconfiguration.beans.provider;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.fusesource.ide.camel.editor.globalconfiguration.beans.wizards.AddGlobalBeanWizard;
import org.fusesource.ide.camel.editor.globalconfiguration.beans.wizards.EditGlobalBeanWizard;
import org.fusesource.ide.camel.editor.provider.ext.GlobalConfigElementType;
import org.fusesource.ide.camel.editor.provider.ext.GlobalConfigurationTypeWizard;
import org.fusesource.ide.camel.editor.provider.ext.ICustomGlobalConfigElementContribution;
import org.fusesource.ide.camel.editor.utils.GlobalConfigUtils;
import org.fusesource.ide.camel.model.service.core.catalog.Dependency;
import org.fusesource.ide.camel.model.service.core.catalog.cache.CamelCatalogCacheManager;
import org.fusesource.ide.camel.model.service.core.catalog.cache.CamelModel;
import org.fusesource.ide.camel.model.service.core.catalog.components.Component;
import org.fusesource.ide.camel.model.service.core.model.AbstractCamelModelElement;
import org.fusesource.ide.camel.model.service.core.model.CamelFile;
import org.fusesource.ide.camel.model.service.core.model.eips.GlobalBeanEIP;
import org.fusesource.ide.foundation.core.util.CamelUtils;

/**
 * @author brianf
 *
 */
public class GlobalBeanContributor implements ICustomGlobalConfigElementContribution {

	private AddGlobalBeanWizard wizard = null;
	private GlobalConfigUtils globalConfigUtils = null;

	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.editor.provider.ext.ICustomGlobalConfigElementContribution#createGlobalElement(org.w3c.dom.Document)
	 */
	@Override
	public GlobalConfigurationTypeWizard createGlobalElement(CamelFile camelFile) {
		wizard = createAddWizard(camelFile);
		return wizard;
	}

	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.editor.provider.ext.ICustomGlobalConfigElementContribution#modifyGlobalElement(org.w3c.dom.Document)
	 */
	@Override
	public GlobalConfigurationTypeWizard modifyGlobalElement(CamelFile camelFile) {
		return createEditWizard(camelFile);
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.editor.provider.ext.ICustomGlobalConfigElementContribution#getElementDependencies()
	 */
	@Override
	public List<Dependency> getElementDependencies() {
		if (wizard != null) {
			final Component component = wizard.getComponent();
			if (component != null) {
				return component.getDependencies();
			}
		}
		return Collections.emptyList();
	}

	@Override
	public void onGlobalElementDeleted(AbstractCamelModelElement camelModelElement) {
		// possible actions if one of my nodes got deleted from the context
	}

	@Override
	public boolean canHandle(AbstractCamelModelElement camelModelElementToHandle) {
		// to avoid clash with SAP extension, needs to NOT handle a bean with the class 
		// org.fusesource.camel.component.sap.SapConnectionConfiguration
		
		if (globalConfigUtils == null) {
			globalConfigUtils = new GlobalConfigUtils();
		}
		boolean isBeanElement = 
				CamelUtils.getTagNameWithoutPrefix(camelModelElementToHandle.getXmlNode()).equalsIgnoreCase(CamelFile.BEAN_NODE);
		boolean isSAPClass = true;
		if (isBeanElement) {
			Object classParm = camelModelElementToHandle.getParameter(GlobalBeanEIP.PROP_CLASS);
			if (classParm != null && classParm instanceof String) {
				isSAPClass = "org.fusesource.camel.component.sap.SapConnectionConfiguration".equals((String)classParm); //$NON-NLS-1$
			}
		}
		if (!globalConfigUtils.isSAPExtInstalled() || !isSAPClass) {
			return isBeanElement;
		}
		return false;
	}

	@Override
	public GlobalConfigElementType getGlobalConfigElementType() {
		return GlobalConfigElementType.GLOBAL_BEAN;
	}

	/**
	 * Creates edit wizard
	 * 
	 * @return Bean edit wizard
	 */
	private EditGlobalBeanWizard createEditWizard(CamelFile camelFile) {
		IProject project = camelFile.getResource().getProject();
		final CamelModel camelModel = CamelCatalogCacheManager.getInstance().getCamelModelForProject(project);
		EditGlobalBeanWizard editWizard = new EditGlobalBeanWizard(camelFile, camelModel);
		editWizard.init();
		return editWizard;
	}

	/**
	 * Creates edit wizard
	 * 
	 * @return Bean edit wizard
	 */
	private AddGlobalBeanWizard createAddWizard(CamelFile camelFile) {
		IProject project = camelFile.getResource().getProject();
		final CamelModel camelModel = CamelCatalogCacheManager.getInstance().getCamelModelForProject(project);
		AddGlobalBeanWizard addWizard = new AddGlobalBeanWizard(camelFile, camelModel);
		addWizard.init();
		return addWizard;
	}
}
