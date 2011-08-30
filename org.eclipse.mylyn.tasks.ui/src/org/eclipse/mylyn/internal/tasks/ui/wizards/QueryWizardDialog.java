/*******************************************************************************
 * Copyright (c) 2011 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.wizards;

import java.util.HashMap;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.mylyn.internal.provisional.commons.ui.dialogs.EnhancedWizardDialog;
import org.eclipse.mylyn.internal.provisional.tasks.ui.wizards.AbstractRepositoryQueryPage2;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class QueryWizardDialog extends EnhancedWizardDialog {

	private static final String UPDATE_BUTTON_KEY = "update"; //$NON-NLS-1$

	private static final String CLEAR_BUTTON_KEY = "clear"; //$NON-NLS-1$

	public static final int UPDATE_BUTTON_ID = 2001;

	public static final int CLEAR_BUTTON_ID = 2002;

	private Button updateButton;

	private Button clearButton;

	private AbstractRepositoryQueryPage2 abstractRepositoryQueryPage;

	public QueryWizardDialog(Shell parentShell, IWizard newWizard) {
		super(parentShell, newWizard);
		setHelpAvailable(false);
		for (IWizardPage page : newWizard.getPages()) {
			if (page instanceof AbstractRepositoryQueryPage2) {
				abstractRepositoryQueryPage = (AbstractRepositoryQueryPage2) page;
			}
		}
	}

	@Override
	protected void createExtraButtons(Composite composite) {
		clearButton = createButton(composite, CLEAR_BUTTON_ID, Messages.QueryWizardDialog_Clear_Fields, false);
		clearButton.setVisible(false);
		setButtonLayoutData(clearButton);
		updateButton = createButton(composite, UPDATE_BUTTON_ID,
				Messages.QueryWizardDialog_Update_Attributes_from_Repository, false);
		updateButton.setImage(TasksUiImages.REPOSITORY_UPDATE_CONFIGURATION.createImage());
		updateButton.setVisible(false);
		setButtonLayoutData(updateButton);
	}

	@Override
	protected void updateExtraButtons() {
		if (abstractRepositoryQueryPage != null) {
			abstractRepositoryQueryPage.setExtraButtonState(clearButton);
			abstractRepositoryQueryPage.setExtraButtonState(updateButton);
		}
	}

	@Override
	protected boolean handleExtraButtonPressed(int buttonId) {
		if (abstractRepositoryQueryPage != null) {
			abstractRepositoryQueryPage.handleExtraButtonPressed(buttonId);
		}
		return false;
	}

	/**
	 * Modeled after WizardDialog.saveAndSetEnabledState(), but that one is private, so create our own
	 */
	@Override
	protected HashMap<String, Boolean> saveAndSetEnabledStateMylyn() {
		HashMap<String, Boolean> savedEnabledState = null;
		if (getShell() != null) {
			savedEnabledState = new HashMap<String, Boolean>();
			if (clearButton != null && clearButton.getShell() == getShell()) {
				savedEnabledState.put(CLEAR_BUTTON_KEY, clearButton.getEnabled());
				clearButton.setEnabled(false);
			}
			if (updateButton != null && updateButton.getShell() == getShell()) {
				savedEnabledState.put(UPDATE_BUTTON_KEY, updateButton.getEnabled());
				updateButton.setEnabled(false);
			}
		}
		return savedEnabledState;
	}

	/**
	 * Modeled after WizardDialog.restoreEnabledState() and WizardDialog.restoreUIState() -- couldn't override those
	 * since they are private, so create our own. Currently only single button to work with, so don't create two
	 * separate methods
	 */
	@Override
	protected void restoreEnabledStateMylyn(HashMap<String, Boolean> savedEnabledState) {
		if (savedEnabledState != null) {
			Boolean savedValidateEnabledState = savedEnabledState.get(CLEAR_BUTTON_KEY);
			if (clearButton != null && savedValidateEnabledState != null) {
				clearButton.setEnabled(savedValidateEnabledState);
			}
			savedValidateEnabledState = savedEnabledState.get(UPDATE_BUTTON_KEY);
			if (updateButton != null && savedValidateEnabledState != null) {
				updateButton.setEnabled(savedValidateEnabledState);
			}
		}
	}
}