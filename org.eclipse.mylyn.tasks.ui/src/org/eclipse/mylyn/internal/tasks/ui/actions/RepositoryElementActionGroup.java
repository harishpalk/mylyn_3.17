/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.AbstractTaskCategory;
import org.eclipse.mylyn.internal.tasks.core.TaskCategory;
import org.eclipse.mylyn.internal.tasks.ui.IDynamicSubMenuContributor;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.internal.tasks.ui.views.Messages;
import org.eclipse.mylyn.internal.tasks.ui.views.UpdateRepositoryConfigurationAction;
import org.eclipse.mylyn.tasks.core.IRepositoryElement;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskContainer;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;

/**
 * @author Steffen Pingel
 */
public class RepositoryElementActionGroup {

	protected static final String ID_SEPARATOR_NEW = "new"; //$NON-NLS-1$

	private static final String ID_SEPARATOR_OPERATIONS = "operations"; //$NON-NLS-1$

	private static final String ID_SEPARATOR_TASKS = "tasks"; //$NON-NLS-1$

	protected static final String ID_SEPARATOR_REPOSITORY = "repository"; //$NON-NLS-1$

	private static final String ID_SEPARATOR_PROPERTIES = "properties"; //$NON-NLS-1$

	private static final String ID_SEPARATOR_NAVIGATE = "navigate"; //$NON-NLS-1$

	private static final String ID_SEPARATOR_OPEN = "open"; //$NON-NLS-1$

	protected static final String ID_SEPARATOR_EDIT = "edit"; //$NON-NLS-1$

	private final CopyTaskDetailsAction copyDetailsAction;

	private final OpenTaskListElementAction openAction;

	private final OpenWithBrowserAction openWithBrowserAction;

	private final DeleteAction deleteAction;

	private final RemoveFromCategoryAction removeFromCategoryAction;

	private final ShowInSearchViewAction showInSearchViewAction;

	private final ShowInTaskListAction showInTaskListAction;

	private final TaskActivateAction activateAction;

	private final TaskDeactivateAction deactivateAction;

	private ISelectionProvider selectionProvider;

	public RepositoryElementActionGroup() {
		activateAction = new TaskActivateAction();
		deactivateAction = new TaskDeactivateAction();

		copyDetailsAction = new CopyTaskDetailsAction();
		copyDetailsAction.setActionDefinitionId(IWorkbenchActionDefinitionIds.COPY);

		removeFromCategoryAction = new RemoveFromCategoryAction();

		deleteAction = new DeleteAction();
		openAction = new OpenTaskListElementAction();
		openWithBrowserAction = new OpenWithBrowserAction();
		showInSearchViewAction = new ShowInSearchViewAction();
		showInTaskListAction = new ShowInTaskListAction();
	}

	public void setSelectionProvider(ISelectionProvider selectionProvider) {
		if (this.selectionProvider != null) {
			this.selectionProvider.removeSelectionChangedListener(openWithBrowserAction);
			this.selectionProvider.removeSelectionChangedListener(showInSearchViewAction);
			this.selectionProvider.removeSelectionChangedListener(copyDetailsAction);
			this.selectionProvider.removeSelectionChangedListener(deleteAction);
			this.selectionProvider.removeSelectionChangedListener(openAction);
			this.selectionProvider.removeSelectionChangedListener(removeFromCategoryAction);
		}
		this.selectionProvider = selectionProvider;
		if (selectionProvider != null) {
			selectionProvider.addSelectionChangedListener(openWithBrowserAction);
			selectionProvider.addSelectionChangedListener(showInSearchViewAction);
			selectionProvider.addSelectionChangedListener(copyDetailsAction);
			selectionProvider.addSelectionChangedListener(deleteAction);
			selectionProvider.addSelectionChangedListener(openAction);
			selectionProvider.addSelectionChangedListener(removeFromCategoryAction);
		}
	}

