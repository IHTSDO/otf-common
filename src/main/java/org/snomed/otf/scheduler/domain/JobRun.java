package org.snomed.otf.scheduler.domain;

import java.net.URL;
import java.util.*;

public class JobRun {
	int run;
	String jobName;
	List<JobParameter> parameters;
	URL terminologyServer;
	Date requestTime;
	String author;
	String authToken;
	JobStatus status;
	String debugInfo;
	Date resultTime;
	int issuesReported;
	URL result;
	
	public int getRun() {
		return run;
	}
	public void setRun(int run) {
		this.run = run;
	}
	public String getJobName() {
		return jobName;
	}
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	public List<JobParameter> getParameters() {
		return parameters;
	}
	public void setParameters(List<JobParameter> parameters) {
		this.parameters = parameters;
	}
	public URL getTerminologyServer() {
		return terminologyServer;
	}
	public void setTerminologyServer(URL terminologyServer) {
		this.terminologyServer = terminologyServer;
	}
	public Date getRequestTime() {
		return requestTime;
	}
	public void setRequestTime(Date requestTime) {
		this.requestTime = requestTime;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getAuthToken() {
		return authToken;
	}
	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}
	public JobStatus getStatus() {
		return status;
	}
	public void setStatus(JobStatus status) {
		this.status = status;
	}
	public String getDebugInfo() {
		return debugInfo;
	}
	public void setDebugInfo(String debugInfo) {
		this.debugInfo = debugInfo;
	}
	public Date getResultTime() {
		return resultTime;
	}
	public void setResultTime(Date resultTime) {
		this.resultTime = resultTime;
	}
	public int getIssuesReported() {
		return issuesReported;
	}
	public void setIssuesReported(int issuesReported) {
		this.issuesReported = issuesReported;
	}
	public URL getResult() {
		return result;
	}
	public void setResult(URL result) {
		this.result = result;
	}
}
