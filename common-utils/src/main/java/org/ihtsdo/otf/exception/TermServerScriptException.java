package org.ihtsdo.otf.exception;

import java.io.Serial;

public class TermServerScriptException extends ScriptException {

	@Serial
	private static final long serialVersionUID = 1L;

	public TermServerScriptException(String msg, Throwable t) {
		super(msg, t);
	}

	public TermServerScriptException(String msg) {
		super(msg);
	}

	public TermServerScriptException(Throwable t) {
		super(t);
	}
}
