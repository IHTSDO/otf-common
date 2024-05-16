package org.ihtsdo.otf.exception;

import java.io.Serial;

public class ScriptException extends Exception {

	@Serial
	private static final long serialVersionUID = 2L;

	public ScriptException(String msg, Throwable t) {
		super(msg, t);
	}

	public ScriptException(String msg) {
		super(msg);
	}

	public ScriptException(Throwable t) {
		super(t);
	}

}
