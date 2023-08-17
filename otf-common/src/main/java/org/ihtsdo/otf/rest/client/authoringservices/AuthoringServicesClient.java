package org.ihtsdo.otf.rest.client.authoringservices;

import java.io.IOException;
import java.util.List;

import org.ihtsdo.otf.rest.client.ExpressiveErrorHandler;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.Status;
import org.ihtsdo.otf.rest.client.resty.RestyHelper;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Classification;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Project;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Task;
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

import us.monoid.json.JSONObject;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;

public class AuthoringServicesClient {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private RestTemplate restTemplate;
	private HttpHeaders headers;
	private final Resty resty;
	private final String serverUrl;
	private static final String apiRoot = "authoring-services/";
	private static final String ALL_CONTENT_TYPE = "*/*";
	private static final String JSON_CONTENT_TYPE = "application/json";
	
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
		resty = new Resty(new RestyOverrideAccept(ALL_CONTENT_TYPE));
		resty.withHeader("Cookie", authToken);
		resty.withHeader("Connection", "close");
		resty.authenticate(this.serverUrl, null,null);
		
		updateAuthToken(authToken);
		
		restTemplate = new RestTemplateBuilder()
				.rootUri(this.serverUrl)
				.additionalMessageConverters(new GsonHttpMessageConverter())
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

	public String createTask(String projectKey, String summary, String description) throws Exception {
		String endPoint = serverUrl + apiRoot + "projects/" + projectKey + "/tasks";
		JsonObject requestJson = new JsonObject();
		requestJson.addProperty("summary", summary);
		requestJson.addProperty("description", description);
		HttpEntity<Object> requestEntity = new HttpEntity<>(requestJson, headers);
		Task task = restTemplate.postForObject(endPoint, requestEntity, Task.class);
		return task.getKey();
	}

	public void setEditPanelUIState(String project, String taskKey, String quotedList) throws IOException {
		String endPointRoot = serverUrl + apiRoot + "projects/" + project + "/tasks/" + taskKey + "/ui-state/";
		String endPoint = endPointRoot + "edit-panel";
		resty.json(endPoint, RestyHelper.content(quotedList, JSON_CONTENT_TYPE));
		//TODO Move to locally maintained Resty so we can easily check for HTTP200 return status
	}
	
	public void setSavedListUIState(String project, String taskKey, JSONObject items) throws IOException {
		String endPointRoot = serverUrl + apiRoot + "projects/" + project + "/tasks/" + taskKey + "/ui-state/";
		String endPoint = endPointRoot + "saved-list";
		resty.json(endPoint, RestyHelper.content(items, JSON_CONTENT_TYPE));
	}
	
	public String updateTask(String project, String taskKey, String summary, String description, String author, String reviewer) throws Exception {
		String endPoint = serverUrl + apiRoot + "projects/" + project + "/tasks/" + taskKey;
		
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
	
	public void deleteTask(String project, String taskKey, boolean optional) throws RestClientException {
		String endPoint = serverUrl + apiRoot + "projects/" + project + "/tasks/" + taskKey;
		try {
			JSONObject requestJson = new JSONObject();
			requestJson.put("status", "DELETED");
			resty.json(endPoint, Resty.put(RestyHelper.content(requestJson, JSON_CONTENT_TYPE)));
		} catch (Exception e) {
			String errStr = "Failed to delete task - " + taskKey;
			if (optional) {
				System.out.println(errStr + ": " + e.getMessage());
			} else {
				throw new RestClientException (errStr, e);
			}
		}
	}

	public Project getProject(String projectStr) throws RestClientException {
		try {
			logger.debug("Recovering project " + projectStr + " from " + serverUrl);
			String url = serverUrl + apiRoot + "projects/" + projectStr;
			Project project = restTemplate.getForObject(url, Project.class);
			if (project.getMetadata() == null || project.getMetadata().getPreviousPackage() == null) {
				throw new IllegalStateException ("Metadata not populated on project " + project.getKey());
			}
			return project;
		} catch (Exception e) {
			throw new RestClientException("Unable to recover project " + projectStr, e);
		}
	}
	
	public Task getTask(String taskKey) throws RestClientException {
		String json = "Unknown";
		try {
			String projectStr = taskKey.substring(0, taskKey.indexOf("-"));
			String endPoint = serverUrl + apiRoot + "projects/" + projectStr + "/tasks/" + taskKey;
			JSONResource response = resty.json(endPoint);
			json = response.toObject().toString();
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
			String endPoint = serverUrl + apiRoot + "projects/" + projectStr + "/tasks/" + taskKey + "/classifications";
			HttpEntity<Object> requestEntity = new HttpEntity<>("", headers);
			return restTemplate.postForObject(endPoint, requestEntity, Classification.class);
		} catch (Exception e) {
			throw new RestClientException("Unable to classify " + taskKey, e);
		}
	}
	
	public Status validate(String taskKey) throws RestClientException {
		try {
			String projectStr = taskKey.substring(0, taskKey.indexOf("-"));
			String endPoint = serverUrl + apiRoot + "projects/" + projectStr + "/tasks/" + taskKey + "/validation";
			JSONResource response = resty.json(endPoint, Resty.content(""));
			String json = response.toObject().toString();
			Status status = gson.fromJson(json, Status.class);
			return status;
		} catch (Exception e) {
			throw new RestClientException("Unable to initiate validation on " + taskKey, e);
		}
	}

	public List<Project> listProjects() {
		String endPoint = serverUrl + apiRoot + "projects";
		ParameterizedTypeReference<List<Project>> type = new ParameterizedTypeReference<>() {
        };
		logger.info("Recovering list of visible projects from {}", endPoint);
		return restTemplate.exchange(endPoint, HttpMethod.GET, null, type).getBody();
	}

}
