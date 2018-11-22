package org.ihtsdo.otf.rest.client.ims;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class IMSRestClient {

	private static final String DOT_SEPARATOR = ".";

	private static final String IMS_PATTERN = "ims-ihtsdo=";

	private static final String SEMICOLUM_SEPARATOR = ";";

	private static final String TOKEN_PATTERN = "[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}";

	private static final String SET_COOKIE = "Set-Cookie";

	private static final String COOKIE = "Cookie";

	private static final String CSRF_TOKEN = "CSRF_TOKEN";

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
		Map<String, String> requestMap = getRequestHeadersMap();

		URI uri = new URI(imsUrl + "/j_security_check");
		MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<String, String>();
		bodyMap.add("j_username", username);
		bodyMap.add("j_password", password);
		bodyMap.add("_spring_security_remember_me", "true");
		bodyMap.add("submit", "Login");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.add("Accept", "application/json, text/plain, */*");
		headers.add("Origin", imsUrl);
		headers.add("X-CSRF-TOKEN", requestMap.get(CSRF_TOKEN));
		headers.add("Cookie", requestMap.get(COOKIE));

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(bodyMap, headers);
		ResponseEntity<String> model = restTemplate.exchange(uri, HttpMethod.POST, request, String.class);
		return getAuthenticationToken(model.getHeaders());
	}

	private String getAuthenticationToken(HttpHeaders responeHeader) throws URISyntaxException {
		System.out.println(responeHeader);
		String cookies = responeHeader.get(SET_COOKIE).toString();
		String[] strArr = cookies.substring(1, cookies.length() - 1).split(SEMICOLUM_SEPARATOR);
		Pattern r = Pattern.compile(IMS_PATTERN);
		List<String> tokens = new ArrayList<String>();
		for (String str : strArr) {
			Matcher m = r.matcher(str);
			if (m.find()) {
				tokens.add(str);
			}
		}
		
		// find corresponding token with IMS environment
		URI uri = new URI(imsUrl);
		String imsHost = uri.getHost();
		String imsEnvrionemnt = imsHost.substring(0, imsHost.indexOf(DOT_SEPARATOR));
		for (String token : tokens) {
			if (token.indexOf(imsEnvrionemnt) != -1) {
				return token;
			}
		}
		return "";
	}

	private Map<String, String> getRequestHeadersMap() throws MalformedURLException, IOException {
		Map<String, String> requestMap = new HashMap<>();
		URLConnection connection = new URL(imsUrl).openConnection();
		List<String> cookies = connection.getHeaderFields().get(SET_COOKIE);
		
		String set_cookie = cookies.toString();
		set_cookie = set_cookie.substring(1, set_cookie.length() - 1);
		requestMap.put(COOKIE, set_cookie);

		Pattern r = Pattern.compile(TOKEN_PATTERN);
		Matcher m = r.matcher(set_cookie);
		if (m.find()) {
			requestMap.put(CSRF_TOKEN, m.group(0));
		}

		return requestMap;
	}

}
