package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import com.google.gson.annotations.Expose;

public class MergeReview {

	public boolean isFinalState() {
		return status != Status.PENDING;
	}

	// Enum for the status field
	public enum Status {
		FAILED,
		CONFLICTS,
		CURRENT,
		PENDING,
		STALE
	}

	@Expose
	private String id;

	@Expose
	private String sourcePath;

	@Expose
	private String targetPath;

	@Expose
	private String sourceToTargetReviewId;

	@Expose
	private String targetToSourceReviewId;

	@Expose
	private Status status;

	@Expose
	private long created;

	// Getters and setters (optional, Gson sets fields directly)
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getSourcePath() { return sourcePath; }
	public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

	public String getTargetPath() { return targetPath; }
	public void setTargetPath(String targetPath) { this.targetPath = targetPath; }

	public String getSourceToTargetReviewId() { return sourceToTargetReviewId; }
	public void setSourceToTargetReviewId(String sourceToTargetReviewId) { this.sourceToTargetReviewId = sourceToTargetReviewId; }

	public String getTargetToSourceReviewId() { return targetToSourceReviewId; }
	public void setTargetToSourceReviewId(String targetToSourceReviewId) { this.targetToSourceReviewId = targetToSourceReviewId; }

	public Status getStatus() { return status; }
	public void setStatus(Status status) { this.status = status; }

	public long getCreated() { return created; }
	public void setCreated(long created) { this.created = created; }

}
