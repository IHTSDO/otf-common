package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.Map;

/**
 * The Snowstorm fsn and pt values including the language code and term.
 * This deserializer flattens that into just the term to match the snow owl response.
 */
public class DescriptionDeserializer extends JsonDeserializer<String> {
	@Override
	public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
		JsonToken jsonToken = jsonParser.getCurrentToken();
		if (jsonToken == JsonToken.VALUE_STRING) {
			return jsonParser.getValueAsString();
		} else {
			Map description = jsonParser.readValueAs(Map.class);
			Object term = description.get("term");
			return term != null ? (String) term : null;
		}
	}

}
