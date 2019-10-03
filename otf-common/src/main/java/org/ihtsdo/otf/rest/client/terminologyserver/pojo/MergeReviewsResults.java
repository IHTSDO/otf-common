package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

public class MergeReviewsResults {
	public enum MergeReviewStatus {
		/** New, changed and detached concepts are still being collected. */
		PENDING,

		/** Changes are available, no commits have happened since the start of the review. */
		CURRENT,

		/** Computed differences are not up-to-date; a commit on either of the compared branches invalidated it. */
		STALE,
		
		/** Differences could not be computed for some reason. */
		FAILED
	}
	
	private String id;
	private String sourcePath;
	private String targetPath;
	private String sourceToTargetReviewId;
	private String targetToSourceReviewId;
	private MergeReviewStatus status;
	
	
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getSourcePath() {
		return sourcePath;
	}
	
	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}
	
	public String getTargetPath() {
		return targetPath;
	}
	
	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}
	
	public String getSourceToTargetReviewId() {
		return sourceToTargetReviewId;
	}
	
	public void setSourceToTargetReviewId(String sourceToTargetReviewId) {
		this.sourceToTargetReviewId = sourceToTargetReviewId;
	}
	
	public String getTargetToSourceReviewId() {
		return targetToSourceReviewId;
	}
	
	public void setTargetToSourceReviewId(String targetToSourceReviewId) {
		this.targetToSourceReviewId = targetToSourceReviewId;
	}

	public MergeReviewStatus getStatus() {
		return status;
	}

	public void setStatus(MergeReviewStatus status) {
		this.status = status;
	}
	
}
