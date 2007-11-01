/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
/**
 * Copied from newsgroup, forwarded from Make Technologies
 */

package org.eclipse.mylyn.internal.tasks.ui.views;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.mylyn.internal.tasks.core.ScheduledTaskContainer;
import org.eclipse.mylyn.internal.tasks.ui.ITaskListNotification;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.monitor.core.DateUtil;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskContainer;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.AbstractTask.RepositoryTaskSyncState;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

/**
 * @author Mik Kersten
 * @author Eric Booth
 * @author Leo Dos Santos - multi-monitor support
 * @author Steffen Pingel
 */
public class TaskListToolTip extends ToolTip {

	private AbstractTaskContainer currentTipElement;

	private List<TaskListToolTipListener> listeners = new ArrayList<TaskListToolTipListener>();

	private boolean visible;

	private Point lastLocation;

	private boolean forced;
	
	public TaskListToolTip(Control control) {
		super(control);
		this.setShift(new Point(0, 1));
	}

	@Override
	protected void afterHideToolTip(Event event) {
		visible = false;
		for (TaskListToolTipListener listener : listeners.toArray(new TaskListToolTipListener[0])) {
			listener.toolTipHidden(event);
		}
	}
	
	public void addTaskListToolTipListener(TaskListToolTipListener listener) {
		listeners.add(listener);
	}

	public void removeTaskListToolTipListener(TaskListToolTipListener listener) {
		listeners.remove(listener);
	}

	private AbstractTaskContainer getTaskListElement(Object hoverObject) {
		if (hoverObject instanceof Widget) {
			Object data = ((Widget) hoverObject).getData();
			if (data != null) {
				if (data instanceof AbstractTaskContainer) {
					return (AbstractTaskContainer) data;
				} else if (data instanceof IAdaptable) {
					return (AbstractTaskContainer) ((IAdaptable) data).getAdapter(AbstractTaskContainer.class);
				}
			}
		}
		return null;
	}

	private String getTitleText(AbstractTaskContainer element) {
		if (element instanceof ScheduledTaskContainer) {
			StringBuilder sb = new StringBuilder();
			sb.append(element.getSummary());
			Calendar start = ((ScheduledTaskContainer) element).getStart();
			sb.append("  [");
			sb.append(DateFormat.getDateInstance(DateFormat.LONG).format(start.getTime()));
			sb.append("]");
			return sb.toString();
		} else if (element instanceof AbstractRepositoryQuery) {
			AbstractRepositoryQuery query = (AbstractRepositoryQuery) element;
			StringBuilder sb = new StringBuilder();
			sb.append(element.getSummary());
			sb.append("  [");
			sb.append(getRepositoryLabel(query.getRepositoryKind(), query.getRepositoryUrl()));
			sb.append("]");
			return sb.toString();
		} else {
			return element.getSummary();
		}
	}

	private String getDetailsText(AbstractTaskContainer element, TaskListView taskListView) {
		if (element instanceof ScheduledTaskContainer) {
			ScheduledTaskContainer container = (ScheduledTaskContainer) element;
			int estimateTotal = 0;
			long elapsedTotal = 0;
			if (taskListView != null) {
				Object[] children = ((TaskListContentProvider) taskListView.getViewer().getContentProvider()).getChildren(element);
				for (Object object : children) {
					if (object instanceof AbstractTask) {
						estimateTotal += ((AbstractTask) object).getEstimateTimeHours();
						elapsedTotal += TasksUiPlugin.getTaskActivityManager().getElapsedTime((AbstractTask) object,
								container.getStart(), container.getEnd());
					}
				}
			}

			StringBuilder sb = new StringBuilder();
			sb.append("Estimate: ");
			sb.append(estimateTotal);
			sb.append(" hours");
			sb.append("\n");
			sb.append("Elapsed: ");
			sb.append(DateUtil.getFormattedDurationShort(elapsedTotal));
			sb.append("\n");
			return sb.toString();
		} else if (element instanceof AbstractTask) {
			AbstractTask task = (AbstractTask) element;
			StringBuilder sb = new StringBuilder();
			sb.append(TasksUiPlugin.getConnectorUi(task.getConnectorKind()).getTaskKindLabel(task));
			String key = task.getTaskKey();
			if (key != null) {
				sb.append(" ");
				sb.append(key);
			}
			sb.append(", ");
			sb.append(task.getPriority());
			sb.append("  [");
			sb.append(getRepositoryLabel(task.getConnectorKind(), task.getRepositoryUrl()));
			sb.append("]");
			sb.append("\n");
			return sb.toString();
		} else {
			return null;
		}
	}

