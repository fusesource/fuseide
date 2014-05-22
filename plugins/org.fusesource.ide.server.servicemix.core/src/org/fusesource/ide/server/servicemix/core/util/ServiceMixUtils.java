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

package org.fusesource.ide.server.servicemix.core.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author lhein
 */
public class ServiceMixUtils {
	/**
	 * checks if the given folder contains a valid smx version with the given
	 * version (must match version to return true)
	 * 
	 * @param path		the path to the installation
	 * @param version	the version to match or null if version doesn't matter
	 * @return	true if smx install folder and version matches
	 */
	public static boolean isValidServiceMixInstallation(File path, String version) {
		boolean valid = false;
		
		if (path != null && path.isDirectory()) {
			File[] folders = path.listFiles(new FileFilter() {
				/*
				 * (non-Javadoc)
				 * @see java.io.FileFilter#accept(java.io.File)
				 */
				public boolean accept(File checkFile) {
					return checkFile.isDirectory() && 
						(checkFile.getName().equalsIgnoreCase("bin") ||
						 checkFile.getName().equalsIgnoreCase("etc") ||
					     checkFile.getName().equalsIgnoreCase("deploy") ||
						 checkFile.getName().equalsIgnoreCase("system"));
				}
			});
			// all folders must be there
			if (folders.length == 4) {
				// now check for servicemix-version.jar
				valid = getVersion(path) != null;
				
				// now check if the version matches
				if (valid) valid = checkVersion(path, version);
			}
		}
				
		return valid;
	}
	
	/**
	 * retrieves the version of the installation
	 * 
	 * @param installFolder	the installation folder
	 * @return	the version string or null on errors
	 */
	public static String getVersion(File installFolder) {
		String version = null;
		
		String jarPath = String.format("%s%slib%sservicemix-version.jar", installFolder.getPath(), File.separator, File.separator);
		File f = new File(jarPath);
		JarFile jar = null;
		if (f.exists() && f.isFile()) {
			try {
				jar = new JarFile(f);
			} catch (IOException ex) {
				jar = null;
			}
		}
		Manifest mf = null;
		if (jar != null) {
			try {
				mf = jar.getManifest();
			} catch (IOException ex) {
				mf = null;
			}
		}
		
		if (mf != null) {
			Attributes am = mf.getMainAttributes();
			if (am != null) {
				version = am.getValue("Bundle-Version");
			}
		}
		
		return version;
	}
	
	/**
	 * checks if the version required is matched
	 * 
	 * @param installFolder	the installation folder
	 * @param version		the required version or null
	 * @return	true if version is null or matched
	 */
	public static boolean checkVersion(File installFolder, String version) {
		boolean match = false;
		
		if (version != null) {
			// we will check for correct version string (. is appended if not there)
			String checkVersion = (version.trim().endsWith(".") ? version : version + ".");
			String jarVersion = getVersion(installFolder);
			if (jarVersion != null && jarVersion.trim().startsWith(checkVersion)) {
				// bundle name and version matches
				match = true;
			}
		} else {
			// no version specified so always true
			match = true;
		}
		
		return match;
	}
}