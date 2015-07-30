package org.ihtsdo.otf.rest.client;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.ihtsdo.otf.rest.client.resty.RestyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.BinaryResource;
import us.monoid.web.JSONResource;

public class OrchestrationRestClient {

	public static final String ANY_CONTENT_TYPE = "*/*";

	private final String orchestrationUrl;
	private final RestyHelper resty;
	private final Gson gson;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public OrchestrationRestClient(String orchestrationUrl, String username, String password) {
		this.orchestrationUrl = orchestrationUrl;
		this.resty = new RestyHelper(ANY_CONTENT_TYPE);
		resty.authenticate(orchestrationUrl, username, password.toCharArray());
		gson = new GsonBuilder().setPrettyPrinting().create();
	}


}
