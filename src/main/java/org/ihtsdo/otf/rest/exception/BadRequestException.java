package org.ihtsdo.otf.rest.exception;

public class BadRequestException extends BusinessServiceException {

	public BadRequestException(String message) {
		super(message);
	}

	public BadRequestException(String message, Throwable cause) {
		super(message, cause);
	}

}
