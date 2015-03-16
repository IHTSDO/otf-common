package org.ihtsdo.otf.rest.exception;

/**
 * RuntimeException when effective date in file name doesn't match with expected date.
 */
public class EffectiveDateNotMatchedException extends RuntimeException {

	public EffectiveDateNotMatchedException(String errormsg) {
		super(errormsg);
	}
}