	private String getRepositoryLabel(String repositoryKind, String repositoryUrl) {
		TaskRepository repository = TasksUiPlugin.getRepositoryManager().getRepository(repositoryKind, repositoryUrl);
		if (repository != null) {
			String label = repository.getRepositoryLabel();
			if (label.indexOf("//") != -1) {
				return label.substring((repository.getUrl().indexOf("//") + 2));
			}
			return label + "";
		}
		return "";
	}

	private String getActivityText(AbstractTaskContainer element) {
//		if (element instanceof ScheduledTaskDelegate) {
//			ScheduledTaskDelegate task = (ScheduledTaskDelegate) element;
//
//			StringBuilder sb = new StringBuilder();
//			Date date = task.getScheduledForDate();
//			if (date != null) {
//				sb.append("Scheduled for: ");
//				sb.append(new SimpleDateFormat("E").format(date)).append(", ");
//				sb.append(DateFormat.getDateInstance(DateFormat.LONG).format(date));
//				sb.append(" (").append(DateFormat.getTimeInstance(DateFormat.SHORT).format(date)).append(")\n");
//			}
//
//			long elapsed = TasksUiPlugin.getTaskActivityManager().getElapsedTime(task.getCorrespondingTask(),
//					task.getDateRangeContainer().getStart(), task.getDateRangeContainer().getEnd());
//			String elapsedTimeString = DateUtil.getFormattedDurationShort(elapsed);
//			sb.append("Elapsed: ");
//			sb.append(elapsedTimeString);
//			sb.append("\n");
//
//			return sb.toString();
//		} else 
//			
		if (element instanceof AbstractTask) {
			AbstractTask task = (AbstractTask) element;

			StringBuilder sb = new StringBuilder();
			Date date = task.getScheduledForDate();
			if (date != null) {
				sb.append("Scheduled for: ");
				sb.append(new SimpleDateFormat("E").format(date)).append(", ");
				sb.append(DateFormat.getDateInstance(DateFormat.LONG).format(date));
				sb.append(" (").append(DateFormat.getTimeInstance(DateFormat.SHORT).format(date)).append(")\n");
			}

			long elapsed = TasksUiPlugin.getTaskActivityManager().getElapsedTime(task);
			String elapsedTimeString = DateUtil.getFormattedDurationShort(elapsed);
			sb.append("Elapsed: ");
			sb.append(elapsedTimeString);
			sb.append("\n");

			return sb.toString();
		}
		return null;
	}

