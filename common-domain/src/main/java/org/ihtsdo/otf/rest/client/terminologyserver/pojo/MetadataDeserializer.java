package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.lang.reflect.Type;
import java.util.*;

import com.google.gson.*;

public class MetadataDeserializer implements JsonDeserializer<Metadata> {

	private static final String prefix = "requiredLanguageRefset.";
	@Override
	public Metadata deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		//For countries that only use a single refset, we have elements like:
		//"requiredLanguageRefset.sv": "46011000052107"
		//And there's just no way to specify a Java variable like that.
		//So we're going to have to loop through all the variable to idenitfy those
		//and force them into the same structure as requiredLanguageRefsets.
		Gson gson = new Gson();
		Metadata metadata = gson.fromJson(json.getAsJsonObject(), Metadata.class);

		//Let's also work this object as a map so we can find the tricky cases
		@SuppressWarnings("unchecked")
		Map<String,Object> elementMap = (Map<String,Object>) gson.fromJson(json, Map.class);
		for (String elementName : elementMap.keySet()) {
			if (elementName.startsWith(prefix)) {
				//Force this into the same structure as if we had multiple lang refsets
				List<Map<String,String>> langRefMapList = metadata.getRequiredLanguageRefsets(true);
				Map<String,String> langRefMap = new HashMap<>();
				String langCode = elementName.substring(prefix.length());
				langRefMap.put(langCode, elementMap.get(elementName).toString());
				langRefMapList.add(langRefMap);
			}
		}
		return metadata;
	}

}
