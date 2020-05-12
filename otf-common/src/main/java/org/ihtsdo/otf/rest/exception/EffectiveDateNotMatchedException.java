package org.ihtsdo.otf.rest.exception;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * RuntimeException when effective date in file name doesn't match with expected date.
 */
@JsonPropertyOrder({"message", "localizedMessage", "stackTrace", "cause", "suppressed"})
public class EffectiveDateNotMatchedException extends RuntimeException {

	public EffectiveDateNotMatchedException(String errormsg) {
		super(errormsg);
	}
}
