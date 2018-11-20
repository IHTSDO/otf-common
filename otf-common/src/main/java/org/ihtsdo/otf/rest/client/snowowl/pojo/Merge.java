package org.ihtsdo.otf.rest.client.snowowl.pojo;

import java.util.Date;
import java.util.UUID;

public class Merge {

	public enum Status {
		SCHEDULED,
		IN_PROGRESS,
		COMPLETED,
		FAILED,
		CANCEL_REQUESTED,
		CONFLICTS
	}

	private UUID id;

	private String source;

	private String target;

	private Status status;

	private Date startDate;

	private Date endDate;

	private ApiError apiError;

	public Merge() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public ApiError getApiError() {
		return apiError;
	}

	public void setApiError(ApiError apiError) {
		this.apiError = apiError;
	}
}
