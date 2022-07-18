package org.snomed.otf.scheduler.domain;

import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.*;

import org.ihtsdo.otf.utils.StringUtils;

@Entity
public class JobRun {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(columnDefinition = "BINARY(16)")
	UUID id;
	
	String jobName;
	
	String project;
	
	String task;
	
	String codeSystemShortname;
	
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	JobRunParameters parameters;
	
	String terminologyServerUrl;
	Date requestTime;
	String user;
	
	@Transient
	String authToken;
	JobStatus status;
	
	@Column(length = 65535,columnDefinition="Text")
	String debugInfo;
	Date resultTime;
	Integer issuesReported;
	String resultUrl;
	
	@Transient //No need to persist whitelist for every run, once for the Job is fine
	Set<WhiteListedConcept> whiteList;
	
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
				+ " with codeSystem '" + codeSystemShortname +"'"
				+ " in project '" + project + "'" + (task==null?"":(" task " + task))
				+ " with parameters: " + getParameters()
				+ " and resultUrl: '" + getResultUrl() + "'"
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
		clone.setProject(getProject());
		clone.setTask(getTask());
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
		//Must always have a parameters object, even if it's empty
		if (parameters == null) {
			this.parameters = new JobRunParameters();
		} else {
			this.parameters = parameters;
		}
	}

	public void setParameter(String key, Object value) {
		if (value instanceof Collection) {
			List<String> values = ((Collection<?>)value).stream()
					.map(v -> v.toString())
					.collect(Collectors.toList());
			parameters.setValues(key, values);
		} else {
			parameters.setValue(key, value);
		}
	}

	public String getMandatoryParamValue(String key) {
		String value = parameters.getValue(key);
		if (value == null || StringUtils.isEmpty(value.trim())) {
			throw new IllegalArgumentException("Mandatory parameter '" + key + "' was not supplied");
		}
		return value;
	}
	
	public String getParamValue(String key) {
		String value = parameters.getValue(key);
		if (value == null || StringUtils.isEmpty(value.trim())) {
			return null;
		}
		return value.trim();
	}
	
	public JobRunParameters getParameters() {
		return parameters;
	}

	public String getParamValue(String key, String defaultValue) {
		String value = parameters.getValue(key);
		if (value == null || StringUtils.isEmpty(value.trim())) {
			value = defaultValue;
		}
		return value;
	}
	
	public boolean getParamBoolean(String key) {
		return parameters.getBoolean(key);
	}

	public Set<WhiteListedConcept> getWhiteList() {
		return whiteList;
	}

	public void setWhiteList(Set<WhiteListedConcept> whiteList) {
		this.whiteList = whiteList;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getcodeSystemShortname() {
		return codeSystemShortname;
	}

	public void setcodeSystemShortname(String codeSystemShortname) {
		this.codeSystemShortname = codeSystemShortname;
	}

	public String getTask() {
		return task;
	}

	public void setTask(String task) {
		this.task = task;
	}

}
