package org.ihtsdo.otf.rest.client.authoringservices;

import java.net.URI;
import java.util.*;

import org.ihtsdo.otf.rest.client.ExpressiveErrorHandler;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.Status;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.springframework.web.util.UriComponentsBuilder;

public class AuthoringServicesClient {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthoringServicesClient.class);

	private static final String STATUS_PARAM = "statuses";
	private static final String TASKS = "/tasks/";
	private static final String PROJECTS = "projects/";
	
	private final RestTemplate restTemplate;
	private HttpHeaders headers;
	private final String serverUrl;
	private static final String API_ROOT = "authoring-services/";
	
	protected static Gson gson;
	static {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.setPrettyPrinting();
		gsonBuilder.disableHtmlEscaping();
		gsonBuilder.excludeFieldsWithoutExposeAnnotation();
		gson = gsonBuilder.create();
	}

	public AuthoringServicesClient(String serverUrl, String authToken) {
		this.serverUrl = serverUrl;
		updateAuthToken(authToken);
		
		restTemplate = new RestTemplateBuilder()
				.rootUri(this.serverUrl)
				.additionalMessageConverters(new GsonHttpMessageConverter(gson))
				.errorHandler(new ExpressiveErrorHandler())
				.build();
		
		//Add a ClientHttpRequestInterceptor to the RestTemplate to add cookies as required
		restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().addAll(headers);
            return execution.execute(request, body);
        });
	}

	public void updateAuthToken(String authToken) {
		headers = new HttpHeaders();
		headers.add("Cookie", authToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
	}

	public String createTask(String projectKey, String summary, String description) {
		String endPoint = serverUrl + API_ROOT + PROJECTS + projectKey + "/tasks";
		JsonObject requestJson = new JsonObject();
		requestJson.addProperty("summary", summary);
		requestJson.addProperty("description", description);
		HttpEntity<Object> requestEntity = new HttpEntity<>(requestJson, headers);
		Task task = restTemplate.postForObject(endPoint, requestEntity, Task.class);
		return task.getKey();
	}

	public void setEditPanelUIState(String project, String taskKey, String quotedList) {
		String endPoint = serverUrl + API_ROOT + PROJECTS + project + TASKS + taskKey + "/ui-state/edit-panel";
		HttpEntity<String> request = new HttpEntity<>(quotedList, headers);
		restTemplate.postForObject(endPoint, request, Void.class);
	}

	public void setSavedListUIState(String project, String taskKey, Map<String, Object> items) {
		String endPoint = serverUrl + API_ROOT + PROJECTS + project + TASKS + taskKey + "/ui-state/saved-list";
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(items, headers);
		restTemplate.postForObject(endPoint, request, Void.class);
	}

	public String updateTask(String project, String taskKey, String summary, String description, String author, String reviewer) {
		String endPoint = serverUrl + API_ROOT + PROJECTS + project + TASKS + taskKey;
		
		JsonObject requestJson = new JsonObject();
		if (summary != null) {
			requestJson.addProperty("summary", summary);
		}
		
		if (description != null) {
			requestJson.addProperty("description", description);
		}
		
		if (author != null) {
			JsonObject assigneeJson = new JsonObject();
			assigneeJson.addProperty("username", author);
			requestJson.add("assignee", assigneeJson);
		}
		
		if (reviewer != null) {
			requestJson.addProperty("status", "IN_REVIEW");
			JsonArray reviewers = new JsonArray();
			JsonObject reviewerJson = new JsonObject();
			reviewerJson.addProperty("username", reviewer);
			reviewers.add(reviewerJson);
			requestJson.add("reviewers", reviewers);
		}
		
		HttpEntity<Object> requestEntity = new HttpEntity<>(requestJson, headers);
		restTemplate.put(endPoint, requestEntity);
		return taskKey;
	}

	public void updateTask(String project, Task task) {
		String endPoint = serverUrl + API_ROOT + PROJECTS + project + TASKS + task.getKey();
		HttpEntity<Task> requestEntity = new HttpEntity<>(task, headers);
		restTemplate.put(endPoint, requestEntity);
	}

	public void deleteTask(String project, String taskKey, boolean optional) throws RestClientException {
		String endPoint = serverUrl + API_ROOT + PROJECTS + project + TASKS + taskKey;
		try {
			Map<String, String> body = new HashMap<>();
			body.put("status", "DELETED");
			HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
			restTemplate.put(endPoint, request);
		} catch (Exception e) {
			String errStr = "Failed to delete task - " + taskKey;
			if (optional) {
				LOGGER.info("{}: {} - marked as optional so continuing...", errStr, e.getMessage());
			} else {
				throw new RestClientException(errStr, e);
			}
		}
	}

	public Project getProject(String projectStr) throws RestClientException {
		return getProject(projectStr, false);
	}

	public Project getProject(String projectStr, boolean allowFirstTimeRelease) throws RestClientException {
		try {
			String url = serverUrl + API_ROOT + PROJECTS + projectStr;
			LOGGER.debug("Recovering project '{}' via {}", projectStr, url);
			Project project = restTemplate.getForObject(url, Project.class);
			
			if (project.getMetadata() == null) {
				throw new IllegalStateException ("Metadata not populated (at all) on project: " + project.getKey());
			}
			
			if (!allowFirstTimeRelease && project.getMetadata().getPreviousPackage() == null) {
				throw new IllegalStateException ("Metadata item 'previousPackage' not specified on (not first time release) project: " + project.getKey());
			}
			return project;
		} catch (Exception e) {
			throw new RestClientException("Unable to recover project " + projectStr + " due to " + e.getMessage(), e);
		}
	}

	public Task getTask(String taskKey) throws RestClientException {
		String json = "Unknown";
		try {
			String projectStr = taskKey.substring(0, taskKey.indexOf("-"));
			String endPoint = serverUrl + API_ROOT + PROJECTS + projectStr + TASKS + taskKey;
			json = restTemplate.getForObject(endPoint, String.class);
			Task taskObj = gson.fromJson(json, Task.class);
			if (taskObj.getAssignedAuthor() == null && taskObj.getAssignee() != null) {
				taskObj.setAssignedAuthor(taskObj.getAssignee().get("username"));
			}
			return taskObj;
		} catch (Exception e) {
			throw new RestClientException("Unable to recover task '" + taskKey + "' instead received: " + json, e);
		}
	}

	public Classification classify(String taskKey) throws RestClientException {
		try {
			String projectStr = taskKey.substring(0, taskKey.indexOf("-"));
			String endPoint = serverUrl + API_ROOT + PROJECTS + projectStr + TASKS + taskKey + "/classifications";
			HttpEntity<Object> requestEntity = new HttpEntity<>("", headers);
			return restTemplate.postForObject(endPoint, requestEntity, Classification.class);
		} catch (Exception e) {
			throw new RestClientException("Unable to classify " + taskKey, e);
		}
	}

	public Status validate(String taskKey) throws RestClientException {
		try {
			String projectStr = taskKey.substring(0, taskKey.indexOf("-"));
			String endPoint = serverUrl + API_ROOT + PROJECTS + projectStr + TASKS + taskKey + "/validation";
			HttpEntity<String> request = new HttpEntity<>("", headers);
			String json = restTemplate.postForObject(endPoint, request, String.class);
			return gson.fromJson(json, Status.class);
		} catch (Exception e) {
			throw new RestClientException("Unable to initiate validation on " + taskKey, e);
		}
	}

	public List<Project> listProjects() {
		String endPoint = serverUrl + API_ROOT + "projects";
		ParameterizedTypeReference<List<Project>> type = new ParameterizedTypeReference<>() {
        };
		LOGGER.info("Recovering list of visible projects from {}", endPoint);
		return restTemplate.exchange(endPoint, HttpMethod.GET, null, type).getBody();
	}

	public List<Task> listTasksOnProject(String projectKey) {
		String baseUrl = serverUrl + API_ROOT;
		URI uri = UriComponentsBuilder
				.fromUriString(baseUrl + "/projects/tasks/search")
				.queryParam("projectKeys", projectKey)
				.queryParam(STATUS_PARAM, "New")
				.queryParam(STATUS_PARAM, "In Review")
				.queryParam(STATUS_PARAM, "In Progress")
				.queryParam("lightweight", true)
				.build()
				.toUri();ParameterizedTypeReference<List<Task>> type = new ParameterizedTypeReference<>(){};
		LOGGER.info("Recovering list of active tasks from {}", uri);
		return restTemplate.exchange(uri, HttpMethod.GET, null, type).getBody();
	}

}
