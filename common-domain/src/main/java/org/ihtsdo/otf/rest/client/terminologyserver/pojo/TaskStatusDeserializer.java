package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import com.google.gson.*;
import java.lang.reflect.Type;

public class TaskStatusDeserializer implements JsonDeserializer<Task.TaskStatus> {

	@Override
	public Task.TaskStatus deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		if (json == null || json.getAsString().isEmpty()) {
			return Task.TaskStatus.UNKNOWN;
		}

		String value = json.getAsString().trim().toUpperCase().replace(" ", "_");

		try {
			return Task.TaskStatus.valueOf(value);
		} catch (IllegalArgumentException e) {
			// If it doesn't match any enum, return UNKNOWN
			return Task.TaskStatus.UNKNOWN;
		}
	}
}
