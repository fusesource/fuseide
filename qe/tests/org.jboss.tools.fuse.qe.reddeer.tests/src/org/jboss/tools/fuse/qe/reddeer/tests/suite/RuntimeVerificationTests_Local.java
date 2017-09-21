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
package org.jboss.tools.fuse.qe.reddeer.tests.suite;

import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.fuse.qe.reddeer.tests.QuickStartsTest;
import org.jboss.tools.fuse.qe.reddeer.tests.RegressionFuseTest;
import org.jboss.tools.fuse.qe.reddeer.tests.RegressionTest;
import org.jboss.tools.fuse.qe.reddeer.tests.ServerJRETest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import junit.framework.TestSuite;

/**
 * Runs verification tests for a JBoss Fuse runtime.
 * 
 * @author tsedmik
 */
@SuiteClasses({
	RegressionFuseTest.class,
	RegressionTest.class,
	QuickStartsTest.class,
	ServerJRETest.class })
@RunWith(RedDeerSuite.class)
public class RuntimeVerificationTests_Local extends TestSuite {

}
