package org.snomed.otf.scheduler.domain;

import java.util.Date;
import java.util.Map;

public class SnomedServiceException {
	
	private String environment;
	private String serviceName;
	private Date eventStartTime;
	private Date eventFailureTime;
	private String message;
	private Map<String, Object> configuration;
	
	public String getEnvironment() {
		return environment;
	}
	public void setEnvironment(String environment) {
		this.environment = environment;
	}
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public Date getEventStartTime() {
		return eventStartTime;
	}
	public void setEventStartTime(Date eventStartTime) {
		this.eventStartTime = eventStartTime;
	}
	public Date getEventFailureTime() {
		return eventFailureTime;
	}
	public void setEventFailureTime(Date eventFailureTime) {
		this.eventFailureTime = eventFailureTime;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Map<String, Object> getConfiguration() {
		return configuration;
	}
	public void setConfiguration(Map<String, Object> configuration) {
		this.configuration = configuration;
	}
	
	public String toString() {
		return environment + " " + serviceName + 
				"[Start: " + eventStartTime + " | " + eventFailureTime + "]" +
				configuration.toString();
	}
}
