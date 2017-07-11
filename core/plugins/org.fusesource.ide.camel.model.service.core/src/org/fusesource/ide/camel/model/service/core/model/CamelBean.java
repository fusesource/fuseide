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
package org.fusesource.ide.camel.model.service.core.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fusesource.ide.camel.model.service.core.catalog.Parameter;
import org.fusesource.ide.camel.model.service.core.catalog.eips.Eip;
import org.w3c.dom.Node;

/**
 * @author bfitzpat
 */
public class CamelBean extends GlobalDefinitionCamelModelElement {

	public static final String PROP_ID = "id"; //$NON-NLS-1$
	public static final String PROP_CLASS = "class"; //$NON-NLS-1$
	public static final String PROP_SCOPE = "scope"; //$NON-NLS-1$
	public static final String PROP_DEPENDS_ON = "depends-on"; //$NON-NLS-1$
	public static final String PROP_INIT_METHOD = "init-method"; //$NON-NLS-1$
	public static final String PROP_DESTROY_METHOD = "destroy-method"; //$NON-NLS-1$
	public static final String PROP_FACTORY_METHOD = "factory-method"; //$NON-NLS-1$
	public static final String PROP_FACTORY_BEAN = "factory-bean"; //$NON-NLS-1$
	public static final String ARG_TYPE = "type"; //$NON-NLS-1$
	public static final String ARG_VALUE = "value"; //$NON-NLS-1$
	public static final String PROP_NAME = "name"; //$NON-NLS-1$
	public static final String PROP_VALUE = "value"; //$NON-NLS-1$
	public static final String TAG_PROPERTY = "property"; //$NON-NLS-1$
	public static final String TAG_ARGUMENT = "argument"; //$NON-NLS-1$
	public static final String TAG_CONSTRUCTOR_ARG = "constructor-arg"; //$NON-NLS-1$

	/**
	 * @param parent
	 * @param underlyingNode
	 */
	public CamelBean(AbstractCamelModelElement parent, Node underlyingNode) {
		super(parent, underlyingNode);
		setUnderlyingMetaModelObject(new GlobalBeanEIP());
	}
	
	public CamelBean(String name) {
		super(null, null);
		setUnderlyingMetaModelObject(new GlobalBeanEIP());
		setParameter(PROP_CLASS, name);
	}
	
	public String getClassName() {
		return (String)getParameter(PROP_CLASS);
	}
	public void setClassName(String name) {
		setParameter(PROP_CLASS, name);
	}
	public String getScope() {
		return (String)getParameter(PROP_SCOPE);
	}
	public void setScope(String value) {
		setParameter(PROP_SCOPE, value);
	}
	public String getDependsOn() {
		return (String)getParameter(PROP_DEPENDS_ON);
	}
	public void setDependsOn(String value) {
		setParameter(PROP_DEPENDS_ON, value);
	}
	public String getInitMethod() {
		return (String)getParameter(PROP_INIT_METHOD);
	}
	public void setInitMethod(String value) {
		setParameter(PROP_INIT_METHOD, value);
	}
	public String getDestroyMethod() {
		return (String)getParameter(PROP_DESTROY_METHOD);
	}
	public void setDestroyMethod(String value) {
		setParameter(PROP_DESTROY_METHOD, value);
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.model.AbstractCamelModelElement#getKind(java.lang.String)
	 */
	@Override
	public String getKind(String name) {
		// due to the missing EIP as underlying meta model we have to tell AbstractCamelModelElement what
		// kind the attribute is ... so if we got other than ATTRIBUTE please adapt this methods logic!
		return NODE_KIND_ATTRIBUTE;
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.model.AbstractCamelModelElement#parseAttributes()
	 */
	@Override
	protected void parseAttributes() {
		// there is no model info for beans so we need to parse them manually
		initAttribute(PROP_ID);
		initAttribute(PROP_CLASS);
		initAttribute(PROP_DEPENDS_ON);
		initAttribute(PROP_DESTROY_METHOD);
		initAttribute(PROP_FACTORY_BEAN);
		initAttribute(PROP_FACTORY_METHOD);
		initAttribute(PROP_INIT_METHOD);
		initAttribute(PROP_SCOPE);
	}
	
	private void initAttribute(String paramName) {
		String value = parseAttribute(paramName);
		if (value != null) {
			setParameter(paramName, value);
		}
	}	
	
	private String parseAttribute(String name) {
		Node tmp = getXmlNode().getAttributes().getNamedItem(name);
		if (tmp != null) {
			return tmp.getNodeValue();
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.model.AbstractCamelModelElement#shouldParseNode()
	 */
	@Override
	protected boolean shouldParseNode() {
		// we do want to parse bean contents
		return true;
	}

	class GlobalBeanEIP extends Eip {
		
		private Map<String, Object> parameters = new HashMap<>();
		
		public GlobalBeanEIP() {
			Parameter p = createParameter(PROP_ID, String.class.getName());
			p.setRequired("true");
			parameters.put(p.getName(), p);
			p = createParameter(PROP_CLASS, String.class.getName());
			p.setRequired("true");
			parameters.put(p.getName(), p);
			p = createParameter(PROP_SCOPE, String.class.getName());
			parameters.put(p.getName(), p);
			p = createParameter(PROP_DEPENDS_ON, String.class.getName());
			parameters.put(p.getName(), p);
			p = createParameter(PROP_INIT_METHOD, String.class.getName());
			parameters.put(p.getName(), p);
			p = createParameter(PROP_DESTROY_METHOD, String.class.getName());
			parameters.put(p.getName(), p);
			p = createParameter(PROP_FACTORY_METHOD, String.class.getName());
			parameters.put(p.getName(), p);
			
			setParameters(parameters);
		}
		
		/* (non-Javadoc)
		 * @see org.fusesource.ide.camel.model.service.core.catalog.eips.Eip#canHaveChildren()
		 */
		@Override
		public boolean canHaveChildren() {
			return true;
		}
		
		/* (non-Javadoc)
		 * @see org.fusesource.ide.camel.model.service.core.catalog.eips.Eip#canBeAddedToCamelContextDirectly()
		 */
		@Override
		public boolean canBeAddedToCamelContextDirectly() {
			return false;
		}
		
		/* (non-Javadoc)
		 * @see org.fusesource.ide.camel.model.service.core.catalog.eips.Eip#getAllowedChildrenNodeTypes()
		 */
		@Override
		public List<String> getAllowedChildrenNodeTypes() {
			return Arrays.asList(PROP_NAME);
		}
		
		/* (non-Javadoc)
		 * @see org.fusesource.ide.camel.model.service.core.catalog.eips.Eip#getKind()
		 */
		@Override
		public String getKind() {
			return "model";
		}
		
		/* (non-Javadoc)
		 * @see org.fusesource.ide.camel.model.service.core.catalog.eips.Eip#getName()
		 */
		@Override
		public String getName() {
			return BEAN_NODE;
		}
		
		/* (non-Javadoc)
		 * @see org.fusesource.ide.camel.model.service.core.catalog.eips.Eip#getTitle()
		 */
		@Override
		public String getTitle() {
			return BEAN_NODE;
		}
		
		private Parameter createParameter(String name, String jType) {
			Parameter outParm = new Parameter();
			outParm.setName(name);
			outParm.setJavaType(jType);
			return outParm;
		}
	}
}