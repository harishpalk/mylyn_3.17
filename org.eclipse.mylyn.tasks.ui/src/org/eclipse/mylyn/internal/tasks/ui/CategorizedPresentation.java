/*******************************************************************************
 * Copyright (c) 2004, 2008 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui;

import org.eclipse.mylyn.internal.tasks.ui.views.AbstractTaskListContentProvider;
import org.eclipse.mylyn.internal.tasks.ui.views.AbstractTaskListPresentation;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskListContentProvider;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskListView;

/**
 * @author Mik Kersten
 */
public class CategorizedPresentation extends AbstractTaskListPresentation {

	public static final String ID = "org.eclipse.mylyn.tasks.ui.categorized"; //$NON-NLS-1$

	public CategorizedPresentation() {
		super(ID);
	}

	@Override
	protected AbstractTaskListContentProvider createContentProvider(TaskListView taskListView) {
		return new TaskListContentProvider(taskListView);
	}

}
