/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at https://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.fusesource.ide.wsdl2rest.ui.internal;

import org.jboss.tools.foundation.core.plugin.log.IPluginLog;
import org.jboss.tools.foundation.ui.plugin.BaseUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Wsdl2RestUIActivator extends BaseUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.fusesource.ide.wsdl2rest.ui"; //$NON-NLS-1$

	// The shared instance
	private static Wsdl2RestUIActivator plugin;

	/**
	 * The constructor
	 */
	public Wsdl2RestUIActivator() {
		// empty
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		setInstance(this);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		setInstance(null);
		super.stop(context);
	}
	
	private static synchronized void setInstance(Wsdl2RestUIActivator wsdl2RestUIActivator) {
		Wsdl2RestUIActivator.plugin = wsdl2RestUIActivator;
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Wsdl2RestUIActivator getDefault() {
		return plugin;
	}

	/**
	 * Get the IPluginLog for this plugin. This method 
	 * helps to make logging easier, for example:
	 * 
	 *     FoundationCorePlugin.pluginLog().logError(etc)
	 *  
	 * @return IPluginLog object
	 */
	public static IPluginLog pluginLog() {
		return getDefault().pluginLogInternal();
	}
}
