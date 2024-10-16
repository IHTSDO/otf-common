package org.ihtsdo.otf.rest.client.traceability;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.lang.StringUtils;
import org.ihtsdo.otf.exception.TermServerScriptException;
import org.ihtsdo.otf.rest.client.ExpressiveErrorHandler;
import org.ihtsdo.otf.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.traceability.domain.Activity;
import org.snomed.otf.traceability.domain.ActivityType;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TraceabilityServiceClient {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceabilityServiceClient.class);
	private static final String CONTENT_TYPE = "application/json";
	private static final int DATA_SIZE = 500;
	private static final String PAGE_PARAM = "&page=";
	private static final String SIZE_PARAM = "&size=";
	private static final TypeReference<List<Activity>> ACTIVITY_RESPONSE_CONTENT_TYPE = new TypeReference<>() {};

	private static final String TIMEOUT_MESSAGE = "Timeout while waiting for traceability.  Sleeping 30s then trying again...";

	private static final int BATCH_SIZE = 50;

	private final HttpHeaders headers;
	private final RestTemplate restTemplate;
	private final String serverUrl;
	private final ObjectMapper mapper = new ObjectMapper();

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
 		restTemplate.getInterceptors().add((request, body, execution) -> {
             request.getHeaders().addAll(headers);
             return execution.execute(request, body);
         });

		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);
	}

	public static int getBatchSize() {
		return BATCH_SIZE;
	}

	public List<Activity> getConceptActivity(List<String> conceptIds,  ActivityType activityType, String user) throws InterruptedException {
		if (conceptIds == null || conceptIds.isEmpty()) {
			LOGGER.warn("TraceabilityServiceClient was asked to recover activities for ZERO (0) concepts");
			return new ArrayList<>();
		}
		
		String url = this.serverUrl + "traceability-service/activitiesBulk?activityType=" + activityType;

		if (user != null && !StringUtils.isEmpty(user)) {
			url += "&user=" + user;
		}
		List<Long> conceptIdsL = conceptIds.stream()
				.map(Long::parseLong)
				.toList();
		HttpEntity<List<Long>> requestEntity = new HttpEntity<>(conceptIdsL, headers);
		List<Activity> activities = new ArrayList<>();
		boolean isLast = false;
		int offset = 0;
		url = url + PAGE_PARAM + offset + SIZE_PARAM + DATA_SIZE;
		int failureCount = 0;
		while (!isLast) {
			ResponseEntity<Object> responseEntity;
			try {
				responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Object.class);
			} catch (RestClientResponseException e) {
				if (e.getRawStatusCode()==500) {
					//Are we asking for too much here? Try splitting
					if (conceptIds.size() > BATCH_SIZE / 2) {
						LOGGER.warn("Issue with call to {}", url);
						LOGGER.warn("Received 500 error {} retrying smaller batches",e.toString());
						return retryAsSplit(conceptIds, activityType, user);
					}
					//No need to retry if the server is failing this badly
					throw (e);
				}
				failureCount++;
				if (failureCount > 3) {
					throw e;
				}
				LOGGER.warn(TIMEOUT_MESSAGE);
				Thread.sleep(30*1000L);  //Wait 30 seconds before trying again
				continue;
			}
			LinkedTreeMap<String, Object> responseBody = (LinkedTreeMap<String, Object>) responseEntity.getBody();
			if (responseBody != null) {
				Object content = responseBody.get("content");
				if (content != null) {
					activities.addAll(mapper.convertValue(content, ACTIVITY_RESPONSE_CONTENT_TYPE));
					isLast = Boolean.parseBoolean(responseBody.get("last").toString());
				} else {
					isLast = true;
				}
			} else {
				isLast = true;
			}
		}
		String exampleConceptId = conceptIds.get(0);
		LOGGER.info("Recovered {} activities for {} concepts from {}/{} eg {}", activities.size(), conceptIds.size(), serverUrl, url, exampleConceptId);
		return activities;
	}

	//TODO As per SonarQube, create a filter object to populate and pass, rather than all these parameters
	public List<Activity> getConceptActivity(String conceptId, ActivityType activityType, String fromDate, String toDate, boolean summaryOnly, boolean intOnly, String branchPrefix) throws InterruptedException, TermServerScriptException {
		return getComponentActivity(conceptId, activityType, fromDate, toDate, summaryOnly, intOnly, branchPrefix, true, false);
	}
	
	public List<Activity> getComponentActivity(String componentId, String onBranch) throws InterruptedException, TermServerScriptException {
		//This method should only be used to recover traceability at the component level
		boolean isConcept = false;
		return getComponentActivity(componentId, null, null, null, false, false, onBranch, isConcept, true);
	}

	public List<Activity> getComponentActivity(String componentId, ActivityType activityType, String fromDate, String toDate, boolean summaryOnly, boolean intOnly, String branchPath, boolean isConceptId, boolean useOnBranch) throws InterruptedException, TermServerScriptException {
		if (componentId == null) {
			LOGGER.warn("TraceabilityServiceClient was asked to recover activities for null id component.");
			return new ArrayList<>();
		}
		
		String baseUrl = getActivitiesUrl(componentId, activityType, fromDate, toDate, summaryOnly, intOnly, branchPath, isConceptId, useOnBranch);
		return recoverPagesOfActivities(baseUrl);
	}

	private List<Activity> recoverPagesOfActivities(String baseUrl) throws InterruptedException, TermServerScriptException {
		ActivityPages activityPages = new ActivityPages(baseUrl);
		while (!activityPages.isLast()) {
			recoverPageOfActivities(activityPages);
		}
		LOGGER.info("Recovered total {} activities via baseUrl {}", activityPages.size(), baseUrl);
		return activityPages.getActivities();
	}

	private void recoverPageOfActivities(ActivityPages activityPages) throws InterruptedException, TermServerScriptException {
		ResponseEntity<Object> responseEntity = null;
		try {
			responseEntity = restTemplate.exchange(activityPages.getThisPageUrl(), HttpMethod.GET, null, Object.class);
		} catch (RestClientResponseException e) {
			if (e.getRawStatusCode() == 500) {
				//No need to retry if the server is failing this badly
				throw (e);
			}
			activityPages.recordFailure(e);
			LOGGER.warn(TIMEOUT_MESSAGE);
			Thread.sleep(30*1000L);  //Wait 30 seconds before trying again
		}

		if (responseEntity == null) {
			throw new TermServerScriptException("Failed to recover activities from " + activityPages.getThisPageUrl() + ", null response received from " + activityPages.getThisPageUrl());
		}

		LinkedTreeMap<String, Object> responseBody = (LinkedTreeMap<String, Object>) responseEntity.getBody();
		if (responseBody != null) {
			Object content = responseBody.get("content");
			if (content != null) {
				try {
					List<Activity> thisPageActivities = mapper.convertValue(content, ACTIVITY_RESPONSE_CONTENT_TYPE);
					LOGGER.debug("Recovered {} activities via {}", thisPageActivities.size(), activityPages.getThisPageUrl());
					activityPages.addAll(thisPageActivities);
					activityPages.setIsLast(Boolean.parseBoolean(responseBody.get("last").toString()));
				} catch (NoSuchFieldError e) {
					throw new TermServerScriptException("Failed to parse " + content, e);
				}
			} else {
				activityPages.setIsLast(true);  //Don't try and recover any more if we received an empty set
			}
		} else {
			activityPages.setIsLast(true);  //Don't try and recover any more if we received null!
		}
	}

	//TODO As per SonarQube, create a filter object to populate and pass, rather than all these parameters
	private String getActivitiesUrl(String componentId, ActivityType activityType, String fromDate, String toDate,
			boolean summaryOnly, boolean intOnly, String branchPath, boolean isConceptId, boolean useOnBranch) {
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
		
		return url;
	}

	private List<Activity> retryAsSplit(List<String> conceptIds, ActivityType activityType, String user) throws InterruptedException {
		//Try the conceptIds again, split into batches of 15
		List<List<String>> subBatches = Lists.partition(conceptIds, 10);
		List<Activity> activity = new ArrayList<>();
		for (List<String> subBatch : subBatches) {
			try {
				activity.addAll(getConceptActivity(subBatch, activityType, user));
			} catch (Exception e) {
				String subBatchSctIds = StringUtils.join(subBatch, ", ");
				LOGGER.error("Exception against {} conceptIds {}", activityType, subBatchSctIds);
			}
		}
		return activity;
	}


	public List<Activity> getActivitiesForUsersOnBranches(String componentSubType, String users, String branches, String fromEffectiveTime) throws InterruptedException, TermServerScriptException {
		String baseUrl = this.serverUrl + "traceability-service/activitiesForUsersOnBranches?componentSubType="+componentSubType+"&users=" + users + "&branches=" + branches + "&fromEffectiveTime=" + fromEffectiveTime;
		return recoverPagesOfActivities(baseUrl);
	}

	class ActivityPages {
		String baseUrl;
		List<Activity> activities = new ArrayList<>();
		boolean isLast = false;
		int pageCount = 0;
		int failureCount = 0;

		ActivityPages(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		String getThisPageUrl() {
			return baseUrl + PAGE_PARAM + pageCount + SIZE_PARAM + DATA_SIZE;
		}

		public boolean isLast() {
			return isLast;
		}

		public List<Activity> getActivities() {
			return activities;
		}

		public int size() {
			return activities.size();
		}

		public void recordFailure(RestClientResponseException e) {
			failureCount++;
			if (failureCount > 3) {
				isLast = true;
				throw e;
			}
		}

		public void addAll(List<Activity> thisPageActivities) {
			activities.addAll(thisPageActivities);
			pageCount ++;
		}

		public void setIsLast(boolean isLast) {
			this.isLast = isLast;
		}
	}
}
