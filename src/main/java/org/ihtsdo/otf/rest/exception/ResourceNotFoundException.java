package org.ihtsdo.otf.rest.exception;

public class ResourceNotFoundException extends BusinessServiceRuntimeException {

	private static final long serialVersionUID = -4281549626769059242L;

	public ResourceNotFoundException(String resourceType, String resourceKey) {
		super(resourceType + " with key " + resourceKey + " is not accessible.");
	}

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public ResourceNotFoundException(Throwable cause) {
		super(cause);
	}

	public ResourceNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
