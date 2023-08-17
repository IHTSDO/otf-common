package org.ihtsdo.otf.rest.exception;

import java.io.Serial;

public class EntityAlreadyExistsException extends BusinessServiceException {

	@Serial
	private static final long serialVersionUID = -652015419944747839L;

	public EntityAlreadyExistsException(String message) {
		super(message);
	}
}
