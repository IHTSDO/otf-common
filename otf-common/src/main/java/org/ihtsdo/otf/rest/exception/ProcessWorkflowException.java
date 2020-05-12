package org.ihtsdo.otf.rest.exception;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"message", "localizedMessage", "stackTrace", "cause", "suppressed"})
public class ProcessWorkflowException extends Exception {

	public ProcessWorkflowException(String message) {
		super(message);
	}

	public ProcessWorkflowException(String message, Throwable cause) {
		super(message, cause);
	}

}
