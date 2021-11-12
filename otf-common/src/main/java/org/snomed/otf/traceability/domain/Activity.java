package org.snomed.otf.traceability.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Activity {
	private String id;
	private String username;
	private String branch;
	private int branchDepth;
	private String sourceBranch;
	private String highestPromotedBranch;
	private Date commitDate;
	private ActivityType activityType;
	private final List<ConceptChange> conceptChanges = new ArrayList<>();

	public Activity() {
	}

	public Activity(String id, String username, String branch, int branchDepth, String sourceBranch,
					String highestPromotedBranch, Date commitDate, ActivityType activityType, List<ConceptChange> conceptChanges) {
		this.id = id;
		this.username = username;
		this.branch = branch;
		this.branchDepth = branchDepth;
		this.sourceBranch = sourceBranch;
		this.highestPromotedBranch = highestPromotedBranch;
		this.commitDate = commitDate;
		this.activityType = activityType;
		if (conceptChanges != null) {
			this.conceptChanges.addAll(conceptChanges);
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public int getBranchDepth() {
		return branchDepth;
	}

	public void setBranchDepth(int branchDepth) {
		this.branchDepth = branchDepth;
	}

	public String getSourceBranch() {
		return sourceBranch;
	}

	public void setSourceBranch(String sourceBranch) {
		this.sourceBranch = sourceBranch;
	}

	public String getHighestPromotedBranch() {
		return highestPromotedBranch;
	}

	public void setHighestPromotedBranch(String highestPromotedBranch) {
		this.highestPromotedBranch = highestPromotedBranch;
	}

	public Date getCommitDate() {
		return commitDate;
	}

	public void setCommitDate(Date commitDate) {
		this.commitDate = commitDate;
	}

	public ActivityType getActivityType() {
		return activityType;
	}

	public void setActivityType(ActivityType activityType) {
		this.activityType = activityType;
	}

	public List<ConceptChange> getConceptChanges() {
		return conceptChanges;
	}

	public void setConceptChanges(List<ConceptChange> conceptChanges) {
		if (conceptChanges != null) {
			this.conceptChanges.addAll(conceptChanges);
		}
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Activity{");
		sb.append("id = " + id);
		sb.append(", username = " + username);
		sb.append(", branch = " + branch);
		sb.append(", commitDate = " + commitDate);
		sb.append(", activityType = " + activityType);
		sb.append("}");
		return sb.toString();
	}
}
