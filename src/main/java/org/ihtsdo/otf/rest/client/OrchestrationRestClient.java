package org.ihtsdo.otf.rest.client;

import org.ihtsdo.otf.rest.client.resty.RestyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.monoid.json.JSONException;
import us.monoid.web.JSONResource;

import java.io.FileNotFoundException;
import java.io.IOException;

public class OrchestrationRestClient {

	public static final String ANY_CONTENT_TYPE = "*/*";
	public static final String VALIDATIONS_ENDPOINT = "REST/termserver/validations";

	private final String orchestrationUrl;
	private final RestyHelper resty;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public OrchestrationRestClient(String orchestrationUrl, String username, String password) {
		this.orchestrationUrl = orchestrationUrl;
		this.resty = new RestyHelper(ANY_CONTENT_TYPE);
		resty.authenticate(orchestrationUrl, username, password.toCharArray());
	}

	public String retrieveValidation(String branchPath) throws IOException, JSONException {
		try {
			final String path = orchestrationUrl + VALIDATIONS_ENDPOINT + "/" + branchPath + "/latest";
			logger.info("Path '{}'", path);
			final JSONResource json = resty.json(path);
			final String httpStatus = json.getHTTPStatus().toString();
			logger.info("Path '{}', response '{}'", path, httpStatus);
			if (httpStatus.startsWith("2")) {
				return json.toObject().toString();
			}
		} catch (FileNotFoundException e) {
			// Swallowing this. Gulp
		}
		return null;
	}

}
