package org.ihtsdo.otf.rest.client.ims;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IMSRestClient {

	private static final String DOT_SEPARATOR = ".";

	private static final String IMS_PATTERN = "ims-ihtsdo=";

	private static final String SEMICOLUM_SEPARATOR = ";";

	private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}");

	private static final String SET_COOKIE = "Set-Cookie";

	private static final String COOKIE = "Cookie";

	private static final String X_CSRF_TOKEN = "X-CSRF-TOKEN";

	private RestTemplate restTemplate;

	private String imsUrl;

	public IMSRestClient(String imsUrl) {
		this.imsUrl = imsUrl;
		this.restTemplate = new RestTemplate();
	}

	/**
	 * A REST endpoint to login and get token for an user.
	 * @param username
	 * @param password
	 * @return Token - The token of login user
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public String login(String username, String password)
			throws URISyntaxException, MalformedURLException, IOException {

		MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<>();
		bodyMap.add("j_username", username);
		bodyMap.add("j_password", password);
		bodyMap.add("submit", "Login");

		HttpHeaders headers = getRequestHeadersWithSessionAndCSRFToken();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.add("Accept", "application/json, text/plain, */*");

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(bodyMap, headers);
		ResponseEntity<String> model = restTemplate.exchange(imsUrl + "/j_security_check", HttpMethod.POST, request, String.class);
		return getAuthenticationToken(model.getHeaders());
	}

	public String loginForceNewSession(String username, String password) throws IOException, URISyntaxException {
		String token = login(username, password);
		logout(token);
		return login(username, password);
	}

	private void logout(String token) throws IOException {
		HttpHeaders headers = getRequestHeadersWithSessionAndCSRFToken();
		headers.add(HttpHeaders.COOKIE, token);
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);
		restTemplate.exchange(imsUrl + "/j_spring_security_logout", HttpMethod.POST, request, String.class);
	}

	private String getAuthenticationToken(HttpHeaders responseHeader) throws URISyntaxException {
		String cookies = responseHeader.get(SET_COOKIE).toString();
		String[] strArr = cookies.substring(1, cookies.length() - 1).split(SEMICOLUM_SEPARATOR);
		Pattern r = Pattern.compile(IMS_PATTERN);
		List<String> tokens = new ArrayList<>();
		for (String str : strArr) {
			Matcher m = r.matcher(str);
			if (m.find()) {
				tokens.add(str);
			}
		}
		
		// find corresponding token with IMS environment
		URI uri = new URI(imsUrl);
		String imsHost = uri.getHost();
		String imsEnvironment = imsHost.substring(0, imsHost.indexOf(DOT_SEPARATOR));
		for (String token : tokens) {
			if (token.contains(imsEnvironment)) {
				return token;
			}
		}
		return "";
	}

	private HttpHeaders getRequestHeadersWithSessionAndCSRFToken() throws IOException {
		URLConnection connection = new URL(imsUrl).openConnection();
		List<String> cookies = connection.getHeaderFields().get(SET_COOKIE);
		
		String set_cookie = cookies.toString();
		set_cookie = set_cookie.substring(1, set_cookie.length() - 1);

		HttpHeaders headers = new HttpHeaders();
		headers.add(COOKIE, set_cookie);

		Matcher matcher = TOKEN_PATTERN.matcher(set_cookie);
		if (matcher.find()) {
			headers.add(X_CSRF_TOKEN, matcher.group(0));
		}

		return headers;
	}

}
