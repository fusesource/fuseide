/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at https://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.fusesource.ide.projecttemplates.wizards.pages.provider;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.fusesource.ide.projecttemplates.wizards.pages.model.CategoryItem;
import org.fusesource.ide.projecttemplates.wizards.pages.model.TemplateItem;
import org.fusesource.ide.projecttemplates.wizards.pages.model.TemplateModel;

/**
 * @author lhein
 *
 */
public class TemplateContentProvider implements ITreeContentProvider {

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(Object element) {
		if (element instanceof TemplateModel) {
			return ((TemplateModel)element).getRootTemplates().toArray();
		} else if (element instanceof CategoryItem) {
			ArrayList<Object> resultSet = new ArrayList<>();
			resultSet.addAll(((CategoryItem)element).getSubCategories());
			resultSet.addAll(((CategoryItem)element).getTemplates());
			return resultSet.toArray();
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof CategoryItem) { 
			return ((CategoryItem)element).getParentCategory();
		} else if (element instanceof TemplateItem) {
			return ((TemplateItem)element).getCategory();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof CategoryItem) {
			CategoryItem cat = (CategoryItem)element;
			return !cat.getTemplates().isEmpty() || !cat.getSubCategories().isEmpty();
		}
		return false;
	}
}
