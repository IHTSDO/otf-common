package org.snomed.otf.traceability.domain;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

@Entity
@Table(
		name = "activity",
		indexes = {
			@Index(columnList = "branch", name = "branch_index"),
			@Index(columnList = "highest_promoted_branch", name = "highest_branch_index"),
			@Index(columnList = "user", name = "user_index"),
			@Index(columnList = "commitDate", name = "commit_date_index")
		}
)
public class Activity {

	@Id
	@GeneratedValue(strategy= GenerationType.AUTO)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user")
	private User user;

	@Enumerated
	private ActivityType activityType;

	@ManyToOne
	@JoinColumn(name = "branch")
	private Branch branch;

	@ManyToOne
	private Branch mergeSourceBranch;

	@ManyToOne
	@JoinColumn(name = "highest_promoted_branch")
	private Branch highestPromotedBranch;

	private String commitComment;
	private Date commitDate;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "activity", fetch = FetchType.EAGER)
	private Set<ConceptChange> conceptChanges;

	public Activity() {
	}

	public Activity(User user, String commitComment, Branch branch, Date commitDate) {
		this.user = user;
		this.commitComment = commitComment;
		this.branch = branch;
		this.commitDate = commitDate;
		this.highestPromotedBranch = branch;
		conceptChanges = new HashSet<>();
	}

	public void addConceptChange(ConceptChange conceptChange) {
		conceptChanges.add(conceptChange);
		conceptChange.setActivity(this);
	}

	public void setActivityType(ActivityType activityType) {
		this.activityType = activityType;
	}

	public void setMergeSourceBranch(Branch mergeSourceBranch) {
		this.mergeSourceBranch = mergeSourceBranch;
	}

	public void setHighestPromotedBranch(Branch highestPromotedBranch) {
		this.highestPromotedBranch = highestPromotedBranch;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getCommitComment() {
		return commitComment;
	}

	public Branch getBranch() {
		return branch;
	}

	public Date getCommitDate() {
		return commitDate;
	}

	public ActivityType getActivityType() {
		return activityType;
	}

	public Set<ConceptChange> getConceptChanges() {
		return conceptChanges;
	}

	public Branch getMergeSourceBranch() {
		return mergeSourceBranch;
	}

	public Branch getHighestPromotedBranch() {
		return highestPromotedBranch;
	}

	@Override
	public String toString() {
		return "Activity{" +
				"id=" + id +
				", user=" + user +
				", activityType=" + activityType +
				", commitComment='" + commitComment + '\'' +
				", branch=" + branch +
				", mergeSourceBranch=" + mergeSourceBranch +
				", highestPromotedBranch=" + highestPromotedBranch +
				", commitDate=" + commitDate +
				", conceptChanges=" + conceptChanges +
				'}';
	}
}
