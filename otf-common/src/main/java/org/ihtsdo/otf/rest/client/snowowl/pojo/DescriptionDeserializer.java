package org.ihtsdo.otf.rest.client.snowowl.pojo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * The Snowstorm fsn and pt values including the language code and term.
 * This deserializer flattens that into just the term to match the snow owl response.
 */
public class DescriptionDeserializer extends JsonDeserializer<String> {
	@Override
	public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
		JsonToken jsonToken = jsonParser.getCurrentToken();
		if (jsonToken == JsonToken.VALUE_STRING) {
			return jsonParser.getValueAsString();
		} else {
			Description description = jsonParser.readValueAs(Description.class);
			return description.term;
		}
	}

	private static final class Description {
		String term;
	}
}
