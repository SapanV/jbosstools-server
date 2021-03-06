/******************************************************************************* 
 * Copyright (c) 2013 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.ide.eclipse.as.core.server.bean;

import java.io.File;

import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;

public class ServerBeanTypeWildfly80 extends JBossServerType {
	private static final String AS_RELEASE_MANIFEST_KEY = "JBossAS-Release-Version"; //$NON-NLS-1$
	public ServerBeanTypeWildfly80() {
		super(
				"Wildfly", //$NON-NLS-1$
				"Wildfly Application Server", //$NON-NLS-1$
				asPath("modules","system","layers","base",
						"org","jboss","as","server","main"),
				new String[]{V8_0}, new Wildfly80ServerTypeCondition());
	}
	
	protected String getServerTypeBaseName() {
		return getId();
	}
	
	public static class Wildfly80ServerTypeCondition extends AbstractCondition {
		public boolean isServerRoot(File location) {
			return scanManifestPropFromJBossModules(new File[]{new File(location, "modules")}, "org.jboss.as.server", null, AS_RELEASE_MANIFEST_KEY, "8.");
		}
		public String getServerTypeId(String version) {	
			// Just return adapter type wf8 until we discover incompatibility. 
			return IJBossToolingConstants.SERVER_WILDFLY_80;
		}
	}
}