	private String getIncommingText(AbstractTaskContainer element) {
		if (element instanceof AbstractTask) {
			AbstractTask task = (AbstractTask) element;
			if (task.getSynchronizationState() == RepositoryTaskSyncState.INCOMING) {
				AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
						task);
				if (connector != null) {
					ITaskListNotification notification = TasksUiPlugin.getDefault().getIncommingNotification(connector,
							task);
					if (notification != null) {
						String res = null;
						if (notification.getDescription() != null) {
							String descriptionText = notification.getDescription();
							if (descriptionText != null && descriptionText.length() > 0) {
								res = descriptionText;
							}
						}
						if (notification.getDetails() != null) {
							String details = notification.getDetails();
							if (details != null && details.length() > 0) {
								res = res == null ? details : res + "\n" + details;
							}
						}
						return res;
					}
				}
			}
		}
		return null;
	}

	private String getStatusText(AbstractTaskContainer element) {
		IStatus status = null;
		if (element instanceof AbstractTask) {
			AbstractTask task = (AbstractTask) element;
			status = task.getSynchronizationStatus();
		} else if (element instanceof AbstractRepositoryQuery) {
			AbstractRepositoryQuery query = (AbstractRepositoryQuery) element;
			status = query.getSynchronizationStatus();
		}

		if (status != null) {
			StringBuilder sb = new StringBuilder();
			sb.append(status.getMessage());
			if (status instanceof RepositoryStatus && ((RepositoryStatus) status).isHtmlMessage()) {
				sb.append(" Please synchronize manually for full error message.");
			}
			return sb.toString();
		}

		return null;
	}

	private ProgressData getProgressData(AbstractTaskContainer element, TaskListView taskListView) {
		if (element instanceof AbstractTask) {
			return null;
		}
		Object[] children = new Object[0];

		if (element instanceof ScheduledTaskContainer && taskListView != null) {
			children = ((TaskListContentProvider) taskListView.getViewer().getContentProvider()).getChildren(element);
		} else {
			children = element.getChildren().toArray();
		}

		int total = children.length;
		int completed = 0;
		for (AbstractTask task : element.getChildren()) {
			if (task.isCompleted()) {
				completed++;
			}
		}

		String text = "Total: " + total + " (Complete: " + completed + ", Incomplete: " + (total - completed) + ")";
		return new ProgressData(completed, total, text);
	}

	private Image getImage(AbstractTaskContainer element) {
		if (element instanceof AbstractRepositoryQuery) {
			AbstractRepositoryQuery query = (AbstractRepositoryQuery) element;
			AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
					query.getRepositoryKind());
			if (connector != null) {
				return TasksUiPlugin.getDefault().getBrandingIcon(connector.getConnectorKind());
			}
		} else if (element instanceof AbstractTask) {
			AbstractTask repositoryTask = (AbstractTask) element;
			AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
					repositoryTask.getConnectorKind());
			if (connector != null) {
				return TasksUiPlugin.getDefault().getBrandingIcon(connector.getConnectorKind());
			}
		} else if (element instanceof ScheduledTaskContainer) {
			return TasksUiImages.getImage(TasksUiImages.CALENDAR);
		}
		return null;
	}

	private Widget getTipWidget(Event event) {
		Point widgetPosition = new Point(event.x, event.y);
		Widget widget = event.widget;
		if (widget instanceof ToolBar) {
			ToolBar w = (ToolBar) widget;
			return w.getItem(widgetPosition);
		}
		if (widget instanceof Table) {
			Table w = (Table) widget;
			return w.getItem(widgetPosition);
		}
		if (widget instanceof Tree) {
			Tree w = (Tree) widget;
			return w.getItem(widgetPosition);
		}

		return null;
	}

	@Override
	protected boolean shouldCreateToolTip(Event event) {
		if (forced) {
			forced = false;
			return true;
		}
		
		currentTipElement = null;

		if (super.shouldCreateToolTip(event)) {
			Widget tipWidget = getTipWidget(event);
			if (tipWidget != null) {
				currentTipElement = getTaskListElement(tipWidget);
			}
		}
		
		return currentTipElement != null;
	}

	@Override
	protected Composite createToolTipContentArea(Event event, Composite parent) {
		assert currentTipElement != null;

		TaskListView taskListView = TaskListView.getFromActivePerspective();

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginWidth = 5;
		gridLayout.marginHeight = 2;
		composite.setLayout(gridLayout);
		composite.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		addIconAndLabel(composite, getImage(currentTipElement), getTitleText(currentTipElement));

		String detailsText = getDetailsText(currentTipElement, taskListView);
		if (detailsText != null) {
			addIconAndLabel(composite, null, detailsText);
		}

		String synchText = getSynchText(currentTipElement);
		if (synchText != null) {
			addIconAndLabel(composite, TasksUiImages.getImage(TasksUiImages.REPOSITORY_SYNCHRONIZE), synchText);
		}

		String activityText = getActivityText(currentTipElement);
		if (activityText != null) {
			addIconAndLabel(composite, TasksUiImages.getImage(TasksUiImages.CALENDAR), activityText);
		}

		String incommingText = getIncommingText(currentTipElement);
		if (incommingText != null) {
			addIconAndLabel(composite, TasksUiImages.getImage(TasksUiImages.OVERLAY_INCOMMING), incommingText);
		}

		ProgressData progress = getProgressData(currentTipElement, taskListView);
		if (progress != null) {
			addIconAndLabel(composite, null, progress.text);

			// label height need to be set to 0 to remove gap below the progress bar 
			Label label = new Label(composite, SWT.NONE);
			GridData labelGridData = new GridData(SWT.FILL, SWT.TOP, true, false);
			labelGridData.heightHint = 0;
			label.setLayoutData(labelGridData);

			Composite progressComposite = new Composite(composite, SWT.NONE);
			GridLayout progressLayout = new GridLayout(1, false);
			progressLayout.marginWidth = 0;
			progressLayout.marginHeight = 0;
			progressComposite.setLayout(progressLayout);
			progressComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

			WorkweekProgressBar taskProgressBar = new WorkweekProgressBar(progressComposite);
			taskProgressBar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			taskProgressBar.reset(progress.completed, progress.total);

			// do we really need custom canvas? code below renders the same
//			IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
//			Color color = themeManager.getCurrentTheme().getColorRegistry().get(
//					TaskListColorsAndFonts.THEME_COLOR_TASK_TODAY_COMPLETED);
//			ProgressBar bar = new ProgressBar(tipShell, SWT.SMOOTH);
//			bar.setForeground(color);
//			bar.setSelection((int) (100d * progress.completed / progress.total));
//			GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
//			gridData.heightHint = 5;
//			bar.setLayoutData(gridData);
		}

		String statusText = getStatusText(currentTipElement);
		if (statusText != null) {
			addIconAndLabel(composite, TasksUiImages.getImage(TasksUiImages.WARNING), statusText);
		}

		visible = true;
		lastLocation = composite.getShell().getLocation();

		return composite;
	}

	private String getSynchText(AbstractTaskContainer element) {
		if (element instanceof AbstractRepositoryQuery) {
			String syncStamp = ((AbstractRepositoryQuery) element).getLastSynchronizedTimeStamp();
			if (syncStamp != null) {
				return "Synchronized: " + syncStamp;
			}
		}
		return null;
	}

	private String removeTrailingNewline(String text) {
		if (text.endsWith("\n")) {
			return text.substring(0, text.length() - 1);
		}
		return text;
	}

	private void addIconAndLabel(Composite parent, Image image, String text) {
		Label imageLabel = new Label(parent, SWT.NONE);
		imageLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		imageLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		imageLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING));
		imageLabel.setImage(image);

		Label textLabel = new Label(parent, SWT.NONE);
		textLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		textLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		textLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER));
		textLabel.setText(removeTrailingNewline(text));
	}

	private static class ProgressData {

		int completed;

		int total;

		String text;

		public ProgressData(int completed, int total, String text) {
			this.completed = completed;
			this.total = total;
			this.text = text;
		}

	}

	public static interface TaskListToolTipListener {
	
		void toolTipHidden(Event event);
		
	}

	public boolean isVisible() {
		return visible;
	}

	public void update(AbstractTaskContainer item) {
		if (!isVisible()) {
			throw new IllegalStateException();
		}

		hide();

		this.forced = true;
		this.currentTipElement = item;
		
		show(lastLocation);
	}
	
}
