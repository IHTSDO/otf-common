package org.ihtsdo.otf.rest.client;

import org.ihtsdo.otf.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientResponseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.stream.Collectors;

public class ExpressiveErrorHandler extends DefaultResponseErrorHandler {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void handleError(URI url, HttpMethod method, ClientHttpResponse response) {
		// Recover error code and message from response
		int statusCode = 0;
		String statusText = "";
		String errMsg;
		try {
			statusCode = response.getStatusCode().value();
			statusText = response.getStatusText();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody()))) {
				errMsg = reader.lines().collect(Collectors.joining("\n"));
			}
		} catch (IOException ignored) {
			errMsg = "Unable to recover failure reason";
		}

		if (StringUtils.isEmpty(errMsg)) {
			errMsg = "HTTP Status " + statusCode + " received";
		}

		logger.info("Got REST client error {} - {}", statusCode, statusText);
		throw new RestClientResponseException(errMsg, statusCode, statusText, null, null, null);
	}
}
