package org.ihtsdo.otf.rest.client;


public class SnowOwlRestClientException extends Exception {
	public SnowOwlRestClientException(String message) {
		super(message);
	}

	public SnowOwlRestClientException(String message, Throwable cause) {
		super(message, cause);
	}
}
