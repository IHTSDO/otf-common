package org.ihtsdo.otf.rest.client.traceability;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.lang.StringUtils;
import org.ihtsdo.otf.rest.client.ExpressiveErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.traceability.domain.Activity;
import org.snomed.otf.traceability.domain.ActivityType;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TraceabilityServiceClient {
	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final HttpHeaders headers;
	private final RestTemplate restTemplate;
	private final String serverUrl;
	ObjectMapper mapper = new ObjectMapper();
	private static final String CONTENT_TYPE = "application/json";
	private final int DATA_SIZE = 1000;
	
	public TraceabilityServiceClient(String serverUrl, String cookie) {
		headers = new HttpHeaders();
		this.serverUrl = serverUrl;
		headers.add("Cookie", cookie);
		headers.add("Accept", CONTENT_TYPE);
		
		restTemplate = new RestTemplateBuilder()
				.additionalMessageConverters(new GsonHttpMessageConverter())
				.errorHandler(new ExpressiveErrorHandler())
				.build();
		
		//Add a ClientHttpRequestInterceptor to the RestTemplate
 		restTemplate.getInterceptors().add(new ClientHttpRequestInterceptor(){
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
				request.getHeaders().addAll(headers);
				return execution.execute(request, body);
			}
		});

		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	public List<Activity> getConceptActivity(List<Long> conceptIds, String commentFilter, ActivityType activityType, String user) {
		if (conceptIds == null || conceptIds.size() == 0) {
			logger.warn("TraceabilityServiceClient was asked to recover activities for ZERO (0) concepts");
			return new ArrayList<>();
		}
		
		String url = this.serverUrl + "traceability-service/activitiesBulk?activityType=" + activityType;
		if (!StringUtils.isEmpty(commentFilter)) {
				url += "&commentFilter="+commentFilter;
		}

		if (user != null && !StringUtils.isEmpty(user)) {
			url += "&user=" + user;
		}

		
		HttpEntity<List<Long>> requestEntity = new HttpEntity<>(conceptIds, headers);
		List<Activity> activities = new ArrayList<>();
		boolean isLast = false;
		int offset = 0;
		TypeReference<List<Activity>> responseContentType = new TypeReference<>() {
		};
		url = url + "&offset=" + offset + "&size=" + DATA_SIZE;
		while (!isLast) {
			ResponseEntity<Object> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Object.class);
			LinkedTreeMap<String, Object> responseBody = (LinkedTreeMap<String, Object>) responseEntity.getBody();
			if (responseBody != null) {
				Object content = responseBody.get("content");
				if (content != null) {
					activities.addAll(mapper.convertValue(content, responseContentType));
					isLast = Boolean.parseBoolean(responseBody.get("last").toString());
				} else {
					isLast = true;
				}
			} else {
				isLast = true;
			}
		}
		logger.info("Recovered {} activities for {} concepts from {}/{} eg {}", activities.size(), conceptIds.size(), serverUrl, url, conceptIds.get(0));
		return activities;
	}

}
