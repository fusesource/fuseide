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
package org.fusesource.ide.camel.model.service.core.debug.model.values;

import java.util.ArrayList;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.fusesource.ide.camel.model.service.core.debug.CamelDebugTarget;
import org.fusesource.ide.camel.model.service.core.debug.model.exchange.Header;
import org.fusesource.ide.camel.model.service.core.debug.model.variables.BaseCamelVariable;
import org.fusesource.ide.camel.model.service.core.debug.model.variables.CamelHeaderVariable;
import org.fusesource.ide.camel.model.service.core.debug.model.variables.CamelHeadersVariable;
import org.fusesource.ide.camel.model.service.core.internal.CamelModelServiceCoreActivator;

/**
 * @author lhein
 */
public class CamelHeadersValue extends BaseCamelValue {
	
	private CamelHeadersVariable parent;
	private ArrayList<IVariable> fVariables = new ArrayList<IVariable>();
	private ArrayList<Header> headers;
	private CamelDebugTarget debugTarget;
	
	/**
	 * 
	 * @param debugTarget
	 * @param value
	 * @param type
	 * @param msg
	 */
	public CamelHeadersValue(CamelDebugTarget debugTarget, ArrayList<Header> headers, Class type, CamelHeadersVariable parent) {
		super(debugTarget, "" + headers.hashCode(), type);
		this.parent = parent;
		this.debugTarget = debugTarget;
		this.headers = headers;
		if (this.headers == null) this.headers = new ArrayList<Header>();
		try {
			initHeaders();
		} catch (DebugException ex) {
			CamelModelServiceCoreActivator.pluginLog().logError(ex);
		}
	}
	
	/**
	 * initialize variables
	 */
	private void initHeaders() throws DebugException {
		BaseCamelVariable var = null;
		BaseCamelValue val = null;

		for (Header h : this.headers) {
			var = new CamelHeaderVariable(this.debugTarget, h.getKey(), String.class, parent);
			val = new CamelHeaderValue(this.fTarget, h, var.getReferenceType());
			var.setValue(val);
			this.fVariables.add(var);
		}
	}

	/**
	 * adds a new header to the message
	 * 
	 * @param key
	 * @param value
	 */
	public void addHeader(String key, String value) {
		try {
			this.debugTarget.getDebugger().setMessageHeaderOnBreakpoint(this.debugTarget.getSuspendedNodeId(), key, value);
			CamelHeaderVariable newVar = new CamelHeaderVariable(debugTarget, key, String.class, parent);
			CamelHeaderValue newVal = new CamelHeaderValue(debugTarget, new Header(key, value, String.class.getName()), String.class);
			newVar.setValue(newVal);
			newVar.markChanged();
			this.fVariables.add(newVar);
		} catch (DebugException ex) {
			CamelModelServiceCoreActivator.pluginLog().logError(ex);
		} finally {
			fireCreationEvent();
		}
	}
	
	/**
	 * deletes the header variable with the given key
	 * 
	 * @param key
	 */
	public void deleteHeader(String key) {
		try {
			IVariable v = null;
			this.debugTarget.getDebugger().removeMessageHeaderOnBreakpoint(this.debugTarget.getSuspendedNodeId(), key);
			for (IVariable var : fVariables) {
				if (((CamelHeaderValue)var.getValue()).getHeader().getKey().equals(key)) {
					v = var;
					break;
				}
			}
			this.fVariables.remove(v);
		} catch (DebugException ex) {
			CamelModelServiceCoreActivator.pluginLog().logError(ex);
		} finally {
			fireChangeEvent(DebugEvent.CONTENT);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.launcher.debug.model.values.BaseCamelValue#hasVariables()
	 */
	@Override
	public boolean hasVariables() throws DebugException {
		return this.fVariables.size()>0;
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.launcher.debug.model.values.BaseCamelValue#getVariables()
	 */
	@Override
	public IVariable[] getVariables() throws DebugException {
		return this.fVariables.toArray(new IVariable[this.fVariables.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.launcher.debug.model.values.BaseCamelValue#getVariableDisplayString()
	 */
	@Override
	protected String getVariableDisplayString() {
		return "MessageHeaders";
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.launcher.debug.model.values.BaseCamelValue#getValueString()
	 */
	@Override
	public String getValueString() throws DebugException {
		StringBuffer sb = new StringBuffer();
		for (IVariable v : this.fVariables) {
			sb.append(v.toString());
			sb.append("\n");
		}
		return sb.toString();
	}
}
