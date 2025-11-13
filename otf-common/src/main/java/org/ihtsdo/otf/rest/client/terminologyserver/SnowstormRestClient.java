package org.ihtsdo.otf.rest.client.terminologyserver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.*;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.otf.utils.DateUtils;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.JSONResource;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;

import static org.springframework.util.Assert.notNull;

// TODO: This whole class is a mess and needs refactoring.
public class SnowstormRestClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(SnowstormRestClient.class);

	private static final String COOKIE = "Cookie";
	public static final String JSON_CONTENT_TYPE = "application/json";
	public static final String ANY_CONTENT_TYPE = "*/*";
	public static final FastDateFormat SIMPLE_DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd_HH-mm-ss");
	public static final String US_EN_LANG_REFSET = "900000000000509007";
	public static final String LOCK = "lock";

	public enum ExportType {
		DELTA, SNAPSHOT, FULL
	}

	public enum ProcessingStatus {
		COMPLETED, SAVED
	}

	public enum ExportCategory {
		PUBLISHED, UNPUBLISHED, FEEDBACK_FIX
	}

	private String singleSignOnCookie;
	private RestTemplate restTemplate;
	private final RestyHelper resty;

	private String reasonerId = "org.semanticweb.elk.owlapi.ElkReasonerFactory";
	private boolean useExternalClassificationService = true;

	private boolean flatIndexExportStyle = true;
	private final Gson gson;
	private int importTimeoutMinutes;
	private int classificationTimeoutMinutes; //Timeout of 0 means don't time out.
	private final SnowstormRestUrlHelper urlHelper;

	private static final int BATCH_SIZE = 200;
	private static final int MAX_PAGE_SIZE = 10_000;
	private static final int INDENT = 2;
	private static final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
	private static final ParameterizedTypeReference<ItemsPage<CodeSystem>> CODESYSTEM_PAGE_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
    };
	private static final ParameterizedTypeReference<ItemsPage<CodeSystemVersion>> CODESYSTEM_VERSION_PAGE_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
    };
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private SnowstormRestClient(String snowstormUrl) {
		urlHelper = new SnowstormRestUrlHelper(snowstormUrl);
		gson = new GsonBuilder().setPrettyPrinting().create();
		restTemplate = new RestTemplate();
		this.resty = new RestyHelper(ANY_CONTENT_TYPE);
	}

	public SnowstormRestClient(String snowstormUrl, String singleSignOnCookie) {
		this(snowstormUrl);
		this.singleSignOnCookie = " " + singleSignOnCookie;
		resty.withHeader(HttpHeaders.COOKIE, singleSignOnCookie);
		restTemplate.setInterceptors(Collections.singletonList((request, body, execution) -> {
			// Set cookie
			request.getHeaders().set(HttpHeaders.COOKIE, singleSignOnCookie);

			// Log request
			LOGGER.debug("===========================request begin================================================");
			LOGGER.debug("URI         : {}", request.getURI());
			LOGGER.debug("Method      : {}", request.getMethod());
			HttpHeaders headers = request.getHeaders();
			headers.forEach((k, v) -> {
				LOGGER.debug("Header      : {}", k);
				LOGGER.debug("Value       : {}", HttpHeaders.COOKIE.equals(k) ? mask(v.toString()) : v);
			});
			LOGGER.debug("Request body: {}", new String(body, "UTF-8"));
			LOGGER.debug("==========================request end================================================");

			ClientHttpResponse response = execution.execute(request, body);

			// Log response
			LOGGER.debug("============================response begin==========================================");
			LOGGER.debug("Status code  : {}", response.getStatusCode());
			LOGGER.debug("Status text  : {}", response.getStatusText());
			LOGGER.debug("Headers      : {}", response.getHeaders());
			headers = response.getHeaders();
			headers.forEach((k, v) -> {
				LOGGER.debug("Header      : {}", k);
				LOGGER.debug("Value       : {}", v);
			});
			LOGGER.debug("Response body: {}", Arrays.toString(body));
			LOGGER.debug("=======================response end=================================================");

			return response;
		}));
	}

	private String mask(String token) {
		if (token == null) {
			return null;
		}
		int start = 1;
		if (token.contains("=")) {
			start = token.indexOf("=");
		}
		char[] maskedToken = new char[token.length()];
		for (int i = 0; i < maskedToken.length; i++) {
			maskedToken[i] = '*';
		}
		for (int j = 0; j < start; j++) {
			maskedToken[j] = token.charAt(j);
		}
		maskedToken[maskedToken.length - 1] = token.charAt(token.length() - 1);
		return new String(maskedToken);
	}

	public SnowstormRestClient(String snowstormUrl, String apiUsername, String apiPassword) {
		this(snowstormUrl);
	}

	public SnowstormRestClient(String snowstormUrl, String apiUsername, String apiPassword, String userName, Set<String> userRoles) {
		this(snowstormUrl, apiUsername, apiPassword);
	}

	public CodeSystem getCodeSystem(String codeSystemShortname) throws RestClientException {
		return getEntity(urlHelper.getCodeSystemUrl(codeSystemShortname), CodeSystem.class);
	}

	public List<CodeSystem> getCodeSystems() {
		ResponseEntity<ItemsPage<CodeSystem>> responseEntity = restTemplate.exchange(urlHelper.getCodeSystemsUrl(), HttpMethod.GET, new org.springframework.http.HttpEntity<>(null), CODESYSTEM_PAGE_TYPE_REFERENCE);
		ItemsPage<CodeSystem> page = responseEntity.getBody();
		return page.getItems();
	}

	public List<CodeSystemVersion> getCodeSystemVersions(String shortName, Boolean showFutureVersions, Boolean showInternalReleases) {
		if (showFutureVersions == null) {
			showFutureVersions = false;
		}
		if (showInternalReleases == null) {
			showInternalReleases = false;
		}
		ResponseEntity<ItemsPage<CodeSystemVersion>> responseEntity = restTemplate.exchange(urlHelper.getCodeSystemVersionsUri(shortName, showFutureVersions, showInternalReleases), HttpMethod.GET, new org.springframework.http.HttpEntity<>(null), CODESYSTEM_VERSION_PAGE_TYPE_REFERENCE);
		ItemsPage<CodeSystemVersion> page = responseEntity.getBody();
		return page.getItems();
	}

	public void  updateCodeSystemVersionPackage(String codeSystemShortname, String effectiveDate, String releasePackage) throws RestClientException {
		UriComponentsBuilder queryBuilder = UriComponentsBuilder.fromUriString(urlHelper.getUpdateCodeSystemVersionPackageUri(codeSystemShortname, effectiveDate).toString())
				.queryParam("releasePackage", releasePackage);
		URI uri = queryBuilder.build().toUri();
		try {
			restTemplate.exchange(uri, HttpMethod.PUT, new org.springframework.http.HttpEntity<>(null), Void.class);
		} catch (HttpClientErrorException | HttpServerErrorException e) {
			int statusCode = e.getStatusCode().value();
			if (statusCode == 404 && StringUtils.hasLength(e.getResponseBodyAsString())) {
				try {
					JSONObject jsonObject = new JSONObject(e.getResponseBodyAsString());
					if (jsonObject.has("message")) {
						throw new RestClientException(jsonObject.getString("message"));
					}
				}catch (JSONException err){
					// do nothing
				}
			}
			throw  e;
		}
	}

	/**
	 * Note: This APi uses browser endpoint  and returns the full information about the concept 
	 * However it is very slow.
	 * 
	 */
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
			branch.setMetadata(new HashMap<>());
		}
		return branch;
	}

	public MembersResponse getMembers(String branchPath, String referenceSet, int limit) throws RestClientException {
		RequestEntity<Void> countRequest = createRefsetRequest(branchPath, referenceSet, limit);
		MembersResponse entries = doExchange(countRequest, MembersResponse.class);
		if (entries == null) {
			throw new ResourceNotFoundException("No refset entries found.");
		}
		return entries;
	}
	
	public String getFsn(String branchPath, String conceptId) throws RestClientException {
		return getFsns(branchPath, Collections.singletonList(conceptId)).get(conceptId);
	}
	
	public Map<String, Set<SimpleDescriptionPojo>> getDescriptions(String branchPath, Collection<String> conceptIds) throws RestClientException {
		Map<String, Set<SimpleDescriptionPojo>> result = new HashMap<>();
		List<String> batchJob = null;
		int counter=0;
		for (String conceptId : conceptIds) {
			if (batchJob == null) {
				batchJob = new ArrayList<>();
			}
			batchJob.add(conceptId);
			counter++;
			if (counter % BATCH_SIZE == 0 || counter == conceptIds.size()) {
				RequestEntity<Void> countRequest = createDescriptionsByConceptsSearchRequest(branchPath, batchJob, batchJob.size());
				SimpleConceptResponse simpleConceptResp = doExchange(countRequest, SimpleConceptResponse.class);
				if (simpleConceptResp == null || simpleConceptResp.getItems().isEmpty()) {
					throw new ResourceNotFoundException("Can't find concepts from branch " + branchPath);
				}
				for (SimpleConceptPojo pojo : simpleConceptResp.getItems()) {
					result.put(pojo.getId(), pojo.getDescriptions().getItems());
				}
				batchJob = null;
			}
		}
		return result;
	}
	
	private RequestEntity<Void> createDescriptionsByConceptsSearchRequest(String branchPath, Collection<String> conceptIds, int limit) {
		if (conceptIds == null || conceptIds.isEmpty()) {
			throw new IllegalArgumentException("Concept ids must be specified");
		}
		String authenticationToken = singleSignOnCookie != null ?
				singleSignOnCookie : SecurityUtil.getAuthenticationToken();
		UriComponentsBuilder queryBuilder = UriComponentsBuilder.fromUriString
				(urlHelper.getSimpleConceptsUrl(branchPath))
				.queryParam("active", true)
				.queryParam("offset", 0)
				.queryParam("expand", "descriptions()")
				.queryParam("termActive", true)
				.queryParam("limit", limit)
				.queryParam("conceptIds", conceptIds.toArray());
		URI uri = queryBuilder.build().toUri();
		logger.debug("URI {}", uri);
		return RequestEntity.get(uri)
				.header(COOKIE, authenticationToken)
				.build();
	}

	public Map<String, String> getFsns(String branchPath, Collection<String> conceptIds) throws RestClientException {
		RequestEntity<Void> countRequest = createConceptsRequest(branchPath,conceptIds, conceptIds.size());
		SimpleConceptResponse simpleConceptResp = doExchange(countRequest, SimpleConceptResponse.class);
		if (simpleConceptResp == null || simpleConceptResp.getItems().isEmpty()) {
			throw new ResourceNotFoundException("Can't find concepts from branch:" + branchPath);
		}
		Map<String, String> result = new HashMap<>();
		for (SimpleConceptPojo pojo : simpleConceptResp.getItems()) {
			result.put(pojo.getId(), pojo.getFsn().getTerm());
		}
		return result;
	}
	
	private RequestEntity<Void> createConceptsRequest(String branchPath, Collection<String> conceptIds, int size) {
		return createConceptsRequest(branchPath, null, null, conceptIds, size, false);
	}

	
	public Set<ConceptMiniPojo> getConceptMinis(String branchPath, List<String> concepts, int limit) throws RestClientException {
		
		RequestEntity<Void> countRequest = createConceptsRequest(branchPath, null, null, concepts, limit, true);
		ConceptMiniResponse conceptMiniResp = doExchange(countRequest, ConceptMiniResponse.class);
		if (conceptMiniResp == null) {
			throw new ResourceNotFoundException("Concepts query returned null result on branch " + branchPath);
		}
		return conceptMiniResp.getItems();
	}
	
	public Set<SimpleConceptPojo> getConcepts(String branchPath, String ecl,
			String termPrefix, List<String> concepts, int limit) throws RestClientException {
		
		return getConcepts(branchPath, ecl, termPrefix, concepts, limit, false);
	}
	
	public Set<SimpleConceptPojo> getConcepts(String branchPath, String ecl,
			String termPrefix, List<String> concepts, int limit, boolean stated) throws RestClientException {
		
		RequestEntity<Void> countRequest = createConceptsRequest(branchPath, ecl, termPrefix, concepts, limit, stated);
		SimpleConceptResponse simpleConceptResp = doExchange(countRequest, SimpleConceptResponse.class);
		if (simpleConceptResp == null) {
			throw new ResourceNotFoundException("ECL query returned null result.");
		}
		return simpleConceptResp.getItems();
	}

	public Set<String> eclQuery(String branchPath, String ecl, int limit) throws RestClientException {
		return eclQuery(branchPath, ecl, limit, false);
	}
	
	
	public Set<String> eclQuery(String branchPath, String ecl, int totalLimit, boolean stated) throws RestClientException {
		if (totalLimit > MAX_PAGE_SIZE) {
			Set<String> all = new HashSet<>();
			int pageOffset = 0;
			int pageLimit = MAX_PAGE_SIZE;
			boolean complete = false;
			while (!complete) {
				Set<String> pageResults = doEclQueryWithoutPaging(branchPath, ecl, pageOffset, pageLimit, stated);
				if (!pageResults.isEmpty()) {
					all.addAll(pageResults);
				}
				if (pageResults.size() == MAX_PAGE_SIZE) {
					pageOffset += MAX_PAGE_SIZE;
					if ((pageOffset + pageLimit) > totalLimit) {
						pageLimit = totalLimit - pageOffset;
					}
				} else {
					complete = true;
				}
			}
			return all;
		} else {
			return doEclQueryWithoutPaging(branchPath, ecl, 0, totalLimit, stated);
		}
	}

	private Set<String> doEclQueryWithoutPaging(String branchPath, String ecl, int offset, int limit, boolean stated) throws RestClientException {
		RequestEntity<Void> countRequest = createEclRequest(branchPath, ecl, offset, limit, stated);
		ConceptIdsResponse conceptIdsResponse = doExchange(countRequest, ConceptIdsResponse.class);
		if (conceptIdsResponse == null) {
			throw new ResourceNotFoundException("ECL query returned null result.");
		}
		return conceptIdsResponse.getConceptIds();
	}

	public boolean eclQueryHasAnyMatches(String branchPath, String ecl) throws RestClientException {
		return eclQueryHasAnyMatches(branchPath, ecl, false);
	}
	
	public boolean eclQueryHasAnyMatches(String branchPath, String ecl, boolean stated) throws RestClientException {
		RequestEntity<Void> countRequest = createEclRequest(branchPath, ecl, 0, 1, stated);
		ConceptIdsResponse conceptIdsResponse = doExchange(countRequest, ConceptIdsResponse.class);
		if (conceptIdsResponse == null) {
			throw new ResourceNotFoundException("ECL query returned null result.");
		}
		return conceptIdsResponse.getTotal() > 0;
	}
	
	
	private RequestEntity<Void> createConceptsRequest(String branchPath, String ecl,
			String termPrefix, Collection<String> concepts, int limit, boolean stated) {
		String authenticationToken = singleSignOnCookie != null ?
				singleSignOnCookie : SecurityUtil.getAuthenticationToken();
		UriComponentsBuilder queryBuilder = UriComponentsBuilder.fromUriString
				(urlHelper.getSimpleConceptsUrl(branchPath))
				.queryParam("active", true)
				.queryParam("offset", 0)
				.queryParam("expand", "fsn()")
				.queryParam("termActive", true)
				.queryParam("limit", limit);
		
		if (ecl != null) {
			if (stated) {
				queryBuilder.queryParam("statedEcl", ecl);
			} else {
				queryBuilder.queryParam("ecl", ecl);
			}
		}
		
		if (termPrefix != null && !termPrefix.isEmpty()) {
			queryBuilder.queryParam("term", termPrefix);
		}
		
		if (concepts != null && !concepts.isEmpty()) {
			queryBuilder.queryParam("conceptIds", concepts.toArray());
		}
		URI uri = queryBuilder.build().toUri();
		logger.debug("URI {}", uri);
		return RequestEntity.get(uri)
				.header(COOKIE, authenticationToken)
				.build();
	}

	private RequestEntity<Void> createEclRequest(final String branchPath, String ecl, int offset, int limit, boolean stated) {
		String authenticationToken = singleSignOnCookie != null ? singleSignOnCookie : SecurityUtil.getAuthenticationToken();
		UriComponentsBuilder queryBuilder = UriComponentsBuilder.fromUriString(urlHelper.getSimpleConceptsUrl(branchPath))
				.queryParam("active", true)
				.queryParam("offset", offset)
				.queryParam("limit", limit);
		if (stated) {
			queryBuilder.queryParam("statedEcl", ecl);
		} else {
			queryBuilder.queryParam("ecl", ecl);
		}
		
		URI uri = queryBuilder.build().encode().toUri();
		logger.debug("URI {}", uri);
		return RequestEntity.get(uri)
				.header(COOKIE, authenticationToken)
				.build();
	}

	private RequestEntity<Void> createRefsetRequest(final String branchPath, String refset, int limit) {
		String authenticationToken = singleSignOnCookie != null ? singleSignOnCookie : SecurityUtil.getAuthenticationToken();
		return RequestEntity.get(urlHelper.getMembersUrl(branchPath, refset, limit))
				.header(COOKIE, authenticationToken)
				.build();
	}

	private <T> T getEntity(URI uri, Class<T> responseType) throws RestClientException {
		RequestEntity<Void> get = RequestEntity.get(uri)
				.header(COOKIE, singleSignOnCookie)
				.build();

		return doExchange(get, responseType);
	}

	private <T, R> T doExchange(RequestEntity<R> request, Class<T> responseType) throws RestClientException {
		HttpStatusCode statusCode;
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
				.header(COOKIE, singleSignOnCookie)
				.body(request);

		HttpStatusCode statusCode;
		ResponseEntity<String> responseEntity = null;
		try {
			responseEntity = restTemplate.exchange(post, String.class);
			statusCode = responseEntity.getStatusCode();
		} catch (HttpClientErrorException | HttpServerErrorException e) {
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
			resty.json(urlHelper.getBranchesUrl(), RestyHelper.content((jsonObject), JSON_CONTENT_TYPE));
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

	public Set getMergeReviewsDetails(String mergeId) throws RestClientException{
		return getEntity(urlHelper.getMergeReviewsDetailsUri(mergeId), Set.class);
	}
	
	public boolean importRF2Archive(String projectName, String taskName, final InputStream rf2ZipFileStream)
			throws RestClientException {
		notNull(rf2ZipFileStream, "Archive to import should not be null.");

		try {
			// Create import
			String branchPath = urlHelper.getBranchPath(projectName, taskName);
			logger.info("Create import, branch '{}'", branchPath);

			JSONObject params = new JSONObject();
			params.put("type", "DELTA");
			params.put("branchPath", branchPath);
			params.put("languageRefSetId", US_EN_LANG_REFSET);
			params.put("createVersions", "false");
			resty.withHeader("Accept", JSON_CONTENT_TYPE);
			JSONResource json = resty.json(urlHelper.getImportsUrl(), RestyHelper.content(params, JSON_CONTENT_TYPE));
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
			logger.info("Snowstorm processing import, this will probably take a few minutes. (Import ID '{}')", importId);
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
	
	public ClassificationResults startClassification(String branchPath) throws RestClientException {
		ClassificationResults results = new ClassificationResults();
		try {
			JSONObject requestJson = new JSONObject().put("reasonerId", reasonerId).put("useExternalService", useExternalClassificationService);
			String classifyURL = urlHelper.getClassificationsUrl(branchPath);
			logger.info("Initiating classification via {}, reasonerId:{}, useExternalService:{}", classifyURL, reasonerId, useExternalClassificationService);
			JSONResource jsonResponse = resty.json(classifyURL, requestJson, JSON_CONTENT_TYPE);
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
		logger.info("Classifier running, this will probably take a few minutes. (Classification URL '{}')", classificationLocation);
		boolean classifierCompleted = waitForStatus(classificationLocation, getTimeoutDate(classificationTimeoutMinutes), ProcessingStatus.COMPLETED, "classifier");
		if (classifierCompleted) {
			// Fetch classification to get result summary flags.
			ResponseEntity<ClassificationResults> exchange = restTemplate.exchange(
					RequestEntity.get(URI.create(classificationLocation)).header(COOKIE, singleSignOnCookie).build(), ClassificationResults.class);
			if (!exchange.getStatusCode().is2xxSuccessful()) {
				throw new RestClientException("Failed to fetch completed classification.");
			}
			ClassificationResults classificationResults = exchange.getBody();
			classificationResults.setClassificationLocation(classificationLocation);
			return classificationResults;
		} else {
			throw new RestClientException("Classification failed, see logs for details.");
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
			resty.put(classifyURL, jsonObj, JSON_CONTENT_TYPE);
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

		JSONObject exportConfigJson = prepareExportJSON(branchPath, effectiveDate, moduleIds, exportCategory, exportType);

		String exportLocationURL = initiateExport(exportConfigJson.toString());

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
                case UNPUBLISHED -> {
                    String tet = (effectiveDate == null) ? DateUtils.now(DateUtils.YYYYMMDD) : effectiveDate;
                    jsonObj.put("transientEffectiveTime", tet);
                    if (flatIndexExportStyle) {
                        jsonObj.put("type", ExportType.DELTA);
                    }
                }
                case PUBLISHED -> {
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
                }
                case FEEDBACK_FIX -> {
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
                }
                default -> throw new BadRequestException("Export type " + exportCategory + " not recognised");
            }
		} catch (JSONException e) {
			throw new ProcessingException("Failed to prepare JSON for export request.", e);
		}
		return jsonObj;
	}

	private String initiateExport(String exportJsonString) throws BusinessServiceException {
		logger.info("Initiating export via url {} with json: {}", urlHelper.getExportsUrl(), exportJsonString);
		RequestEntity<?> post = RequestEntity.post(urlHelper.getExportsUri())
				.header(COOKIE, singleSignOnCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.body(exportJsonString);
		URI location = restTemplate.postForLocation(urlHelper.getExportsUrl(), post);
		if (location == null) {
			throw new ProcessingException("Failed to obtain location of export with " + exportJsonString);
		}
		return location.toString() + "/archive";
	}

	private File recoverExportedArchive(String exportLocationURL) {
		logger.debug("Recovering exported archive from {}", exportLocationURL);
		File exportedArchive = restTemplate.execute(exportLocationURL, HttpMethod.GET, null, responseExtractor -> {
			File archive = File.createTempFile("ts-extract", ".zip");
			try (OutputStream outputStream = new FileOutputStream(archive)) {
				StreamUtils.copy(responseExtractor.getBody(), outputStream);
				return archive;
			}
		});
		logger.debug("Extract saved to {}", exportedArchive.getAbsolutePath());
		return exportedArchive;
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
			resty.put(urlHelper.getMergesUrl(), params, JSON_CONTENT_TYPE);
		} catch (Exception e) {
			throw new RestClientException("Failed to merge " + sourcePath + " to " + targetPath, e);
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

	public boolean isUseExternalClassificationService() {
		return useExternalClassificationService;
	}

	public void setUseExternalClassificationService(boolean useExternalClassificationService) {
		this.useExternalClassificationService = useExternalClassificationService;
	}

	public void setImportTimeoutMinutes(int importTimeoutMinutes) {
		this.importTimeoutMinutes = importTimeoutMinutes;
	}

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

	public List<ConceptPojo> searchConcepts(String branchPath, List<String> conceptIds) throws RestClientException {
		List<ConceptPojo> result = new ArrayList<>();
		List<String> batchJob = null;
		int counter=0;
		for (String conceptId : conceptIds) {
			if (batchJob == null) {
				batchJob = new ArrayList<>();
			}
			batchJob.add(conceptId);
			counter++;
			if (counter % 1000 == 0 || counter == conceptIds.size()) {
				try {
					Map<String,Object> request = new HashMap<>();
					request.put("conceptIds", batchJob);			
					URI uri = urlHelper.getBulkConceptsUri(branchPath);
					
					RequestEntity<?> post = RequestEntity.post(uri)
							.header(COOKIE, singleSignOnCookie)
							.accept(MediaType.APPLICATION_JSON)
							.body(request);
					ResponseEntity<List<ConceptPojo>> response = null;
					ParameterizedTypeReference<List<ConceptPojo>> typeRef = new ParameterizedTypeReference<>() {
                    };
					response = restTemplate.exchange(post, typeRef);
					if (response == null) {
						throw new ResourceNotFoundException("Bulk search returned null result on branch " + branchPath);
					}
					result.addAll(response.getBody());
				} catch (Exception e) {
					throw new RestClientException("Failed to bulk search concepts on branch " + branchPath, e);
				}
				batchJob = null;
			}
		}
		return result;
	}

	public boolean isBranchLocked(String branchPath) throws RestClientException {
		Branch branch = getEntity(urlHelper.getBranchUri(branchPath), Branch.class);
		if (branch != null && branch.getMetadata() != null) {
			return branch.getMetadata().containsKey(LOCK);
		}
		return false;
	}

	public void setAuthorFlag(String branchPath, String key, String value) throws RestClientException {
		String authenticationToken = singleSignOnCookie != null ? singleSignOnCookie : SecurityUtil.getAuthenticationToken();
		RequestEntity<Map<String, String>> request = RequestEntity
				.post(urlHelper.getSetAuthorFlagUri(branchPath))
				.header(COOKIE, authenticationToken)
				.body(Map.of("name", key, "value", value));

		ResponseEntity<Object> exchange = restTemplate.exchange(request, Object.class);
		if (!exchange.getStatusCode().is2xxSuccessful()) {
			throw new RestClientException("Failed to set flag on branch " + branchPath);
		}
	}

	public String upgradeCodeSystem(String shortName, Integer newDependantVersion, Boolean contentAutomations) throws BusinessServiceException {
		Map<String,Object> request = new HashMap<>();
		request.put("newDependantVersion", newDependantVersion);
		request.put("contentAutomations", contentAutomations);

		URI uri = urlHelper.getCodeSystemUpgradeUri(shortName);

		RequestEntity<?> post = RequestEntity.post(uri)
				.header(COOKIE, singleSignOnCookie)
				.accept(MediaType.APPLICATION_JSON)
				.body(request);

		URI location = restTemplate.postForLocation(uri, post);
		if (location == null) {
			throw new ProcessingException("Failed to obtain location of code system upgrade");
		}
		return location.toString();
	}

	public CodeSystemUpgradeJob getCodeSystemUpgradeJob(String jobId) throws RestClientException {
		URI uri = urlHelper.getCodeSystemUpgradeJobUrl(jobId);
		return getEntity(uri, CodeSystemUpgradeJob.class);
	}

	public IntegrityIssueReport integrityCheck(String branch) {
		URI uri = urlHelper.getIntegrityCheckUrl(branch);
		RequestEntity<?> post = RequestEntity.post(uri)
				.header(COOKIE, singleSignOnCookie)
				.accept(MediaType.APPLICATION_JSON)
				.body(null);

		ParameterizedTypeReference<IntegrityIssueReport> typeRef = new ParameterizedTypeReference<>() {};
		ResponseEntity<IntegrityIssueReport> response = restTemplate.exchange(post, typeRef);

		return response.getBody();
	}

	public void generateAdditionalLanguageRefsetDelta(String shortName, String branchPath, String languageRefsetToCopyFrom, Boolean completeCopy) {
		URI uri = urlHelper.getCodeSystemGenerateAdditionalLanguageRefsetDeltaUri(shortName, branchPath, languageRefsetToCopyFrom, Boolean.TRUE.equals(completeCopy));
		RequestEntity<?> post = RequestEntity.post(uri)
				.header(COOKIE, singleSignOnCookie)
				.accept(MediaType.APPLICATION_JSON)
				.body(null);

		restTemplate.exchange(post, Void.class);
	}

	public List<PermissionRecord> findPermissionForBranch(String branchPath) {
		URI uri = urlHelper.getPermissionUrl(branchPath);
		RequestEntity<?> get = RequestEntity.get(uri)
				.header(COOKIE, singleSignOnCookie)
				.accept(MediaType.APPLICATION_JSON)
				.build();
		ParameterizedTypeReference<List<PermissionRecord>> typeRef = new ParameterizedTypeReference<>() {
		};
		ResponseEntity<List<PermissionRecord>> response = restTemplate.exchange(get, typeRef);
		return response.getBody();
	}
}
