/*******************************************************************************
 * Copyright (c) 2015 Frank Becker and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Frank Becker - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.bugzilla.rest.core;

import org.eclipse.mylyn.tasks.core.data.AbstractTaskSchema;
import org.eclipse.mylyn.tasks.core.data.DefaultTaskSchema;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

import com.google.common.collect.ImmutableMap;

public class BugzillaRestCreateTaskSchema extends AbstractTaskSchema {

	private static final BugzillaRestCreateTaskSchema instance = new BugzillaRestCreateTaskSchema();

	public static BugzillaRestCreateTaskSchema getDefault() {
		return instance;
	}

	private static ImmutableMap<String, String> attribute2FieldMapper = new ImmutableMap.Builder()
			.put(getDefault().SUMMARY.getKey(), "summary")
			.put(getDefault().DESCRIPTION.getKey(), "description")
			.put(getDefault().OPERATION.getKey(), "status")
			.put(getDefault().PRODUCT.getKey(), "product")
			.put(getDefault().COMPONENT.getKey(), "component")
			.put(getDefault().CC.getKey(), "cc")
			.put(getDefault().SEVERITY.getKey(), "severity")
			.put(getDefault().PRIORITY.getKey(), "priority")
			.put(getDefault().ASSIGNED_TO.getKey(), "assigned_to")
			.put(getDefault().OS.getKey(), "op_sys")
			.put(getDefault().VERSION.getKey(), "version")
			.build();

	public static String getFieldNameFromAttributeName(String attributeName) {
		String result = attribute2FieldMapper.get(attributeName);
		if (result == null) {
			result = attributeName;
		}
		return result;
	}

	private final DefaultTaskSchema parent = DefaultTaskSchema.getInstance();

	public final Field PRODUCT = inheritFrom(parent.PRODUCT).addFlags(Flag.REQUIRED).create();

	public final Field COMPONENT = inheritFrom(parent.COMPONENT).addFlags(Flag.REQUIRED)
			.dependsOn(PRODUCT.getKey())
			.create();

	public final Field SUMMARY = inheritFrom(parent.SUMMARY).addFlags(Flag.REQUIRED).create();

	public final Field VERSION = createField(TaskAttribute.VERSION, "Version", TaskAttribute.TYPE_SINGLE_SELECT, null,
			PRODUCT.getKey(), Flag.ATTRIBUTE, Flag.REQUIRED);

	public final Field DESCRIPTION = inheritFrom(parent.DESCRIPTION).addFlags(Flag.REQUIRED).create();

	public final Field OS = createField("os", "OS", TaskAttribute.TYPE_SINGLE_SELECT, Flag.ATTRIBUTE);

	public final Field PLATFORM = createField("platform", "Platform", TaskAttribute.TYPE_SINGLE_SELECT, Flag.ATTRIBUTE);

	public final Field PRIORITY = inheritFrom(parent.PRIORITY).create();

	public final Field SEVERITY = inheritFrom(parent.SEVERITY).create();

	public final Field STATUS = inheritFrom(parent.STATUS).create();

	public final Field ALIAS = createField("alias", "Alias", TaskAttribute.TYPE_SHORT_TEXT, Flag.ATTRIBUTE);

	public final Field ASSIGNED_TO = inheritFrom(parent.USER_ASSIGNED).label("Assigned to")
			.dependsOn(COMPONENT.getKey())
			.create();

	public final Field CC = createField(TaskAttribute.USER_CC, "CC", TaskAttribute.TYPE_PERSON, Flag.PEOPLE);

	public final Field ADD_SELF_CC = inheritFrom(parent.ADD_SELF_CC).addFlags(Flag.PEOPLE).create();

	public final Field DESCRIPTION_IS_PRIVATE = createField("description_is_private", "Description is private",
			TaskAttribute.TYPE_BOOLEAN, Flag.ATTRIBUTE);

	public final Field QA_CONTACT = createField("qa_contact", "QA Contact", TaskAttribute.TYPE_PERSON, null,
			COMPONENT.getKey(), Flag.PEOPLE);

	public final Field TARGET_MILESTONE = createField("target_milestone", "Target milestone",
			TaskAttribute.TYPE_SINGLE_SELECT, null, PRODUCT.getKey(), Flag.ATTRIBUTE, Flag.REQUIRED);

	public final Field RESOLUTION = inheritFrom(parent.RESOLUTION).create();

	public final Field OPERATION = createField(TaskAttribute.OPERATION, "Operation", TaskAttribute.TYPE_OPERATION);
}
