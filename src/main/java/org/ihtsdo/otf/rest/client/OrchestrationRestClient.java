package org.ihtsdo.otf.rest.client;

import org.ihtsdo.otf.rest.client.resty.RestyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.web.JSONResource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
		final String url = orchestrationUrl + VALIDATIONS_ENDPOINT + "/" + branchPath + "/latest";
		final JSONResource resource = getResource(url);
		if (resource != null) {
			return resource.toObject().toString();
		} else {
			return null;
		}
	}

	public List<String> retrieveValidationStatuses(List<String> branchPaths) throws IOException, JSONException {
		StringBuilder url = new StringBuilder(orchestrationUrl + VALIDATIONS_ENDPOINT + "/bulk/latest/statuses");
		url.append("?paths=");
		boolean first = true;
		for (String branchPath : branchPaths) {
			if (!first) {
				url.append(",");
			} else {
				first = false;
			}
			url.append(branchPath);
		}
		final JSONResource resource = getResource(url.toString());
		if (resource == null) {
			return null;
		}
		final JSONArray array = resource.array();
		List<String> statuses = new ArrayList<>();
		for (int a = 0; a < array.length(); a++) {
			final String string = array.getString(a);
			statuses.add(!string.equals("null") ? string : null);
		}
		return statuses;
	}

	private JSONResource getResource(String url) throws IOException, JSONException {
		try {
			logger.info("URL '{}'", url);
			final JSONResource json = resty.json(url);
			String httpStatusStr = "Unrecoverable";
			Integer httpStatus = json.getHTTPStatus();
			if (httpStatus != null) {
				httpStatusStr = httpStatus.toString();
			}
			logger.info("URL '{}', response status '{}'", url, httpStatusStr);
			if (httpStatusStr.startsWith("2")) {
				return json;
			}
		} catch (FileNotFoundException e) {
			// Swallowing this. Gulp
		}
		return null;
	}

}
