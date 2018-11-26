package org.snomed.otf.scheduler.domain;

import java.util.*;

import javax.persistence.*;

@Entity
public class JobRun {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(columnDefinition = "BINARY(16)")
	UUID id;
	String jobName;
	
	@OneToOne(cascade = CascadeType.ALL)
	JobRunParameters parameters;
	
	String terminologyServerUrl;
	Date requestTime;
	String user;
	String authToken;
	JobStatus status;
	
	@Column(length = 65535,columnDefinition="Text")
	String debugInfo;
	Date resultTime;
	Integer issuesReported;
	String resultUrl;
	
	private JobRun () {
		parameters = new JobRunParameters();
	}
	
	static public JobRun create(String jobName, String user) {
		JobRun j = new JobRun();
		j.id = UUID.randomUUID();
		j.jobName = jobName;
		j.user = user;
		j.requestTime = new Date();
		return j;
	}
	
	static public JobRun create(JobSchedule jobSchedule) {
		JobRun j = new JobRun();
		j.id = UUID.randomUUID();
		j.jobName = jobSchedule.getJobName();
		j.user = jobSchedule.getUser();
		j.setParameters(new JobRunParameters(jobSchedule.getParameters().getParameterMap()));
		j.requestTime = new Date();
		return j;
	}
	public String getJobName() {
		return jobName;
	}
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	public String getTerminologyServerUrl() {
		return terminologyServerUrl;
	}
	public void setTerminologyServerUrl(String terminologyServerUrl) {
		this.terminologyServerUrl = terminologyServerUrl;
	}
	public Date getRequestTime() {
		return requestTime;
	}
	public void setRequestTime(Date requestTime) {
		this.requestTime = requestTime;
	}
	public String getUser() {
		return user;
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
	public Integer getIssuesReported() {
		return issuesReported;
	}
	public void setIssuesReported(Integer issuesReported) {
		this.issuesReported = issuesReported;
	}
	public String getResultUrl() {
		return resultUrl;
	}
	public void setResultUrl(String resultUrl) {
		this.resultUrl = resultUrl;
	}
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public void setUser(String user) {
		this.user = user;
	}
	@Override
	public String toString() {
		return jobName + " (id: " + id + ")"
				+ " for user '" + user + "' in status: "
				+ status 
				+ (debugInfo == null? "" : " (Reason: " + debugInfo + ")");
	}
	
	@Override 
	public JobRun clone() {
		JobRun clone = new JobRun();
		clone.setAuthToken(getAuthToken());
		clone.setDebugInfo(getDebugInfo());
		clone.setId(getId());
		clone.setIssuesReported(getIssuesReported());
		clone.setJobName(getJobName());
		clone.setParameters(getParameters());
		clone.setRequestTime(getRequestTime());
		clone.setResultTime(getResultTime());
		clone.setResultUrl(getResultUrl());
		clone.setStatus(getStatus());
		clone.setTerminologyServerUrl(getTerminologyServerUrl());
		clone.setUser(getUser());
		return clone;
	}

	public void setParameters(JobRunParameters parameters) {
		this.parameters = parameters;
	}

	public void setParameter(String key, String value) {
		parameters.setValue(key, value);
	}

	public String getParamValue(String key) {
		return parameters.getValue(key);
	}
	
	public JobRunParameters getParameters() {
		return parameters;
	}

}
