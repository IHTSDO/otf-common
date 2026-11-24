package org.snomed.module.storage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Channel {
	private static final Logger LOGGER = LoggerFactory.getLogger(Channel.class);
	private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

	public interface Name {
		String BROWSER = "browser";
	}

	public interface Property {
		interface Visibility {
			String NAME = "visibility";
			String PUBLIC = "PUBLIC";
			String PRIVATE = "PRIVATE";
		}
	}

	private String channel;
	private List<String> environments;
	private String availableFrom;
	private String availableTo;
	private Map<String, Object> properties;

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public List<String> getEnvironments() {
		return environments;
	}

	public void setEnvironments(List<String> environments) {
		this.environments = environments;
	}

	public boolean isAvailableForEnvironment(String environment) {
		return environments != null && !environments.isEmpty() && environments.stream().anyMatch(e -> e.equalsIgnoreCase(environment));
	}

	public String getAvailableFrom() {
		return availableFrom;
	}

	public void setAvailableFrom(String availableFrom) {
		this.availableFrom = availableFrom;
	}

	public String getAvailableTo() {
		return availableTo;
	}

	public void setAvailableTo(String availableTo) {
		this.availableTo = availableTo;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	public boolean hasProperty(String name) {
		return name != null && !name.isEmpty() && properties != null && properties.containsKey(name);
	}

	public Optional<Object> getPropertyValue(String name) {
		if (name == null || name.isEmpty() || properties == null || properties.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(properties.get(name));
	}

	public String getPropertyValueAsStringNullable(String name) {
		return getPropertyValueAsString(name).orElse(null);
	}

	public Optional<String> getPropertyValueAsString(String name) {
		if (name == null || name.isEmpty() || properties == null || properties.isEmpty()) {
			return Optional.empty();
		}
		Object value = properties.get(name);
		if (value instanceof String s) {
			return Optional.of(s);
		}
		return Optional.empty();
	}

	public boolean isAvailable() {
		LocalDate today = LocalDate.now();
		try {
			if (availableFrom != null && today.isBefore(LocalDate.parse(availableFrom, YYYYMMDD))) {
				return false;
			}

			if (availableTo != null && today.isAfter(LocalDate.parse(availableTo, YYYYMMDD))) {
				return false;
			}
		} catch (DateTimeParseException e) {
			LOGGER.warn("Channel {} has invalid availability date - treating as unavailable. availableFrom={}, availableTo={}", channel, availableFrom, availableTo);
			return false;
		}

		return true;
	}
}
