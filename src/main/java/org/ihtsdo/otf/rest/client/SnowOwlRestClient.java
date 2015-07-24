package org.ihtsdo.otf.rest.client;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;

//import net.rcarz.jiraclient.Issue;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.ihtsdo.otf.rest.client.resty.HttpEntityContent;
import org.ihtsdo.otf.rest.client.resty.RestyHelper;
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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class SnowOwlRestClient {

	public static final String SNOWOWL_V1_CONTENT_TYPE = "application/vnd.com.b2international.snowowl+json";
	public static final String ANY_CONTENT_TYPE = "*/*";
	public static final String SNOMED_TERMINOLOGY_URL = "snomed-ct";
	public static final String MAIN = "MAIN";
	public static final String MAIN_BRANCH_URL = SNOMED_TERMINOLOGY_URL + "/" + MAIN;
	public static final String TASKS_URL = MAIN_BRANCH_URL + "/tasks";
	public static final String IMPORTS_URL = SNOMED_TERMINOLOGY_URL + "/imports";
	public static final String EXPORTS_URL = "/exports";
	public static final String CLASSIFICATIONS_URL = "/classifications";
	public static final String EQUIVALENT_CONCEPTS_URL = "/equivalent-concepts";
	public static final String RELATIONSHIP_CHANGES_URL = "/relationship-changes?limit=1000000";
	private static final FastDateFormat SIMPLE_DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd_HH-mm-ss");

	public enum ExtractType {
		DELTA, SNAPSHOT, FULL;
	};

	public enum BranchState {
		NOT_SYNCHRONIZED, SYNCHRONIZED, PROMOTED
	}

	public enum BranchType {
		MAIN, BRANCH
	}

	public enum ProcessingStatus {
		COMPLETED, SAVED
	}

	private final String snowOwlUrl;
	private final RestyHelper resty;
	private String reasonerId;
	private String logPath;
	private String rolloverLogPath;
	private final Gson gson;
	private int importTimeoutMinutes;
	private int classificationTimeoutMinutes; //Timeout of 0 means don't time out.

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public SnowOwlRestClient(String snowOwlUrl, String username, String password) {
		this.snowOwlUrl = snowOwlUrl;
		this.resty = new RestyHelper(ANY_CONTENT_TYPE);
		resty.authenticate(snowOwlUrl, username, password.toCharArray());
		gson = new GsonBuilder().setPrettyPrinting().create();
	}

	@SuppressWarnings("unchecked")
	public List<String> listBranches() throws SnowOwlRestClientException {
		try {
			JSONResource tasks = resty.json(snowOwlUrl + TASKS_URL);
			return (List<String>) tasks.get("items.taskId");
		} catch (IOException e) {
			throw new SnowOwlRestClientException("Failed to retrieve branch list.", e);
		} catch (Exception e) {
			throw new SnowOwlRestClientException("Failed to parse branch list.", e);
		}
	}

	/**
	 * @param branchName
	 * @return true if branch created, false if already existed
	 * @throws SnowOwlRestClientException
	 */
	public boolean getCreateBranch(String branchName) throws SnowOwlRestClientException {
		// http://localhost:8080/snowowl/snomed-ct/MAIN/tasks/create-1
		try {
			resty.json(snowOwlUrl + TASKS_URL + "/" + branchName);
			logger.info("Branch exists {}", branchName);
			return false;
		} catch (IOException e) {
			if (e.getCause() instanceof FileNotFoundException) {
				// Branch not found. Create.
				try {
					String json = "{\n" +
							"  \"description\": \"" + branchName + "\",\n" +
							"  \"taskId\": \"" + branchName + "\"\n" +
							"}";
					resty.json(snowOwlUrl + TASKS_URL, RestyHelper.content(new JSONObject(json), SNOWOWL_V1_CONTENT_TYPE));
					logger.info("Created branch {}", branchName);
					return true;
				} catch (IOException | JSONException e1) {
					throw new SnowOwlRestClientException("Failed to create branch '" + branchName + "'.", e1);
				}
			} else {
				throw new SnowOwlRestClientException("Failed to create branch '" + branchName + "'.", e);
			}
		}
	}

	public boolean importRF2Archive(String branchName, final InputStream rf2ZipFileStream) throws SnowOwlRestClientException {
		Assert.notNull(rf2ZipFileStream, "Archive to import should not be null.");

		try {
			// Create import
			logger.info("Create import, branch name '{}'", branchName);
			String jsonString = "{\n" +
					"  \"version\": \"MAIN\",\n" +
					"  \"type\": \"DELTA\",\n" +
					"  \"taskId\": \"" + branchName + "\",\n" +
					"  \"languageRefSetId\": \"900000000000509007\",\n" +
					"  \"createVersions\": false\n" +
					"}\n";

			resty.withHeader("Accept", SNOWOWL_V1_CONTENT_TYPE);
			JSONResource json = resty.json(snowOwlUrl + IMPORTS_URL, RestyHelper.content(new JSONObject(jsonString), SNOWOWL_V1_CONTENT_TYPE));
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
				resty.json(snowOwlUrl + IMPORTS_URL + "/" + importId + "/archive", new HttpEntityContent(httpEntity));

			} finally {
				tempFile.delete();
				tempDirectory.delete();
			}

			// Poll import entity until complete or times-out
			logger.info("SnowOwl processing import, this will probably take a few minutes. (Import ID '{}')", importId);
			return waitForStatus(snowOwlUrl + IMPORTS_URL + "/" + importId, getTimeoutDate(importTimeoutMinutes), ProcessingStatus.COMPLETED, "import");
		} catch (Exception e) {
			throw new SnowOwlRestClientException("Import failed.", e);
		}
	}
	
	public ClassificationResults startClassification (String branchPath) throws SnowOwlRestClientException {
		ClassificationResults results = new ClassificationResults();
		try {
			JSONObject requestJson = new JSONObject().put("reasonerId", reasonerId);
			String classifyURL = getClassificationsUrl(branchPath);
			logger.debug("Initiating classification via {}", classifyURL);
			JSONResource jsonResponse = resty.json(classifyURL, requestJson, SNOWOWL_V1_CONTENT_TYPE);
			String classificationLocation = jsonResponse.getUrlConnection().getHeaderField("Location");
			results.setClassificationId(classificationLocation.substring(classificationLocation.lastIndexOf("/") + 1));
			results.setClassificationLocation(classificationLocation);
		} catch (IOException | JSONException e) {
			throw new SnowOwlRestClientException("Create classification failed.", e);
		}
		return results;
	}

	/**
	 * Initiates a classification and waits for the results
	 * @param branchPath
	 * @return
	 * @throws SnowOwlRestClientException
	 * @throws InterruptedException
	 */
	public ClassificationResults classify(String branchPath) throws SnowOwlRestClientException, InterruptedException {

		ClassificationResults results = startClassification(branchPath);
		results = waitForClassificationToComplete(results);
		return results;
	}
	
	public ClassificationResults waitForClassificationToComplete(ClassificationResults results) throws SnowOwlRestClientException, InterruptedException {
		String classificationLocation = results.getClassificationLocation();
		logger.info("SnowOwl classifier running, this will probably take a few minutes. (Classification URL '{}')", classificationLocation);
		boolean classifierCompleted = waitForStatus(classificationLocation, getTimeoutDate(classificationTimeoutMinutes), ProcessingStatus.COMPLETED, "classifier");
		if (classifierCompleted) {
			results.setStatus(ProcessingStatus.COMPLETED.toString());
			try {
				// Check equivalent concepts
				JSONArray items = getItems(classificationLocation + EQUIVALENT_CONCEPTS_URL);
				boolean equivalentConceptsFound = !(items == null || items.length() == 0);
				results.setEquivalentConceptsFound(equivalentConceptsFound);
				if (equivalentConceptsFound) {
					results.setEquivalentConceptsJson(toPrettyJson(items.toString()));
				}
			} catch (Exception e) {
				throw new SnowOwlRestClientException("Failed to retrieve equivalent concepts of classification.", e);
			}
			try {
				// Check relationship changes
				JSONResource relationshipChangesUnlimited = resty.json(classificationLocation + RELATIONSHIP_CHANGES_URL);
				Integer total = (Integer) relationshipChangesUnlimited.get("total");
				results.setRelationshipChangesCount(total);
				Path tempDirectory = Files.createTempDirectory(getClass().getSimpleName());
				String date = SIMPLE_DATE_FORMAT.format(new Date());
				File file = new File(tempDirectory.toFile(), "relationship-changes-" + date + ".json");
				toPrettyJson(relationshipChangesUnlimited.object().toString(), file);
				results.setRelationshipChangesFile(file);
			} catch (Exception e) {
				throw new SnowOwlRestClientException("Failed to retrieve relationship changes of classification.", e);
			}
			return results;
		} else {
			throw new SnowOwlRestClientException("Classification failed, see SnowOwl logs for details.");
		}
	}

	public String getLatestClassificationOnBranch(String branchPath) throws SnowOwlRestClientException {
		final String classificationsUrl = getClassificationsUrl(branchPath);
		try {
			final JSONArray items = getItems(classificationsUrl);
			if (items != null) {
				final JSONObject jsonObject = items.getJSONObject(items.length() - 1);
				return jsonObject.toString();
			}
			return null;
		} catch (Exception e) {
			throw new SnowOwlRestClientException("Failed to retrieve list of classifications.", e);
		}
	}

	public void saveClassification(String branchPath, String classificationId) throws SnowOwlRestClientException,
			InterruptedException {
		String classificationUrl = getClassificationsUrl(branchPath) + "/" + classificationId;
		try {
			logger.debug("Saving classification via {}", classificationUrl);
			JSONObject jsonObj = new JSONObject().put("status", "SAVED");
			resty.put(classificationUrl, jsonObj, SNOWOWL_V1_CONTENT_TYPE);
			//We'll wait the same time for saving as we do for the classification
			boolean savingCompleted = waitForStatus(classificationUrl, getTimeoutDate(classificationTimeoutMinutes),
					ProcessingStatus.SAVED, "classifier result saving");
			if (!savingCompleted) {
				throw new IOException("Classifier reported non-saved status when saving");
			}
		} catch (IOException | JSONException e) {
			throw new SnowOwlRestClientException("Failed to save classification via URL " + classificationUrl, e);
		}
	}

	private JSONArray getItems(String url) throws Exception {
		JSONResource jsonResource = resty.json(url);
		JSONArray items = null;
		try {
			items = (JSONArray) jsonResource.get("items");
		} catch (JSONException e) {
			// this gets thrown when the attribute does not exist
			logger.info("No items property of resource at '{}'", url);
		}
		return items;
	}

	public File exportBranch(String branchName, ExtractType extractType, String deltaStartEffectiveTime) throws Exception {
		return export(null, branchName, extractType, deltaStartEffectiveTime);
	}

	public File exportVersion(String version, ExtractType extractType) throws Exception {
		// Note that version could be "MAIN" to extract latest unversioned content on the main branch
		return export(version, null, extractType, null);
	}
	
	private File export(String version, String branchName, ExtractType extractType, String deltaStartEffectiveTime) throws Exception {

		String exportURL = snowOwlUrl + SNOMED_TERMINOLOGY_URL + EXPORTS_URL;
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("type", extractType);

		if (version != null) {
			jsonObj.put("version", version);
		} else {
			jsonObj.put("version", MAIN);
		}

		if (branchName != null) {
			jsonObj.put("taskId", branchName);
		}

		if (deltaStartEffectiveTime != null) {
			jsonObj.put("deltaStartEffectiveTime", deltaStartEffectiveTime);
		}

		jsonObj.put("transientEffectiveTime", DateUtils.today(DateUtils.YYYYMMDD));

		logger.info("Initiating export from {} with json: {}", exportURL, jsonObj.toString());
		JSONResource jsonResponse = resty.json(exportURL, RestyHelper.content(jsonObj, SNOWOWL_V1_CONTENT_TYPE));
		Object exportLocationURLObj = jsonResponse.getUrlConnection().getHeaderField("Location");
		String exportLocationURL = exportLocationURLObj.toString() + "/archive";

		logger.debug("Recovering exported archive from {}", exportLocationURL);
		resty.withHeader("Accept", ANY_CONTENT_TYPE);
		BinaryResource archiveResource = resty.bytes(exportLocationURL);
		File archive = File.createTempFile("ts-extract", ".zip");
		archiveResource.save(archive);
		logger.debug("Extract saved to {}", archive.getAbsolutePath());
		return archive;
	}

	public void promoteBranch(String branchName) throws IOException, JSONException {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("state", BranchState.PROMOTED.name());
		String promotionURL = snowOwlUrl + TASKS_URL + "/" + branchName;
		logger.info("Promoting branch via URL: {} with JSON: {}", promotionURL, jsonObj.toString());
		resty.put(promotionURL, jsonObj, SNOWOWL_V1_CONTENT_TYPE);
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
			throws SnowOwlRestClientException, InterruptedException {
		String status = "";
		boolean finalStateAchieved = false;
		while (!finalStateAchieved) {
			try {
				Object statusObj = resty.json(url).get("status");
				status = statusObj.toString() ;
			} catch (Exception e) {
				throw new SnowOwlRestClientException("Rest client error while checking status of " + waitingFor + ".", e);
			}
			finalStateAchieved = !("RUNNING".equals(status) || "SCHEDULED".equals(status) || "SAVING_IN_PROGRESS".equals(status));
			if (!finalStateAchieved && timeoutDate != null && new Date().after(timeoutDate)) {
				throw new SnowOwlRestClientException("Client timeout waiting for " + waitingFor + ".");
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

	private String getClassificationsUrl(String branchPath) throws SnowOwlRestClientException {
		return snowOwlUrl + branchPath + CLASSIFICATIONS_URL;
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
}
