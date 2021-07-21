package org.snomed.otf.script;

import org.ihtsdo.otf.RF2Constants;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Project;
import org.ihtsdo.otf.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.scheduler.domain.JobRun;

public abstract class Script implements RF2Constants {
	
	static Logger logger = LoggerFactory.getLogger(Script.class);
	
	public static void info (String msg) {
		logger.info(msg);
	}
	
	public static void debug (Object obj) {
		logger.debug(obj==null?"NULL":obj.toString());
	}
	
	public static void warn (Object obj) {
		logger.warn("*** " + (obj==null?"NULL":obj.toString()));
	}
	
	public static void error (Object obj, Exception e) {
		System.err.println ("*** " + (obj==null?"NULL":obj.toString()));
		if (e != null) 
			logger.error(ExceptionUtils.getStackTrace(e));
	}
	
	public static void print (Object msg) {
		System.out.print (msg.toString());
	}
	
	public static void println (Object msg) {
		System.out.println (msg.toString());
	}
	
	public static String getMessage (Exception e) {
		String msg = e.getMessage();
		Throwable cause = e.getCause();
		if (cause != null) {
			msg += " caused by " + cause.getMessage();
			if (cause.getMessage() != null && cause.getMessage().length() < 6) {
				msg += " @ " + cause.getStackTrace()[0];
			}
		}
		return msg;
	}

	public abstract boolean isOffline();

	public abstract JobRun getJobRun();

	public abstract String getReportName();

	public abstract Project getProject();

	public abstract String detectReleaseBranch();

	public abstract String getEnv();

	public String getReportComplexName() {
		//Currently only used by SummaryComponentStats as it compares to releases
		return "";
	}
	
}
