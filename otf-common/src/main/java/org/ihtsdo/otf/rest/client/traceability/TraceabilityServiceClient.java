package org.ihtsdo.otf.rest.client.traceability;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.lang.StringUtils;
import org.ihtsdo.otf.rest.client.ExpressiveErrorHandler;
import org.ihtsdo.otf.utils.DateUtils;
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
	private final int DATA_SIZE = 500;
	public static int BATCH_SIZE = 50;
	
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
					//Are we asking for too much here? Try splitting
					if (conceptIds.size() > BATCH_SIZE / 2) {
						logger.warn("Issue with call to " + url);
						logger.warn("Received 500 error " + e + " retrying smaller batches");
						return retryAsSplit(conceptIds, activityType, user);
					}
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
	
	public List<Activity> getConceptActivity(String conceptId, ActivityType activityType, String fromDate, String toDate, boolean summaryOnly, boolean intOnly, String branchPrefix) throws InterruptedException {
		return getComponentActivity(conceptId, activityType, fromDate, toDate, summaryOnly, intOnly, branchPrefix, true, false);
	}
	
	public List<Activity> getComponentActivity(String componentId, String onBranch) throws InterruptedException {
		return getComponentActivity(componentId, null, null, null, false, false, onBranch, false, true);
	}

	public List<Activity> getComponentActivity(String componentId, ActivityType activityType, String fromDate, String toDate, boolean summaryOnly, boolean intOnly, String branchPath, boolean isConceptId, boolean useOnBranch) throws InterruptedException {
		if (componentId == null) {
			logger.warn("TraceabilityServiceClient was asked to recover activities for null id component.");
			return new ArrayList<>();
		}
		
		String url = this.serverUrl + "traceability-service/activities?";
		
		if (isConceptId) {
			url += "conceptId=" + componentId;
		} else {
			url += "componentId=" + componentId;
		}
		
		if (activityType != null) {
			url += "&activityType=" + activityType;
		}
		
		if (toDate != null) {
			url += "&commitToDate=" + (toDate.length()==8 ? DateUtils.formatAsISO(toDate) : toDate);
		}
		if (fromDate != null) {
			url += "&commitFromDate=" + (fromDate .length()==8 ? DateUtils.formatAsISO(fromDate) : fromDate);
		}
		if (summaryOnly) {
			url += "&summaryOnly=true";
		}
		if (intOnly) {
			url += "&intOnly=true";
		}
		if (branchPath != null) {
			if (useOnBranch) {
				url += "&branchPrefix=" + branchPath;
			} else {
				//Allow for inclusion of changes made on other projects that have been promoted up and rebased back down 
				//to the project we're interested in.
				url += "&includeHigherPromotions=true&onBranch=" + branchPath;
			}
		}
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
				responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, Object.class);
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
		logger.info("Recovered {} activities for component {} using {}", activities.size(), componentId, url);
		return activities;
	}
	private List<Activity> retryAsSplit(List<String> conceptIds, ActivityType activityType, String user) throws InterruptedException {
		//Try the conceptIds again, split into batches of 15
		List<List<String>> subBatches = Lists.partition(conceptIds, 10);
		List<Activity> activity = new ArrayList<>();
		for (List<String> subBatch : subBatches) {
			try {
				activity.addAll(getConceptActivity(subBatch, activityType, user));
			} catch (Exception e) {
				logger.error("Exception against " + activityType + " conceptIds " + StringUtils.join(subBatch, ", "));
			}
		}
		return activity;
	}


}
