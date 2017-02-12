package org.ihtsdo.otf.rest.client.snowowl;

import com.google.common.base.Joiner;
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
import org.ihtsdo.otf.rest.client.snowowl.pojo.ConceptPojo;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.otf.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.Assert;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.BinaryResource;
import us.monoid.web.JSONResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SnowOwlRestClient {

	public static final String SNOWOWL_CONTENT_TYPE = "application/vnd.com.b2international.snowowl+json";
	public static final String ANY_CONTENT_TYPE = "*/*";
	public static final FastDateFormat SIMPLE_DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd_HH-mm-ss");
	public static final String US_EN_LANG_REFSET = "900000000000509007";

	public enum ExtractType {
		DELTA, SNAPSHOT, FULL;

	};


	public enum ProcessingStatus {
		COMPLETED, SAVED;
	}


	public enum ExportType {
		PUBLISHED, UNPUBLISHED, FEEDBACK_FIX;
	}
	private final RestyHelper resty;

	private String reasonerId;
	private boolean flatIndexExportStyle = true;
	private String logPath;
	private String rolloverLogPath;
	private final Gson gson;
	private int importTimeoutMinutes;
	private int classificationTimeoutMinutes; //Timeout of 0 means don't time out.
	private static final int INDENT = 2;
	private static final Joiner COMMA_SEPARATED_JOINER = Joiner.on(',');
	private final SnowOwlRestUrlHelper urlHelper;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public SnowOwlRestClient(String snowOwlUrl, String clientId, String apiKey) {
		this.resty = new RestyHelper(ANY_CONTENT_TYPE);
		urlHelper = new SnowOwlRestUrlHelper(snowOwlUrl);
		resty.authenticate(snowOwlUrl, clientId, apiKey.toCharArray());
		gson = new GsonBuilder().setPrettyPrinting().create();
	}
	
	public SnowOwlRestClient(String snowOwlUrl, String clientId, String apiKey, String userName, Set<String> userRoles) {
		this(snowOwlUrl, clientId, apiKey);
		resty.withHeader("X-AUTH-username", userName);
		resty.withHeader("X-AUTH-roles", COMMA_SEPARATED_JOINER.join(userRoles));
	}

	public void createConcept(String branchPath, ConceptPojo newConcept) throws RestClientException {
		try {
			resty.json(urlHelper.getConceptsUrl(branchPath), RestyHelper.content(gson.toJson(newConcept)));
		} catch (IOException e) {
			final String message = "Failed to create concept";
			logger.error(message, e);
			throw new RestClientException(message);
		}
	}

	public Branch getBranch(String branchPath) throws RestClientException {
		try {
			final JSONResource jsonResource = resty.json(urlHelper.getBranchUrl(branchPath));
			final Integer httpStatus = jsonResource.getHTTPStatus();
			if (httpStatus == 404) {
				return null;
			}
			return gson.fromJson(jsonResource.toObject().toString(), Branch.class);
		} catch (Exception e) {
			final String message = "Failed to retrieve branch " + branchPath;
			logger.error(message, e);
			throw new RestClientException(message);
		}
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
		try {
			List<String> projectNames = new ArrayList<>();
			JSONResource json = resty.json(urlHelper.getBranchChildrenUrl(branchPath));
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
			throw new RestClientException("Failed to retrieve branch list.", e);
		} catch (Exception e) {
			throw new RestClientException("Failed to parse branch list.", e);
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

	
	public File exportTask(String projectName, String taskName, ExtractType extractType) throws Exception {
		String branchPath = urlHelper.getBranchPath(projectName, taskName);
		return export(branchPath, null, ExportType.UNPUBLISHED, extractType);
	}

	public File exportProject(String projectName, ExtractType extractType) throws Exception {
		String branchPath = urlHelper.getBranchPath(projectName, null);
		return export(branchPath, null, ExportType.UNPUBLISHED, extractType);
	}

	public File export(String branchPath, String effectiveDate, ExportType exportType, ExtractType extractType)
			throws BusinessServiceException {

		JSONObject jsonObj = prepareExportJSON(branchPath, effectiveDate, exportType, extractType);

		String exportLocationURL = initiateExport(jsonObj);

		return recoverExportedArchive(exportLocationURL);
	}
	
	private JSONObject prepareExportJSON(String branchPath, String effectiveDate, ExportType exportType, ExtractType extractType)
			throws BusinessServiceException {
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("type", extractType);
			jsonObj.put("branchPath", branchPath);
			switch (exportType) {
				case UNPUBLISHED:
					String tet = (effectiveDate == null) ? DateUtils.now(DateUtils.YYYYMMDD) : effectiveDate;
					jsonObj.put("transientEffectiveTime", tet);
					if (flatIndexExportStyle) {
						jsonObj.put("type", ExtractType.DELTA);
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
					throw new BadRequestException("Export type " + exportType + " not recognised");
			}
		} catch (JSONException e) {
			throw new ProcessingException("Failed to prepare JSON for export request.", e);
		}
		return jsonObj;
	}

	private String initiateExport(JSONObject jsonObj) throws BusinessServiceException {
		try {
			logger.info("Initiating export via url {} with json: {}", urlHelper.getExportsUrl(), jsonObj.toString());
			JSONResource jsonResponse = resty.json(urlHelper.getExportsUrl(), RestyHelper.content(jsonObj, SNOWOWL_CONTENT_TYPE));
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

	public void rebaseTask(String projectName, String taskName) throws RestClientException {
		String taskPath = urlHelper.getBranchPath(projectName, taskName);
		String projectPath = urlHelper.getBranchPath(projectName);
		logger.info("Rebasing branch {} from parent {}", taskPath, projectPath);
		merge(projectPath, taskPath);
	}

	public void mergeTaskToProject(String projectName, String taskName) throws RestClientException {
		String taskPath = urlHelper.getBranchPath(projectName, taskName);
		String projectPath = urlHelper.getBranchPath(projectName);
		logger.info("Promoting branch {} to {}", taskPath, projectPath);
		merge(taskPath, projectPath);
	}

	private void merge(String sourcePath, String targetPath) throws RestClientException {
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
}
