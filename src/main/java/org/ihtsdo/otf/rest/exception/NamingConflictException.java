package org.ihtsdo.otf.rest.exception;

public class NamingConflictException extends Exception {

	public NamingConflictException(String message) {
		super(message);
	}

	public NamingConflictException(String message, Throwable cause) {
		super(message, cause);
	}

}
