package org.ihtsdo.otf.rest.client;


import java.io.IOException;

import org.ihtsdo.otf.rest.client.resty.RestyHelper;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

public class OrchestrationRestClient {

	public static final String ANY_CONTENT_TYPE = "*/*";
	public static final String JSON_CONTENT_TYPE = "application/json";
	public static final String VALIDATION_ENDPOINT = "REST/ts/validate";

	private final String orchestrationUrl;
	private final RestyHelper resty;

	public OrchestrationRestClient(String orchestrationUrl, String username, String password) {
		this.orchestrationUrl = orchestrationUrl;
		this.resty = new RestyHelper(ANY_CONTENT_TYPE);
		resty.authenticate(orchestrationUrl, username, password.toCharArray());
	}

	public void validate(String branchPath) throws JSONException, IOException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("branchPath", branchPath);
		resty.json(orchestrationUrl + VALIDATION_ENDPOINT, RestyHelper.content((jsonObject), JSON_CONTENT_TYPE));
	}


}
