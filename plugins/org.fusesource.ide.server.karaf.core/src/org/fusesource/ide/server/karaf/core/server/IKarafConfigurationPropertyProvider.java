/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.fusesource.ide.server.karaf.core.server;

import java.io.File;

/**
 * @author lhein
 */
public interface IKarafConfigurationPropertyProvider {
	
	/**
	 * retrieves a property from a config file and returns it
	 * 
	 * @param propertyName			the name of the property
	 * @param configPropertyFile	the config property file
	 * @return	the value of that property from the file or null if not found
	 */
	public String getConfigurationProperty(String propertyName, File configPropertyFile);
}