	public void fillContextMenu(final IMenuManager manager) {
		manager.add(new Separator(ID_SEPARATOR_NEW)); // new, mark, schedule
		manager.add(new Separator(ID_SEPARATOR_OPEN)); // open, activate
		manager.add(new Separator(ID_SEPARATOR_EDIT)); // cut, copy paste, delete, rename
		manager.add(new Separator(ID_SEPARATOR_TASKS)); // move to
		manager.add(new Separator(ID_SEPARATOR_NAVIGATE));
		manager.add(new Separator(ID_SEPARATOR_REPOSITORY)); // synchronize
		manager.add(new Separator(ID_SEPARATOR_OPERATIONS)); // import/export, context
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(new Separator(ID_SEPARATOR_PROPERTIES)); // properties

		final ITaskContainer element;
		IStructuredSelection selection = getSelection();
		final Object firstSelectedObject = selection.getFirstElement();
		if (firstSelectedObject instanceof ITaskContainer) {
			element = (ITaskContainer) firstSelectedObject;
		} else {
			element = null;
		}
		final List<IRepositoryElement> selectedElements = getSelectedTaskContainers(selection);
		AbstractTask task = null;
		if (element instanceof ITask) {
			task = (AbstractTask) element;
		}

		if (element instanceof ITask && !isInEditor()) {
			addAction(ID_SEPARATOR_OPEN, openAction, manager, element);
		}
		addAction(ID_SEPARATOR_OPEN, openWithBrowserAction, manager, element);
		showInSearchViewAction.selectionChanged(selection);
		if (showInSearchViewAction.isEnabled()) {
			manager.appendToGroup(ID_SEPARATOR_OPEN, showInSearchViewAction);
		}
		showInTaskListAction.selectionChanged(selection);
		if (showInTaskListAction.isEnabled() && !isInTaskList()) {
			manager.appendToGroup(ID_SEPARATOR_OPEN, showInTaskListAction);
		}
		if (task != null) {
			if (task.isActive()) {
				manager.appendToGroup(ID_SEPARATOR_OPEN, deactivateAction);
			} else {
				manager.appendToGroup(ID_SEPARATOR_OPEN, activateAction);
			}
		}

		Map<String, List<IDynamicSubMenuContributor>> dynamicMenuMap = TasksUiPlugin.getDefault().getDynamicMenuMap();
		for (final String menuPath : dynamicMenuMap.keySet()) {
			for (final IDynamicSubMenuContributor contributor : dynamicMenuMap.get(menuPath)) {
				SafeRunnable.run(new ISafeRunnable() {
					public void handleException(Throwable e) {
						StatusHandler.log(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, "Menu contributor failed")); //$NON-NLS-1$
					}

					public void run() throws Exception {
						MenuManager subMenuManager = contributor.getSubMenuManager(selectedElements);
						if (subMenuManager != null) {
							addMenuManager(menuPath, subMenuManager, manager, element);
						}
					}

				});
			}
		}

		addAction(ID_SEPARATOR_EDIT, copyDetailsAction, manager, element);
		if (!isInEditor()) {
			addAction(ID_SEPARATOR_EDIT, deleteAction, manager, element);
		}
		removeFromCategoryAction.selectionChanged(selection);
		removeFromCategoryAction.setEnabled(isRemoveFromCategoryEnabled(selectedElements));
		if (removeFromCategoryAction.isEnabled()) {
			manager.appendToGroup(ID_SEPARATOR_EDIT, removeFromCategoryAction);
		}

