package org.ihtsdo.otf.rest.client.ims;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IMSRestClient {

	private static final String DOT_SEPARATOR = ".";

	private static final String IMS_PATTERN = "ims-ihtsdo=";

	private static final String SEMICOLON_SEPARATOR = ";";

	private static final String SET_COOKIE = "Set-Cookie";

	private static final String COOKIE = "Cookie";

	private final RestTemplate restTemplate;

	private final String imsUrl;
	private final ObjectMapper objectMapper;

	public IMSRestClient(String imsUrl) {
		this.imsUrl = imsUrl;
		this.restTemplate = new RestTemplate();
		objectMapper = new ObjectMapper();
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

		Map<String, String> bodyMap = new HashMap<>();
		bodyMap.put("login", username);
		bodyMap.put("password", password);

		HttpHeaders headers = getRequestHeadersWithAuthenticationToken();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Accept", "application/json, text/plain, */*");

		HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(bodyMap), headers);
		ResponseEntity<String> model = restTemplate.exchange(imsUrl + "/authenticate", HttpMethod.POST, request, String.class);
		return getAuthenticationToken(model.getHeaders());
	}

	public String loginForceNewSession(String username, String password) throws IOException, URISyntaxException {
		String token = login(username, password);
		logout(token);
		return login(username, password);
	}

	private void logout(String token) throws IOException {
		HttpHeaders headers = getRequestHeadersWithAuthenticationToken();
		headers.add(HttpHeaders.COOKIE, token);
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);
		restTemplate.exchange(imsUrl + "/account/logout", HttpMethod.POST, request, String.class);
	}

	private String getAuthenticationToken(HttpHeaders responseHeader) throws URISyntaxException {
		String cookies = responseHeader.get(SET_COOKIE).toString();
		String[] strArr = cookies.substring(1, cookies.length() - 1).split(SEMICOLON_SEPARATOR);
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

	private HttpHeaders getRequestHeadersWithAuthenticationToken() throws IOException {
		URLConnection connection = new URL(imsUrl).openConnection();
		List<String> cookies = connection.getHeaderFields().get(SET_COOKIE);
		
		String set_cookie = cookies.toString();
		set_cookie = set_cookie.substring(1, set_cookie.length() - 1);

		HttpHeaders headers = new HttpHeaders();
		headers.add(COOKIE, set_cookie);

		return headers;
	}

}
