package org.ihtsdo.otf.rest.exception;

public class BusinessServiceRuntimeException extends RuntimeException {

	public BusinessServiceRuntimeException(String message) {
		super(message);
	}

	public BusinessServiceRuntimeException(Throwable cause) {
		super(cause);
	}

	public BusinessServiceRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}
}
