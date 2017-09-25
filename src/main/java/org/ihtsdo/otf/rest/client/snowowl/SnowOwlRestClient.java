package org.ihtsdo.otf.rest.client.snowowl;

import javax.servlet.http.Cookie;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.resty.HttpEntityContent;
import org.ihtsdo.otf.rest.client.resty.RestyHelper;
import org.ihtsdo.otf.rest.client.snowowl.pojo.*;
import org.ihtsdo.otf.rest.client.snowowl.pojo.MergeReviewsResults.MergeReviewStatus;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.otf.utils.DateUtils;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.BinaryResource;
import us.monoid.web.JSONResource;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SnowOwlRestClient {

	public static final String SNOWOWL_CONTENT_TYPE = "application/vnd.com.b2international.snowowl+json";
	public static final String ANY_CONTENT_TYPE = "*/*";
	public static final FastDateFormat SIMPLE_DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd_HH-mm-ss");
	public static final String US_EN_LANG_REFSET = "900000000000509007";

	public enum ExportType {
		DELTA, SNAPSHOT, FULL
	}

	public enum ProcessingStatus {
		COMPLETED, SAVED
	}

	public enum ExportCategory {
		PUBLISHED, UNPUBLISHED, FEEDBACK_FIX
	}

	private final RestyHelper resty;
	private String singleSignOnCookie;
	private RestTemplate restTemplate;

	private String reasonerId;
	private boolean flatIndexExportStyle = true;
	private String logPath;
	private String rolloverLogPath;
	private final Gson gson;
	private int importTimeoutMinutes;
	private int classificationTimeoutMinutes; //Timeout of 0 means don't time out.
	private static final int INDENT = 2;
	private static final Joiner COMMA_SEPARATED_JOINER = Joiner.on(',');
	private static final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
	private final SnowOwlRestUrlHelper urlHelper;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private SnowOwlRestClient(String snowOwlUrl) {
		this.resty = new RestyHelper(ANY_CONTENT_TYPE);
		urlHelper = new SnowOwlRestUrlHelper(snowOwlUrl);
		gson = new GsonBuilder().setPrettyPrinting().create();
		restTemplate = new RestTemplate();
	}

	public SnowOwlRestClient(String snowOwlUrl, String singleSignOnCookie) {
		this(snowOwlUrl);
		this.singleSignOnCookie = singleSignOnCookie;
		resty.withHeader("Cookie", singleSignOnCookie);
	}

	public SnowOwlRestClient(String snowOwlUrl, String apiUsername, String apiPassword) {
		this(snowOwlUrl);
		resty.authenticate(snowOwlUrl, apiUsername, apiPassword.toCharArray());
	}
	
	public SnowOwlRestClient(String snowOwlUrl, String apiUsername, String apiPassword, String userName, Set<String> userRoles) {
		this(snowOwlUrl, apiUsername, apiPassword);
		resty.withHeader("X-AUTH-username", userName);
		resty.withHeader("X-AUTH-roles", COMMA_SEPARATED_JOINER.join(userRoles));
	}

	public ConceptPojo getConcept(String branchPath, String conceptId) throws RestClientException {
		return getEntity(urlHelper.getBrowserConceptUri(branchPath, conceptId), ConceptPojo.class);
	}

	public ConceptPojo createConcept(String branchPath, ConceptPojo newConcept) throws RestClientException {
		try {
			JSONResource response = resty.json(urlHelper.getBrowserConceptsUrl(branchPath), RestyHelper.contentJSON(gson.toJson(newConcept)));
			ConceptPojo savedConcept = null;
			//Check successful creation (NB Receiving 200 rather than 201 currently)
			if (!response.getHTTPStatus().toString().startsWith("2")) {
				String msg = "<unparsable>";
				try {
					msg = response.toObject().toString(1);
				} catch (Exception e) {
					logger.error("Unable to parse response",e);
				}
				throw new IOException ("Received HTTPStatus: " + response.getHTTPStatus() + ": " + msg);
			}
			try {
				savedConcept = mapper.readValue(response.stream(), ConceptPojo.class);
			} catch (Exception e){
				logger.error("Failed to recover save result",e);
			}
			return savedConcept;
		} catch (IOException e) {
			final String message = "Failed to create concept";
			logger.error(message, e);
			throw new RestClientException(message, e);
		}
	}

	public Branch getBranch(String branchPath) throws RestClientException {
		Branch branch = getEntity(urlHelper.getBranchUri(branchPath), Branch.class);
		if (branch != null && branch.getMetadata() == null) {
			branch.setMetadata(new HashMap<String, Object>());
		}
		return branch;
	}

	public Set<String> eclQuery(String branchPath, String ecl, int limit) throws RestClientException {
		RequestEntity<Void> countRequest = createEclRequest(branchPath, ecl, limit);
		ConceptIdsResponse conceptIdsResponse = doExchange(countRequest, ConceptIdsResponse.class);
		if (conceptIdsResponse == null) {
			throw new ResourceNotFoundException("ECL query returned null result.");
		}
		return conceptIdsResponse.getConceptIds();
	}

	public boolean eclQueryHasAnyMatches(String branchPath, String ecl) throws RestClientException {
		RequestEntity<Void> countRequest = createEclRequest(branchPath, ecl, 1);
		ConceptIdsResponse conceptIdsResponse = doExchange(countRequest, ConceptIdsResponse.class);
		if (conceptIdsResponse == null) {
			throw new ResourceNotFoundException("ECL query returned null result.");
		}
		return conceptIdsResponse.getTotal() > 0;
	}

	private RequestEntity<Void> createEclRequest(final String branchPath, String ecl, int limit) {
		String authenticationToken = SecurityUtil.getAuthenticationToken();
		URI uri = UriComponentsBuilder.fromHttpUrl(urlHelper.getSimpleConceptsUrl(branchPath))
				.queryParam("ecl", ecl)
				.queryParam("active", true)
				.queryParam("offset", 0)
				.queryParam("limit", limit)
				.build().toUri();
		logger.debug("URI {}", uri);
		return RequestEntity.get(uri)
				.header("Cookie", authenticationToken)
				.build();
	}

	private <T> T getEntity(URI uri, Class<T> responseType) throws RestClientException {
		RequestEntity<Void> get = RequestEntity.get(uri)
				.header("Cookie", singleSignOnCookie)
				.build();

		return doExchange(get, responseType);
	}

	private <T, R> T doExchange(RequestEntity<R> request, Class<T> responseType) throws RestClientException {
		HttpStatus statusCode;
		ResponseEntity<T> responseEntity = null;
		try {
			responseEntity = restTemplate.exchange(request, responseType);
			statusCode = responseEntity.getStatusCode();
		} catch (HttpStatusCodeException e) {
			statusCode = e.getStatusCode();
		}

		if (statusCode.value() == 404) {
			return null;
		} else if (!statusCode.is2xxSuccessful()) {
			String errorMessage = "Failed to retrieve " + responseType.getSimpleName() + 
					", status code: " + statusCode + 
					" URI: " + request.getUrl().toString();
			throw new RestClientException(errorMessage);
		}
		return responseEntity.getBody();
	}

	private String createEntity(URI uri, Object request) throws RestClientException {
		RequestEntity<Object> post = RequestEntity.post(uri)
				.header("Cookie", singleSignOnCookie)
				.body(request);

		HttpStatus statusCode;
		ResponseEntity<String> responseEntity = null;
		try {
			responseEntity = restTemplate.exchange(post, String.class);
			statusCode = responseEntity.getStatusCode();
		} catch (HttpClientErrorException e) {
			statusCode = e.getStatusCode();
		}

		if (statusCode.value() == 404) {
			return null;
		} else if (!statusCode.is2xxSuccessful()) {
			String errorMessage = "Failed to create entity URI:" + uri.toString();
			logger.error(errorMessage + ", status code {}", statusCode);
			throw new RestClientException(errorMessage);
		}

		String location = responseEntity.getHeaders().getFirst("Location");
		if (Strings.isNullOrEmpty(location)) {
			String errorMessage = "Failed to create entity, location header missing from response. URI:" + uri.toString();
			logger.error(errorMessage + ", status code {}", statusCode);
			throw new RestClientException(errorMessage);
		}
		return location.substring(location.lastIndexOf("/") + 1);
	}

	public void createProjectBranch(String branchName) throws RestClientException {
		createBranch(urlHelper.getMainBranchPath(), branchName);
	}

	public void createProjectBranchIfNeeded(String projectName) throws RestClientException {
		if (!listProjectBranches().contains(projectName)) {
			createProjectBranch(projectName);
		}
	}

	public void createBranch(String branchPath) throws RestClientException {
		createBranch(PathHelper.getParentPath(branchPath), PathHelper.getName(branchPath));
	}

	private void createBranch(String parentBranch, String newBranchName) throws RestClientException {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("parent", parentBranch);
			jsonObject.put("name", newBranchName);
			resty.json(urlHelper.getBranchesUrl(), RestyHelper.content((jsonObject), SNOWOWL_CONTENT_TYPE));
		} catch (Exception e) {
			throw new RestClientException("Failed to create branch " + newBranchName + ", parent branch " + parentBranch, e);
		}
	}

	public List<String> listProjectBranches() throws RestClientException {
		return listBranchDirectChildren(urlHelper.getMainBranchPath());
	}

	public List<String> listProjectTasks(String projectName) throws RestClientException {
		return listBranchDirectChildren(urlHelper.getBranchPath(projectName));
	}

	private List<String> listBranchDirectChildren(String branchPath) throws RestClientException {
		String url = "";
		int status = -1;
		try {
			List<String> projectNames = new ArrayList<>();
			url = urlHelper.getBranchChildrenUrl(branchPath);
			JSONResource json = resty.json(url);
			status = json.getHTTPStatus();
			try {
				@SuppressWarnings("unchecked")
				List<String> childBranchPaths = (List<String>) json.get("items.path");
				for (String childBranchPath : childBranchPaths) {
					String branchName = childBranchPath.substring((branchPath + "/").length());
					if (!branchName.contains("/")) {
						projectNames.add(branchName);
					}
				}
			} catch (JSONException e) {
				// this thrown if there are no items.. do nothing
			}
			return projectNames;
		} catch (IOException e) {
			throw new RestClientException("Failed to retrieve branch list, status: " + status + ", from " + url, e);
		} catch (Exception e) {
			throw new RestClientException("Failed to parse branch list from, status: " + status + ", from " + url, e);
		}
	}

	public void deleteProjectBranch(String projectBranchName) throws RestClientException {
		deleteBranch(projectBranchName);
	}

	public void deleteTaskBranch(String projectName, String taskName) throws RestClientException {
		deleteBranch(projectName + "/" + taskName);
	}

	private void deleteBranch(String branchPathRelativeToMain) throws RestClientException {
		try {
			resty.json(urlHelper.getBranchUrlRelativeToMain(branchPathRelativeToMain), RestyHelper.delete());
		} catch (Exception e) {
			throw new RestClientException("Failed to delete branch " + branchPathRelativeToMain, e);
		}
	}

	public void createProjectTask(String projectName, String taskName) throws RestClientException {
		createBranch(urlHelper.getBranchPath(projectName), taskName);
	}

	public void createProjectTaskIfNeeded(String projectName, String taskName) throws RestClientException {
		if (!listProjectTasks(projectName).contains(taskName)) {
			createProjectTask(projectName, taskName);
		}
	}

	public String startMerge(String sourceBranchPath, String targetBranchPath) throws RestClientException {
		return startMerge(sourceBranchPath, targetBranchPath, null);
	}

	public String startMerge(String sourceBranchPath, String targetBranchPath, String mergeReviewId) throws RestClientException {
		Map<String, String> request = new HashMap<>();
		request.put("source", sourceBranchPath);
		request.put("target", targetBranchPath);
		if (!Strings.isNullOrEmpty(mergeReviewId)) {
			request.put("reviewId", mergeReviewId);
		}
		request.put("commitComment", "" + SecurityUtil.getUsername() + " performed merge of " + sourceBranchPath + " to " + targetBranchPath);
		return createEntity(urlHelper.getMergesUri(), request);
	}

	public Merge getMerge(String mergeId) throws RestClientException {
		return getEntity(urlHelper.getMergeUri(mergeId), Merge.class);
	}
	
	public MergeReviewsResults getMergeReviewsResult(String mergeId) throws RestClientException {
		return getEntity(urlHelper.getMergeReviewsUri(mergeId), MergeReviewsResults.class);
	}
	
	public boolean isNoMergeConflict(String mergeId) throws RestClientException{
		return getEntity(urlHelper.getMergeReviewsDetailsUri(mergeId), Set.class).isEmpty();
	}

	public boolean importRF2Archive(String projectName, String taskName, final InputStream rf2ZipFileStream)
			throws RestClientException {
		Assert.notNull(rf2ZipFileStream, "Archive to import should not be null.");

		try {
			// Create import
			String branchPath = urlHelper.getBranchPath(projectName, taskName);
			logger.info("Create import, branch '{}'", branchPath);

			JSONObject params = new JSONObject();
			params.put("type", "DELTA");
			params.put("branchPath", branchPath);
			params.put("languageRefSetId", US_EN_LANG_REFSET);
			params.put("createVersions", "false");
			resty.withHeader("Accept", SNOWOWL_CONTENT_TYPE);
			JSONResource json = resty.json(urlHelper.getImportsUrl(), RestyHelper.content(params, SNOWOWL_CONTENT_TYPE));
			String location = json.getUrlConnection().getHeaderField("Location");
			String importId = location.substring(location.lastIndexOf("/") + 1);

			// Create file from stream
			File tempDirectory = Files.createTempDirectory(getClass().getSimpleName()).toFile();
			File tempFile = new File(tempDirectory, "SnomedCT_Release_INT_20150101.zip");
			try {
				try (FileOutputStream output = new FileOutputStream(tempFile)) {
					IOUtils.copy(rf2ZipFileStream, output);
				}

				// Post file to TS
				MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
				multipartEntityBuilder.addBinaryBody("file", tempFile, ContentType.create("application/zip"), tempFile.getName());
				multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
				HttpEntity httpEntity = multipartEntityBuilder.build();
				resty.withHeader("Accept", ANY_CONTENT_TYPE);
				resty.json(urlHelper.getImportArchiveUrl(importId), new HttpEntityContent(httpEntity));

			} finally {
				tempFile.delete();
				tempDirectory.delete();
			}

			// Poll import entity until complete or times-out
			logger.info("SnowOwl processing import, this will probably take a few minutes. (Import ID '{}')", importId);
			return waitForStatus(urlHelper.getImportUrl(importId), getTimeoutDate(importTimeoutMinutes), ProcessingStatus.COMPLETED,
					"import");
		} catch (Exception e) {
			throw new RestClientException("Import failed.", e);
		}
	}
	
	public String createBranchMergeReviews(String sourceBranchPath, String targetBranchPath) throws RestClientException{
		Map<String, String> request = new HashMap<>();
		request.put("source", sourceBranchPath);
		request.put("target", targetBranchPath);
		return createEntity(urlHelper.getMergeReviewsUri(), request);
	}
	
	public ClassificationResults startClassification (String branchPath) throws RestClientException {
		ClassificationResults results = new ClassificationResults();
		try {
			JSONObject requestJson = new JSONObject().put("reasonerId", reasonerId);
			String classifyURL = urlHelper.getClassificationsUrl(branchPath);
			logger.info("Initiating classification via {}", classifyURL);
			JSONResource jsonResponse = resty.json(classifyURL, requestJson, SNOWOWL_CONTENT_TYPE);
			String classificationLocation = jsonResponse.getUrlConnection().getHeaderField("Location");
			if (classificationLocation == null) {
				String errorMsg = "Failed to recover classificationLocation.  Call to " 
						+ classifyURL + " returned httpStatus '"
						+ jsonResponse.getHTTPStatus() + "'";
				try {
					errorMsg += " and body '" + jsonResponse.toObject().toString(INDENT) + "'.";
				} catch (Exception e) {
					errorMsg += ". Also failed to parse response object.";
				}
				throw new RestClientException (errorMsg);
			}
			results.setClassificationId(classificationLocation.substring(classificationLocation.lastIndexOf("/") + 1));
			results.setClassificationLocation(classificationLocation);
		} catch (JSONException | IOException e) {
			throw new RestClientException("Create classification failed.", e);
		}
		return results;
	}

	/**
	 * Initiates a classification and waits for the results.
	 * {@link #isClassificationInProgressOnBranch(String)} can be used to check that a classification is not already in progress
	 * prior to calling this method.
	 * @param branchPath
	 * @return
	 * @throws RestClientException
	 * @throws InterruptedException
	 */
	public ClassificationResults classify(String branchPath) throws RestClientException, InterruptedException {

		ClassificationResults results = startClassification(branchPath);
		results = waitForClassificationToComplete(results);
		return results;
	}
	
	public ClassificationResults waitForClassificationToComplete(ClassificationResults results) throws RestClientException, InterruptedException {
		String classificationLocation = results.getClassificationLocation();
		String date = SIMPLE_DATE_FORMAT.format(new Date());
		logger.info("SnowOwl classifier running, this will probably take a few minutes. (Classification URL '{}')", classificationLocation);
		boolean classifierCompleted = waitForStatus(classificationLocation, getTimeoutDate(classificationTimeoutMinutes), ProcessingStatus.COMPLETED, "classifier");
		if (classifierCompleted) {
			results.setStatus(ProcessingStatus.COMPLETED.toString());
			try {
				// Check equivalent concepts
				JSONArray items = getItems(urlHelper.getEquivalentConceptsUrl(classificationLocation));
				boolean equivalentConceptsFound = !(items == null || items.length() == 0);
				results.setEquivalentConceptsFound(equivalentConceptsFound);
				if (equivalentConceptsFound) {
					results.setEquivalentConceptsJson(toPrettyJson(items.toString()));
				}
			} catch (Exception e) {
				throw new RestClientException("Failed to retrieve equivalent concepts of classification.", e);
			}
			try {
				// Check relationship changes
				JSONResource relationshipChangesUnlimited = resty.json(urlHelper.getRelationshipChangesFirstTenThousand(classificationLocation));
				Integer total = (Integer) relationshipChangesUnlimited.get("total");
				results.setRelationshipChangesCount(total);
				Path tempDirectory = Files.createTempDirectory(getClass().getSimpleName());
				File file = new File(tempDirectory.toFile(), "relationship-changes-" + date + ".json");
				toPrettyJson(relationshipChangesUnlimited.object().toString(), file);
				results.setRelationshipChangesFile(file);
			} catch (Exception e) {
				throw new RestClientException("Failed to retrieve relationship changes of classification.", e);
			}
			return results;
		} else {
			throw new RestClientException("Classification failed, see SnowOwl logs for details.");
		}
	}

	public String getLatestClassificationOnBranch(String branchPath) throws RestClientException {
		JSONObject obj = getLatestClassificationObjectOnBranch(branchPath);
		return (obj != null) ? obj.toString() : null;
	}

	public boolean isClassificationInProgressOnBranch(String branchPath) throws RestClientException, JSONException {
		final JSONObject classification = getLatestClassificationObjectOnBranch(branchPath);
		if (classification != null) {
			final String status = classification.getString("status");
			return "SCHEDULED".equals(status) || "RUNNING".equals(status);
		}
		return false;
	}

	private JSONObject getLatestClassificationObjectOnBranch(String branchPath) throws RestClientException {
		final String classificationsUrl = urlHelper.getClassificationsUrl(branchPath);
		try {
			final JSONArray items = getItems(classificationsUrl);
			if (items != null && items.length() > 0) {
				return items.getJSONObject(items.length() - 1);
			}
			return null;
		} catch (Exception e) {
			throw new RestClientException("Failed to retrieve list of classifications.", e);
		}
	}

	public void saveClassification(String branchPath, String classificationId) throws RestClientException,
			InterruptedException {
		String classifyURL = urlHelper.getClassificationsUrl(branchPath);
		try {
			logger.debug("Saving classification via {}", classifyURL);
			JSONObject jsonObj = new JSONObject().put("status", "SAVED");
			resty.put(classifyURL, jsonObj, SNOWOWL_CONTENT_TYPE);
			//We'll wait the same time for saving as we do for the classification
			boolean savingCompleted = waitForStatus(classifyURL, getTimeoutDate(classificationTimeoutMinutes),
					ProcessingStatus.SAVED, "classifier result saving");
			if (!savingCompleted) {
				throw new IOException("Classifier reported non-saved status when saving");
			}
		} catch (Exception e) {
			throw new RestClientException("Failed to save classification via URL " + classifyURL, e);
		}
	}

	private JSONArray getItems(String url) throws Exception {
		JSONResource jsonResource = resty.json(url);
		JSONArray items = null;
		try {
			items = (JSONArray) jsonResource.get("items");
		} catch (Exception e) {
			// TODO Change this back to JSONException when Resty handles that.
			// this gets thrown when the attribute does not exist
			logger.debug("No items property of resource at '{}'", url);
		}
		return items;
	}

	
	public File exportTask(String projectName, String taskName, ExportType exportType) throws Exception {
		String branchPath = urlHelper.getBranchPath(projectName, taskName);
		return export(branchPath, null, null, ExportCategory.UNPUBLISHED, exportType);
	}

	public File exportProject(String projectName, ExportType exportType) throws Exception {
		String branchPath = urlHelper.getBranchPath(projectName, null);
		return export(branchPath, null, null, ExportCategory.UNPUBLISHED, exportType);
	}

	public File export(ExportConfigurationBuilder exportConfigurationBuilder)
			throws BusinessServiceException {
		String exportConfigString = null;
		try {
			exportConfigString = mapper.writeValueAsString(exportConfigurationBuilder);
		} catch (JsonProcessingException e) {
			throw new BusinessServiceException("Failed to create export configuration.", e);
		}
		String exportUrl = initiateExport(exportConfigString);
		return recoverExportedArchive(exportUrl);
	}

	public File export(String branchPath, String effectiveDate, Set<String> moduleIds, ExportCategory exportCategory, ExportType exportType)
			throws BusinessServiceException {

		JSONObject jsonObj = prepareExportJSON(branchPath, effectiveDate, moduleIds, exportCategory, exportType);

		String exportLocationURL = initiateExport(jsonObj.toString());

		return recoverExportedArchive(exportLocationURL);
	}
	
	private JSONObject prepareExportJSON(String branchPath, String effectiveDate, Set<String> moduleIds, ExportCategory exportCategory, ExportType exportType)
			throws BusinessServiceException {
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("type", exportType);
			jsonObj.put("branchPath", branchPath);
			if (moduleIds != null) {
				jsonObj.put("moduleIds", moduleIds);
			}
			switch (exportCategory) {
				case UNPUBLISHED:
					String tet = (effectiveDate == null) ? DateUtils.now(DateUtils.YYYYMMDD) : effectiveDate;
					jsonObj.put("transientEffectiveTime", tet);
					if (flatIndexExportStyle) {
						jsonObj.put("type", ExportType.DELTA);
					}
					break;
				case PUBLISHED:
					if (effectiveDate == null) {
						throw new ProcessingException("Cannot export published data without an effective date");
					}
					if (flatIndexExportStyle) {
						jsonObj.put("startEffectiveTime", effectiveDate);

					} else {
						jsonObj.put("deltaStartEffectiveTime", effectiveDate);
						jsonObj.put("deltaEndEffectiveTime", effectiveDate);
						jsonObj.put("transientEffectiveTime", effectiveDate);
					}
					break;
				case FEEDBACK_FIX:
					if (effectiveDate == null) {
						throw new ProcessingException("Cannot export feedback-fix data without an effective date");
					}
					if (flatIndexExportStyle) {
						jsonObj.put("startEffectiveTime", effectiveDate);
						jsonObj.put("includeUnpublished", true);

					} else {
						jsonObj.put("deltaStartEffectiveTime", effectiveDate);
					}
					jsonObj.put("transientEffectiveTime", effectiveDate);
					break;
				default:
					throw new BadRequestException("Export type " + exportCategory + " not recognised");
			}
		} catch (JSONException e) {
			throw new ProcessingException("Failed to prepare JSON for export request.", e);
		}
		return jsonObj;
	}

	private String initiateExport(String exportJsonString) throws BusinessServiceException {
		try {
			logger.info("Initiating export via url {} with json: {}", urlHelper.getExportsUrl(), exportJsonString);
			JSONResource jsonResponse = resty.json(urlHelper.getExportsUrl(), RestyHelper.contentJSON(exportJsonString));
			Object exportLocationURLObj = jsonResponse.getUrlConnection().getHeaderField("Location");
			if (exportLocationURLObj == null) {
				throw new ProcessingException("Failed to obtain location of export, instead got status '" + jsonResponse.getHTTPStatus()
						+ "' and body: " + jsonResponse.toObject().toString(INDENT));
			}
			return exportLocationURLObj.toString() + "/archive";
		} catch (Exception e) {
			// TODO Change this to catch JSONException once Resty no longer throws Exceptions
			throw new ProcessingException("Failed to initiate export", e);
		}
	}

	private File recoverExportedArchive(String exportLocationURL) throws BusinessServiceException {
		try {
			logger.debug("Recovering exported archive from {}", exportLocationURL);
			resty.withHeader("Accept", ANY_CONTENT_TYPE);
			BinaryResource archiveResource = resty.bytes(exportLocationURL);
			File archive = File.createTempFile("ts-extract", ".zip");
			archiveResource.save(archive);
			logger.debug("Extract saved to {}", archive.getAbsolutePath());
			return archive;
		} catch (IOException e) {
			throw new BusinessServiceException("Unable to recover exported archive from " + exportLocationURL, e);
		}
	}

	/**
	 * Task rebase should be performed via the authoring-services API
	 */
	@Deprecated()
	public void rebaseTask(String projectName, String taskName) throws RestClientException {
		String taskPath = urlHelper.getBranchPath(projectName, taskName);
		String projectPath = urlHelper.getBranchPath(projectName);
		logger.info("Rebasing branch {} from parent {}", taskPath, projectPath);
		doMerge(projectPath, taskPath);
	}

	/**
	 * Task promotion should be performed via the authoring-services API
	 */
	@Deprecated()
	public void mergeTaskToProject(String projectName, String taskName) throws RestClientException {
		String taskPath = urlHelper.getBranchPath(projectName, taskName);
		String projectPath = urlHelper.getBranchPath(projectName);
		logger.info("Promoting branch {} to {}", taskPath, projectPath);
		doMerge(taskPath, projectPath);
	}

	private void doMerge(String sourcePath, String targetPath) throws RestClientException {
		try {
			JSONObject params = new JSONObject();
			params.put("source", sourcePath);
			params.put("target", targetPath);
			resty.put(urlHelper.getMergesUrl(), params, SNOWOWL_CONTENT_TYPE);
		} catch (Exception e) {
			throw new RestClientException("Failed to merge " + sourcePath + " to " + targetPath, e);
		}
	}

	/**
	 * Warning - this only works when the SnowOwl log is on the same machine.
	 */
	public InputStream getLogStream() throws FileNotFoundException {
		return new FileInputStream(logPath);
	}

	/**
	 * Returns stream from rollover log or null.
	 * @return
	 * @throws FileNotFoundException
	 */
	public InputStream getRolloverLogStream() throws FileNotFoundException {
		if (new File(rolloverLogPath).isFile()) {
			return new FileInputStream(rolloverLogPath);
		} else {
			return null;
		}
	}

	private boolean waitForStatus(String url, Date timeoutDate, ProcessingStatus targetStatus, final String waitingFor)
			throws RestClientException, InterruptedException {
		String status = "";
		boolean finalStateAchieved = false;
		while (!finalStateAchieved) {
			try {
				Object statusObj = resty.json(url).get("status");
				status = statusObj.toString() ;
			} catch (Exception e) {
				throw new RestClientException("Rest client error while checking status of " + waitingFor + ".", e);
			}
			finalStateAchieved = !("RUNNING".equals(status) || "SCHEDULED".equals(status) || "SAVING_IN_PROGRESS".equals(status));
			if (!finalStateAchieved && timeoutDate != null && new Date().after(timeoutDate)) {
				throw new RestClientException("Client timeout waiting for " + waitingFor + ".");
			}
			
			if (!finalStateAchieved) {
				Thread.sleep(1000 * 10);
			}
		}

		boolean targetStatusAchieved = targetStatus.toString().equals(status);
		if (!targetStatusAchieved) {
			logger.warn("TS reported non-complete status {} from URL {}", status, url);
		}
		return targetStatusAchieved;
	}

	private Date getTimeoutDate(int importTimeoutMinutes) {
		if (importTimeoutMinutes > 0) {
			GregorianCalendar timeoutCalendar = new GregorianCalendar();
			timeoutCalendar.add(Calendar.MINUTE, importTimeoutMinutes);
			return timeoutCalendar.getTime();
		} 

		return null;
	}

	private String toPrettyJson(String jsonString) {
		JsonElement el = new JsonParser().parse(jsonString);
		return gson.toJson(el);
	}

	private void toPrettyJson(String jsonString, File outFile) throws IOException {
		JsonElement el = new JsonParser().parse(jsonString);
		try (JsonWriter writer = new JsonWriter(new FileWriter(outFile))) {
			gson.toJson(el, writer);
		}
	}

	public void setReasonerId(String reasonerId) {
		this.reasonerId = reasonerId;
	}

	public String getReasonerId() {
		return reasonerId;
	}

	public void setLogPath(String logPath) {
		this.logPath = logPath;
	}

	public void setRolloverLogPath(String rolloverLogPath) {
		this.rolloverLogPath = rolloverLogPath;
	}

	@Required
	public void setImportTimeoutMinutes(int importTimeoutMinutes) {
		this.importTimeoutMinutes = importTimeoutMinutes;
	}

	@Required
	public void setClassificationTimeoutMinutes(int classificationTimeoutMinutes) {
		this.classificationTimeoutMinutes = classificationTimeoutMinutes;
	}

	public void setFlatIndexExportStyle(boolean flatIndexExportStyle) {
		this.flatIndexExportStyle = flatIndexExportStyle;
	}

	public static class ExportConfigurationBuilder {

		private String branchPath = "MAIN";
		private Set<String> moduleIds = new HashSet<>();
		private String startEffectiveTime;
		private String endEffectiveTime;
		private String namespaceId;
		private String transientEffectiveTime;
		private boolean includeUnpublished = true;
		private ExportType type = ExportType.DELTA;

		public String getBranchPath() {
			return branchPath;
		}

		public ExportConfigurationBuilder setBranchPath(String branchPath) {
			this.branchPath = branchPath;
			return this;
		}

		public Set<String> getModuleIds() {
			return moduleIds;
		}

		public ExportConfigurationBuilder addModuleIds(Set<String> moduleIds) {
			this.moduleIds.addAll(moduleIds);
			return this;
		}

		public ExportConfigurationBuilder addModuleId(String moduleId) {
			this.moduleIds.add(moduleId);
			return this;
		}

		public String getStartEffectiveTime() {
			return startEffectiveTime;
		}

		public ExportConfigurationBuilder setStartEffectiveTime(String startEffectiveTime) {
			this.startEffectiveTime = startEffectiveTime;
			return this;
		}

		public String getEndEffectiveTime() {
			return endEffectiveTime;
		}

		public ExportConfigurationBuilder setEndEffectiveTime(String endEffectiveTime) {
			this.endEffectiveTime = endEffectiveTime;
			return this;
		}

		public String getNamespaceId() {
			return namespaceId;
		}

		public ExportConfigurationBuilder setNamespaceId(String namespaceId) {
			this.namespaceId = namespaceId;
			return this;
		}

		public String getTransientEffectiveTime() {
			return transientEffectiveTime;
		}

		public ExportConfigurationBuilder setTransientEffectiveTime(String transientEffectiveTime) {
			this.transientEffectiveTime = transientEffectiveTime;
			return this;
		}

		public boolean isIncludeUnpublished() {
			return includeUnpublished;
		}

		public ExportConfigurationBuilder setIncludeUnpublished(boolean includeUnpublished) {
			this.includeUnpublished = includeUnpublished;
			return this;
		}

		public ExportType getType() {
			return type;
		}

		public ExportConfigurationBuilder setType(ExportType type) {
			this.type = type;
			return this;
		}
	}
}
