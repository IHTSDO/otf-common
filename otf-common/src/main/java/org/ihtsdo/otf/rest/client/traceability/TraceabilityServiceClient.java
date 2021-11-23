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
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
				.setConnectTimeout(Duration.ofMinutes(3)) // 3 minutes
				.setReadTimeout(Duration.ofMinutes(3))
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
	
	public List<Activity> getConceptActivity(List<String> conceptIds,  ActivityType activityType, String user) throws InterruptedException {
		if (conceptIds == null || conceptIds.size() == 0) {
			logger.warn("TraceabilityServiceClient was asked to recover activities for ZERO (0) concepts");
			return new ArrayList<>();
		}
		
		String url = this.serverUrl + "traceability-service/activitiesBulk?activityType=" + activityType;

		if (user != null && !StringUtils.isEmpty(user)) {
			url += "&user=" + user;
		}
		List<Long> conceptIdsL = conceptIds.stream()
				.map(c -> Long.parseLong(c))
				.collect(Collectors.toList());
		HttpEntity<List<Long>> requestEntity = new HttpEntity<>(conceptIdsL, headers);
		List<Activity> activities = new ArrayList<>();
		boolean isLast = false;
		int offset = 0;
		TypeReference<List<Activity>> responseContentType = new TypeReference<>() {
		};
		url = url + "&offset=" + offset + "&size=" + DATA_SIZE;
		int failureCount = 0;
		while (!isLast) {
			ResponseEntity<Object> responseEntity = null;
			try {
				responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Object.class);
			} catch (RestClientResponseException e) {
				if (e.getRawStatusCode()==500) {
					//No need to retry if the server is failing this badly
					throw (e);
				}
				failureCount++;
				if (failureCount > 3) {
					throw e;
				}
				logger.warn("Timeout while waiting for traceability.  Sleeping 30s then trying again...");
				Thread.sleep(30*1000);  //Wait 30 seconds before trying again
				continue;
			}
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
