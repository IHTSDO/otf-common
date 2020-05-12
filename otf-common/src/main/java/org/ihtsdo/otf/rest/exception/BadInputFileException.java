package org.ihtsdo.otf.rest.exception;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"message", "localizedMessage", "stackTrace", "cause", "suppressed"})
public class BadInputFileException extends Exception {

	public BadInputFileException(String message) {
		super(message);
	}

}
