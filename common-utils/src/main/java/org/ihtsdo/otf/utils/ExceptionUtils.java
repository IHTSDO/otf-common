package org.ihtsdo.otf.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtils {
	
	public static String getExceptionCause(String msg, Throwable t) {
		msg += " due to: ";
		String reason = t.getMessage();
		if (reason == null || reason.equals("remove")) {
			String clazz = t.getClass().getSimpleName();
			String location = t.getStackTrace()[0].toString();
			reason = clazz + " at " + location;
		}
		msg += reason;
		if (t.getCause() != null) {
			if (t.getCause().getMessage() == null || (t.getCause().getMessage() != null && !t.getCause().getMessage().equals(reason))) {
				msg = getExceptionCause(msg, t.getCause());
			}
		}
		return msg;
	}

	public static String getStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
	
}
