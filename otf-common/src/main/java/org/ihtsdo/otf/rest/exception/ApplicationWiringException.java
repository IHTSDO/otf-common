package org.ihtsdo.otf.rest.exception;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"message", "localizedMessage", "stackTrace", "cause", "suppressed"})
public class ApplicationWiringException extends RuntimeException {

	public ApplicationWiringException(String message) {
		super(message);
	}
}
