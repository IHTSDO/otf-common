package org.ihtsdo.otf.rest.client.snowowl.pojo;

import java.util.Map;

public class ApiError {

	private String message;
	private String developerMessage;
	private Integer code;
	private Map<String, Object> additionalInfo;

	public ApiError() {
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getDeveloperMessage() {
		return developerMessage;
	}

	public void setDeveloperMessage(String developerMessage) {
		this.developerMessage = developerMessage;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public Map<String, Object> getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(Map<String, Object> additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	@Override
	public String toString() {
		return "ApiError{" +
				"message='" + message + '\'' +
				", developerMessage='" + developerMessage + '\'' +
				", code=" + code +
				", additionalInfo=" + additionalInfo +
				'}';
	}
}
