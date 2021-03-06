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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.http.HttpStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.mylyn.commons.core.operations.IOperationMonitor;
import org.eclipse.mylyn.commons.repositories.http.core.CommonHttpResponse;
import org.eclipse.mylyn.commons.repositories.http.core.HttpUtil;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.osgi.util.NLS;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

public class BugzillaRestGetTaskData extends BugzillaRestAuthenticatedGetRequest<List<TaskData>> {
	private final String urlSuffix;

	private final TaskRepository taskRepository;

	private final BugzillaRestConnector connector;

	public BugzillaRestGetTaskData(BugzillaRestHttpClient client, BugzillaRestConnector connector, String urlSuffix,
			TaskRepository taskRepository) {
		super(client, "", null); //$NON-NLS-1$
		this.urlSuffix = urlSuffix;
		this.taskRepository = taskRepository;
		this.connector = connector;
	}

	@Override
	protected String getUrlSuffix() {
		return "/bug?" + urlSuffix; //$NON-NLS-1$
	}

	@Override
	protected List<TaskData> parseFromJson(InputStreamReader in) throws BugzillaRestException {
		TypeToken<List<TaskData>> type = new TypeToken<List<TaskData>>() {
		};
		return new GsonBuilder().registerTypeAdapter(type.getType(), new JSonTaskDataDeserializer())
				.create()
				.fromJson(in, type.getType());
	}

	@Override
	protected List<TaskData> doProcess(CommonHttpResponse response, IOperationMonitor monitor)
			throws IOException, BugzillaRestException {
		InputStream is = response.getResponseEntityAsStream();
		InputStreamReader in = new InputStreamReader(is);
		return parseFromJson(in);
	}

	@Override
	protected void doValidate(CommonHttpResponse response, IOperationMonitor monitor)
			throws IOException, BugzillaRestException {
		int statusCode = response.getStatusCode();
		if (statusCode != 400 && statusCode != 200) {
			if (statusCode == HttpStatus.SC_NOT_FOUND) {
				throw new BugzillaRestResourceNotFoundException(
						NLS.bind("Requested resource ''{0}'' does not exist", response.getRequestPath()));
			}
			throw new BugzillaRestException(NLS.bind("Unexpected response from Bugzilla REST server for ''{0}'': {1}",
					response.getRequestPath(), HttpUtil.getStatusText(statusCode)));
		}

	}

	BugzillaRestTaskSchema taskSchema = BugzillaRestTaskSchema.getDefault();

	private class JSonTaskDataDeserializer implements JsonDeserializer<ArrayList<TaskData>> {

		@Override
		public ArrayList<TaskData> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			ArrayList<TaskData> response = new ArrayList<TaskData>();
			BugzillaRestTaskDataHandler dataHandler = (BugzillaRestTaskDataHandler) connector.getTaskDataHandler();
			TaskAttributeMapper mapper = dataHandler.getAttributeMapper(taskRepository);
			for (JsonElement bug : json.getAsJsonObject().get("bugs").getAsJsonArray()) { //$NON-NLS-1$
				JsonObject bugdata = bug.getAsJsonObject();
				TaskData taskData = null;
				Integer taskId = bugdata.get("id").getAsInt(); //$NON-NLS-1$
				taskData = new TaskData(mapper, connector.getConnectorKind(), taskRepository.getRepositoryUrl(),
						taskId.toString());
				try {
					dataHandler.initializeTaskData(taskRepository, taskData, null, null);
				} catch (CoreException e) {
					com.google.common.base.Throwables.propagate(e);
				}
				TaskAttribute idAttribute = taskData.getRoot().getAttribute(taskSchema.BUG_ID.getKey());
				idAttribute.setValue(taskId.toString());
				response.add(taskData);
				for (Entry<String, JsonElement> entry : bugdata.entrySet()) {
					String attributeId = BugzillaRestTaskSchema.getAttributeNameFromFieldName(entry.getKey());
					if (entry.getKey().equals("assigned_to_detail")) { //$NON-NLS-1$
						TaskAttribute attribute = taskData.getRoot().getAttribute(taskSchema.ASSIGNED_TO.getKey());
						JsonElement value = entry.getValue().getAsJsonObject().get("email"); //$NON-NLS-1$//.get("real_name");
						if (attribute != null) {
							attribute.setValue(value.getAsString());
						}
						continue;
					}
					TaskAttribute attribute = taskData.getRoot().getAttribute(attributeId);
					if (attribute != null) {
						JsonElement value = entry.getValue();
						if (!value.isJsonNull()) {
							if (value.isJsonArray()) {
								JsonArray valueArray = value.getAsJsonArray();
								attribute.clearValues();
								for (JsonElement jsonElement : valueArray) {
									attribute.addValue(jsonElement.getAsString());
								}
							} else {
								attribute.setValue(entry.getValue().getAsString());
							}
						}
					}
				}
				BugzillaRestConfiguration config;
				try {
					config = connector.getRepositoryConfiguration(taskRepository);
					if (config != null) {
						config.addValidOperations(taskData);
					}
				} catch (CoreException e) {
					com.google.common.base.Throwables.propagate(e);
				}
			}
			return response;
		}

	}

}