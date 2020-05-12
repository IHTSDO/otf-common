package org.ihtsdo.otf.rest.exception;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"message", "localizedMessage", "stackTrace", "cause", "suppressed"})
public class NamingConflictException extends Exception {

	public NamingConflictException(String message) {
		super(message);
	}

	public NamingConflictException(String message, Throwable cause) {
		super(message, cause);
	}

}
