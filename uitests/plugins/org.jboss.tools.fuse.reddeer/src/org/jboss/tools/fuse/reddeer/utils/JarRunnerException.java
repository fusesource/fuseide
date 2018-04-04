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
package org.jboss.tools.fuse.reddeer.utils;

/**
 * 
 * @author Andrej Podhradsky (apodhrad@redhat.com)
 *
 */
public class JarRunnerException extends RuntimeException {

	public static final long serialVersionUID = 1L;

	public JarRunnerException(String message) {
		super(message);
	}

	public JarRunnerException(Throwable cause) {
		super(cause);
	}

	public JarRunnerException(String message, Throwable cause) {
		super(message, cause);
	}

}
