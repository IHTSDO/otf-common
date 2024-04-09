package org.snomed.otf.script.dao;

import java.io.*;
import java.util.Properties;

import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.ihtsdo.otf.exception.TermServerScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is needed when we're not running as a Spring Boot application and don't 
 * have access to Autowired and all that goodness
 */
public class LocalProperties extends ResourceConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(LocalProperties.class);
	InputStream is;
	String prefix;
	String propertiesFilename = "application-local.properties";
	boolean isInitialised = false;
	Properties prop = new Properties();
	
	public LocalProperties(String prefix) {
		if (prefix != null) {
			if (!prefix.endsWith(".")) {
				prefix += ".";
			}
			this.prefix = prefix;
		} else {
			this.prefix = "";
		}
	}

	public void init() throws TermServerScriptException {
		try {
			is = getClass().getClassLoader().getResourceAsStream(propertiesFilename);
			if (is == null) {
				//If running locally, not from a jar file
				is = new FileInputStream(propertiesFilename);
			}
			if (is != null) {
				prop.load(is);
			} else {
				throw new FileNotFoundException("Property file '" + propertiesFilename + "' not found in the classpath");
			}
			isInitialised = true;
		} catch (Exception e) {
			throw new TermServerScriptException("Unable to load " + propertiesFilename, e);
		} finally {
			try {
				is.close();
			} catch (Exception e) {}
		}
	}
	
	public String getProperty (String propName) throws TermServerScriptException {
		if (!isInitialised) {
			init();
		}
		return prop.getProperty(prefix + propName);
	}
	
	public Boolean getBooleanProperty (String propName) throws TermServerScriptException {
		if (!isInitialised) {
			init();
		}
		String bool = prop.getProperty(prefix + propName);
		if (bool == null) {
			throw new IllegalArgumentException(prefix + propName + " was not found in local properties file");
		}
		return bool.equalsIgnoreCase("true");
	}

	public Integer getIntegerProperty(String propName, Integer defaultValue) {
		if (!isInitialised) {
			try {
				init();
			} catch (TermServerScriptException e) {
				LOGGER.warn(e.getMessage());
			}
		}

		return Integer.parseInt(prop.getProperty(prefix + propName, String.valueOf(defaultValue)));
	}

	public Double getFloatProperty(String propName, Double defaultValue) {
		if (!isInitialised) {
			try {
				init();
			} catch (TermServerScriptException e) {
				LOGGER.warn(e.getMessage());
			}
		}

		return Double.parseDouble(prop.getProperty(prefix + propName, String.valueOf(defaultValue)));
	}

}
