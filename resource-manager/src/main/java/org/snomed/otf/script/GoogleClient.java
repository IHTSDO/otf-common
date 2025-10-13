package org.snomed.otf.script;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Properties;

@Component
public class GoogleClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleClient.class);

	private static final String APPLICATION_NAME = "SI Reporting Engine";

	private static final int MAX_WRITE_ATTEMPTS = 3;

	// Spring-injected values (if running in Spring)
	@Value("${google.client.type:#{null}}")
	private String clientType;

	@Value("${google.client.project_id:#{null}}")
	private String projectId;

	@Value("${google.client.private_key_id:#{null}}")
	private String privateKeyId;

	@Value("${google.client.private_key:#{null}}")
	private String privateKey;

	@Value("${google.client.client_email:#{null}}")
	private String clientEmail;

	@Value("${google.client.client_id:#{null}}")
	private String clientId;

	@Value("${google.client.auth_uri:#{null}}")
	private String authUri;

	@Value("${google.client.token_uri:#{null}}")
	private String tokenUri;

	@Value("${google.client.auth_provider_x509_cert_url:#{null}}")
	private String authProviderX509CertUrl;

	@Value("${google.client.client_x509_cert_url:#{null}}")
	private String clientX509CertUrl;

	private HttpRequestInitializer requestInitializer;

	// Manual fallback if not running in Spring
	private Properties fallbackProps = new Properties();

	Drive driveService;
	Sheets sheetsService;

	private void init() {
		fallbackProps = new Properties();
		loadClasspathProperties("application.properties");

		String[] candidates = {
				System.getProperty("user.dir") + File.separator + "application-local.properties", // local file
				System.getProperty("user.dir") + File.separator + "application.properties"
		};

		boolean loaded = false;
		for (String candidate : candidates) {
			if (tryLoadProperties(candidate)) {
				LOGGER.info("Loaded Google client config properties from {}", candidate);
				loaded = true;
				break; // stop at first successful load
			}
		}

		if (!loaded) {
			throw new IllegalStateException("No properties file found. Provide application-local.properties or application.properties");
		}

		// Optional sanity check
		if ("YOUR_CERT_URL_HERE".equals(choose("google.client.client_x509_cert_url", clientX509CertUrl))) {
			throw new IllegalStateException("Google client not configured. Supply value via Consul, or in application.properties or application-local.properties");
		}
	}

	/**
	 * Attempts to load properties from the given candidate.
	 *
	 * @param candidate either a full file path (local) or a classpath resource name
	 * @return true if successfully loaded, false otherwise
	 */
	private boolean tryLoadProperties(String candidate) {
		if (candidate.contains(File.separator)) {
			// local file
			File file = new File(candidate);
			if (file.exists() && file.canRead()) {
				try (InputStream in = new FileInputStream(file)) {
					fallbackProps.load(in);
					return true;
				} catch (IOException e) {
					LOGGER.error("Failed to load properties from local file {}", candidate, e);
				}
			}
		} else {
			// classpath resource
			try (InputStream in = GoogleClient.class.getClassLoader().getResourceAsStream(candidate)) {
				if (in != null) {
					fallbackProps.load(in);
					return true;
				}
			} catch (IOException e) {
				LOGGER.error("Failed to load properties from classpath resource {}", candidate, e);
			}
		}
		return false;
	}

	/**
	 * Load properties from a classpath resource.
	 */
	private void loadClasspathProperties(String resourceName) {
		try (InputStream in = GoogleClient.class.getClassLoader().getResourceAsStream(resourceName)) {
			if (in != null) {
				fallbackProps.load(in);
				LOGGER.debug("Loaded Google Client config classpath defaults from {}", resourceName);
			} else {
				LOGGER.warn("Classpath resource {} not found", resourceName);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to load classpath resource {}", resourceName, e);
		}
	}


	public void setupServices() throws IOException {
		init();

		if (sheetsService == null) {
			sheetsService = new Sheets.Builder(new NetHttpTransport(),
					GsonFactory.getDefaultInstance(),
					getRequestInitializer())
					.setApplicationName(APPLICATION_NAME)
					.build();
		}

		if (driveService == null) {
			driveService = new Drive.Builder(new NetHttpTransport(),
					GsonFactory.getDefaultInstance(),
					getRequestInitializer())
					.setApplicationName(APPLICATION_NAME)
					.build();
		}
	}

	public HttpRequestInitializer getRequestInitializer() throws IOException {
		if (requestInitializer == null) {
			List<String> scopes = List.of(SheetsScopes.DRIVE_FILE, SheetsScopes.SPREADSHEETS);

			// Prefer Spring-injected, else fallback from props
			String effectiveClientEmail = choose("google.client.client_email", clientEmail);
			String effectivePrivateKey = choose("google.client.private_key", privateKey)
					.replace("\\n", "\n"); // Vault usually escapes newlines

			// Build JSON structure expected by GoogleCredentials
			String json = String.format("""
                    {
                      "type": "%s",
                      "project_id": "%s",
                      "private_key_id": "%s",
                      "private_key": "%s",
                      "client_email": "%s",
                      "client_id": "%s",
                      "auth_uri": "%s",
                      "token_uri": "%s",
                      "auth_provider_x509_cert_url": "%s",
                      "client_x509_cert_url": "%s"
                    }
                    """,
					choose("google.client.type", clientType),
					choose("google.client.project_id", projectId),
					choose("google.client.private_key_id", privateKeyId),
					effectivePrivateKey,
					effectiveClientEmail,
					choose("google.client.client_id", clientId),
					choose("google.client.auth_uri", authUri),
					choose("google.client.token_uri", tokenUri),
					choose("google.client.auth_provider_x509_cert_url", authProviderX509CertUrl),
					choose("google.client.client_x509_cert_url", clientX509CertUrl)
			);

			GoogleCredentials credential = GoogleCredentials
					.fromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
					.createScoped(scopes);

			credential.refreshIfExpired();
			LOGGER.debug("Google credentials loaded OK.");
			requestInitializer = new HttpCredentialsAdapter(credential);
		}

		return requestInitializer;
	}

	private String choose(String key, String springValue) {
		if (springValue != null) {
			return springValue;
		}
		return fallbackProps.getProperty(key);
	}

	public Spreadsheet createSpreadSheet() throws IOException {
		Spreadsheet sheet = null;
		Spreadsheet requestBody = new Spreadsheet();
		Sheets.Spreadsheets.Create request = sheetsService.spreadsheets().create(requestBody);
		int attempt = 0;
		while (sheet == null) {
			try {
				sheet = request.execute();
			} catch (Exception e) {
				LOGGER.error("Failed to initialise sheet", e);
				if (++attempt < 3 ) {
					LOGGER.warn("Retrying...{}", attempt);
					try {
						Thread.sleep(5 * 1000L);
					} catch (InterruptedException i) {
						Thread.currentThread().interrupt();
					}
				} else {
					throw e;
				}
			}
		}
		LOGGER.info("Created: {}", sheet.getSpreadsheetUrl());
		setSheetPermissions(sheet);
		return sheet;
	}

	private void setSheetPermissions(Spreadsheet sheet) throws IOException {
		//And share it with everyone everywhere
		//See https://developers.google.com/drive/api/v2/reference/permissions/insert
		Permission perm = new Permission()
				.setKind("drive#permission")
				.setRole("writer")
				.setType("anyone");
		driveService.permissions()
				.create(sheet.getSpreadsheetId(), perm)
				.setSupportsTeamDrives(true)
				.execute();
		LOGGER.warn("Spreadsheet opened up to the universe.");
	}

	public void executeRequests(Spreadsheet sheet, List<Request> requests, boolean allowRetries) throws IOException, InterruptedException {
		//Execute creation of tabs
		BatchUpdateSpreadsheetRequest batch = new BatchUpdateSpreadsheetRequest();
		batch.setRequests(requests);
		int retry = 0;
		boolean createdOK = false;
		while (!createdOK && retry < 3) {
			try {
				sheetsService.spreadsheets().batchUpdate(sheet.getSpreadsheetId(), batch).execute();
				createdOK = true;
			} catch (SocketTimeoutException e) {
				if (++retry < 3 && allowRetries) {
					LOGGER.warn("Timeout received from Google. Retrying after short nap.");
					Thread.sleep(1000 * 10L);
				}
			}
		}
	}

	public void moveFile(String fileId, String targetFolderId) throws IOException {
		// Retrieve the existing parents to remove
		com.google.api.services.drive.model.File file = driveService.files().get(fileId)
				.setFields("parents")
				.setSupportsTeamDrives(true)
				.execute();
		StringBuilder previousParents = new StringBuilder();
		for (String parent : file.getParents()) {
			previousParents.append(parent);
			previousParents.append(',');
		}
		// Move the file to the new folder
		driveService.files().update(fileId, null)
				.setAddParents(targetFolderId)
				.setRemoveParents(previousParents.toString())
				.setSupportsTeamDrives(true)
				.setFields("id, parents")
				.execute();
	}

	public void writeToSheet(Spreadsheet sheet, BatchUpdateValuesRequest body) throws IOException {
		int writeAttempts = 0;
		boolean writeSuccess = false;

		while (!writeSuccess && writeAttempts <= MAX_WRITE_ATTEMPTS) {
			try {
				writeBatchUpdate(sheet, body);
				writeSuccess = true;
			} catch (Exception e) {
				if (shouldRetry(e, writeAttempts)) {
					handleRetry(e);
				} else {
					throw e;
				}
			}
			writeAttempts++;
		}
	}

	private void writeBatchUpdate(Spreadsheet sheet, BatchUpdateValuesRequest body) throws IOException {
		sheetsService.spreadsheets()
				.values()
				.batchUpdate(sheet.getSpreadsheetId(), body)
				.execute();
	}

	private boolean shouldRetry(Exception e, int attempt) {
		return attempt <= MAX_WRITE_ATTEMPTS &&
				(e.getMessage() == null || !e.getMessage().contains("INVALID_ARGUMENT"));
	}

	private void handleRetry(Exception e) {
		try {
			LOGGER.warn("Exception from Google Sheets, sleeping then trying again. Exception was: ", e);
			int sleepTime = 10_000;
			if (e.getMessage() != null &&
					(e.getMessage().contains("insufficient") || e instanceof ConcurrentModificationException)) {
				sleepTime = 1_000;
			}
			Thread.sleep(sleepTime);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		LOGGER.info("{} trying again...", e.getMessage());
	}

}
