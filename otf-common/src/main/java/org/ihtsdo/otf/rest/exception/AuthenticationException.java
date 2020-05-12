package org.ihtsdo.otf.rest.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AuthenticationException extends UncheckedBusinessServiceException {

	public AuthenticationException(String message) {
		super(message);
	}

	@Override
	@JsonIgnore
	public StackTraceElement[] getStackTrace() {
		return super.getStackTrace();
	}

	@Override
	@JsonIgnore
	public synchronized Throwable getCause() {
		return super.getCause();
	}
}
