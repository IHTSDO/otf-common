package org.ihtsdo.otf.rest.exception;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"message", "localizedMessage", "stackTrace", "cause", "suppressed"})
public class UncheckedBusinessServiceException extends RuntimeException {

	public UncheckedBusinessServiceException(String message) {
		super(message);
	}

}