		if (element instanceof IRepositoryQuery) {
			EditRepositoryPropertiesAction repositoryPropertiesAction = new EditRepositoryPropertiesAction();
			repositoryPropertiesAction.selectionChanged(new StructuredSelection(element));
			if (repositoryPropertiesAction.isEnabled()) {
				MenuManager subMenu = new MenuManager(Messages.TaskListView_Repository);
				manager.appendToGroup(ID_SEPARATOR_REPOSITORY, subMenu);

				UpdateRepositoryConfigurationAction resetRepositoryConfigurationAction = new UpdateRepositoryConfigurationAction();
				resetRepositoryConfigurationAction.selectionChanged(new StructuredSelection(element));
				subMenu.add(resetRepositoryConfigurationAction);
				subMenu.add(new Separator());
				subMenu.add(repositoryPropertiesAction);
			}
		}
	}

	private boolean isInTaskList() {
		return (this instanceof TaskListViewActionGroup);
	}

	private IStructuredSelection getSelection() {
		ISelection selection = (selectionProvider != null) ? selectionProvider.getSelection() : null;
		if (selection instanceof IStructuredSelection) {
			return (IStructuredSelection) selection;
		}
		return StructuredSelection.EMPTY;
	}

	private boolean isInEditor() {
		return (this instanceof TaskEditorActionGroup);
	}

	private boolean isRemoveFromCategoryEnabled(final List<IRepositoryElement> selectedElements) {
		if (selectedElements.isEmpty()) {
			return false;
		}
		for (IRepositoryElement element : selectedElements) {
			if (element instanceof AbstractTask) {
				boolean hasCategory = false;
				for (ITaskContainer container : ((AbstractTask) element).getParentContainers()) {
					if (container instanceof TaskCategory) {
						hasCategory = true;
					}
				}
				if (!hasCategory) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	private void addMenuManager(String path, IMenuManager menuToAdd, IMenuManager manager, ITaskContainer element) {
		if (element instanceof ITask || element instanceof IRepositoryQuery) {
			manager.appendToGroup(path, menuToAdd);
		}
	}

	private void addAction(String path, Action action, IMenuManager manager, ITaskContainer element) {
		action.setEnabled(false);
		if (element != null) {
			updateActionEnablement(action, element);
		}
		if (action instanceof OpenWithBrowserAction) {
			if (action.isEnabled()) {
				manager.appendToGroup(path, action);
			}
		} else {
			manager.appendToGroup(path, action);
		}
	}

	// TODO move the enablement to the action classes
	private void updateActionEnablement(Action action, ITaskContainer element) {
		if (element instanceof ITask) {
			if (action instanceof OpenWithBrowserAction) {
				if (TasksUiInternal.isValidUrl(((ITask) element).getUrl())) {
					action.setEnabled(true);
				} else {
					action.setEnabled(false);
				}
			} else if (action instanceof OpenTaskListElementAction) {
				action.setEnabled(true);
			} else if (action instanceof CopyTaskDetailsAction) {
				action.setEnabled(true);
			} else if (action instanceof RenameAction) {
				action.setEnabled(true);
			}
		} else if (element != null) {
			if (action instanceof GoIntoAction) {
				TaskCategory cat = (TaskCategory) element;
				if (cat.getChildren().size() > 0) {
					action.setEnabled(true);
				} else {
					action.setEnabled(false);
				}
			} else if (action instanceof OpenTaskListElementAction) {
				action.setEnabled(true);
			} else if (action instanceof CopyTaskDetailsAction) {
				action.setEnabled(true);
			} else if (action instanceof RenameAction) {
				if (element instanceof AbstractTaskCategory) {
					AbstractTaskCategory container = (AbstractTaskCategory) element;
					action.setEnabled(container.isUserManaged());
				} else if (element instanceof IRepositoryQuery) {
					action.setEnabled(true);
				}
			}
		} else {
			action.setEnabled(true);
		}
	}

	public List<IRepositoryElement> getSelectedTaskContainers(IStructuredSelection selection) {
		List<IRepositoryElement> selectedElements = new ArrayList<IRepositoryElement>();
		for (Iterator<?> i = selection.iterator(); i.hasNext();) {
			Object object = i.next();
			if (object instanceof ITaskContainer) {
				selectedElements.add((IRepositoryElement) object);
			}
		}
		return selectedElements;
	}

	public OpenTaskListElementAction getOpenAction() {
		return openAction;
	}

	public TaskActivateAction getActivateAction() {
		return activateAction;
	}

	public DeleteAction getDeleteAction() {
		return deleteAction;
	}

	public CopyTaskDetailsAction getCopyDetailsAction() {
		return copyDetailsAction;
	}

}