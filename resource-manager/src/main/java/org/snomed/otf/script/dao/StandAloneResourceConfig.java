package org.snomed.otf.script.dao;

import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.ihtsdo.otf.utils.StringUtils;
import org.ihtsdo.otf.exception.TermServerScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * This is needed when we're not running as a Spring Boot application and don't 
 * have access to Autowired and all that goodness
 */
public class StandAloneResourceConfig extends ResourceConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(StandAloneResourceConfig.class);


	private enum CONFIGURATION {
		READ_ONLY			("readonly"),
		USE_CLOUD			("useCloud"),
		LOCAL_PATH			("local.path"),
		CLOUD_BUCKET_NAME	("cloud.bucketName"),
		CLOUD_PATH			("cloud.path");

		private String value;

		CONFIGURATION(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	public void init(String prefix) throws TermServerScriptException {
		init(prefix, true);
	}

	public void init(String prefix, boolean validate) throws TermServerScriptException {
		LocalProperties properties = new LocalProperties(prefix);
		if (validate && !isConfigurationValid(properties)) {
			throw new TermServerScriptException("Check application-local.properties for correct S3 config. "
					+ describeConfiguration(properties));
		}

		setReadonly(properties.getBooleanProperty(CONFIGURATION.READ_ONLY.value));
		setUseCloud(properties.getBooleanProperty(CONFIGURATION.USE_CLOUD.value));
		setLocal(new Local(properties.getProperty(CONFIGURATION.LOCAL_PATH.value)));
		setCloud(new Cloud(properties.getProperty(CONFIGURATION.CLOUD_BUCKET_NAME.value),
				properties.getProperty(CONFIGURATION.CLOUD_PATH.value)));
	}

	private boolean isConfigurationValid(LocalProperties properties) {
		return Arrays.stream(CONFIGURATION.values())
				.allMatch(aConfig -> {
					try {
						if (StringUtils.isEmpty(properties.getProperty(aConfig.getValue()))) {
							if (aConfig == CONFIGURATION.CLOUD_PATH) {
								LOGGER.warn("Cloud path is empty, working from root of S3 bucket");
							} else {
								return false;
							}
						}
						return true;
					} catch (TermServerScriptException e) {
						return false;
					}
				});
	}

	/** Lists the fully-qualified key each CONFIGURATION entry resolves to, alongside the value found for it (if any),
	 * so a misconfiguration can be diagnosed without guessing which property name or prefix was actually used. */
	private String describeConfiguration(LocalProperties properties) {
		StringBuilder sb = new StringBuilder("Expected keys and values found: ");
		for (CONFIGURATION aConfig : CONFIGURATION.values()) {
			String fullyQualifiedKey = properties.getFullyQualifiedKey(aConfig.getValue());
			String foundValue;
			try {
				foundValue = properties.getProperty(aConfig.getValue());
			} catch (TermServerScriptException e) {
				foundValue = null;
			}
			sb.append(fullyQualifiedKey).append("=").append(foundValue == null ? "<not set>" : foundValue).append(", ");
		}
		return sb.toString();
	}
}
