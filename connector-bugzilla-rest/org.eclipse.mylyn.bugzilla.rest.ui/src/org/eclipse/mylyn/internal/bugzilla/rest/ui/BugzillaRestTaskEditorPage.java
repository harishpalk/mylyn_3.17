/*******************************************************************************
 * Copyright (c) 2014 Frank Becker and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Frank Becker - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.bugzilla.rest.ui;

import org.eclipse.mylyn.internal.bugzilla.rest.core.BugzillaRestCore;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;

public class BugzillaRestTaskEditorPage extends AbstractTaskEditorPage {

	public BugzillaRestTaskEditorPage(TaskEditor editor) {
		this(editor, BugzillaRestCore.CONNECTOR_KIND);
	}

	public BugzillaRestTaskEditorPage(TaskEditor editor, String connectorKind) {
		super(editor, connectorKind);
		setNeedsPrivateSection(true);
		setNeedsSubmitButton(true);
	}

}
