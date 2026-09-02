package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.Map;

/**
 * The Snowstorm fsn and pt values including the language code and term.
 * This deserializer flattens that into just the term to match the snow owl response.
 */
public class DescriptionDeserializer extends ValueDeserializer<String> {
	@Override
	public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws JacksonException {
		JsonToken jsonToken = jsonParser.currentToken();
		if (jsonToken == JsonToken.VALUE_STRING) {
			return jsonParser.getValueAsString();
		} else {
			Map description = jsonParser.readValueAs(Map.class);
			Object term = description.get("term");
			return term != null ? (String) term : null;
		}
	}

}
