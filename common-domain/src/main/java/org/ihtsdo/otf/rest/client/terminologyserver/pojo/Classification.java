package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import com.google.gson.annotations.Expose;

import java.util.Date;

public class Classification {

	@Expose
	private String id;

	@Expose
	private String path;

	@Expose
	private ClassificationStatus status;

	@Expose
	private String errorMessage;

	@Expose
	private String reasonerId;

	@Expose
	private String userId;

	@Expose
	private Date creationDate;

	@Expose
	private Date completionDate;

	@Expose
	private Date lastCommitDate;

	@Expose
	private Date saveDate;

	@Expose
	private Boolean inferredRelationshipChangesFound;

	@Expose
	private Boolean redundantStatedRelationshipsFound;

	@Expose
	private Boolean equivalentConceptsFound;

	public ClassificationStatus getStatus() {
		return status;
	}

	//We can probably calculate this from the id, rather than recover it
	private String location;

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getId() {
		return id;
	}

	public boolean hasInferredRelationshipChangesFound() {
		return inferredRelationshipChangesFound;
	}

	public boolean hasEquivalentConceptsFound() {
		return equivalentConceptsFound;
	}
	
	@Override
	public String toString() {
		String str = "";
		if (id != null ) {
			str += "Classification id: " + id + "\n";
		}
		if (status != null) {
			str += "status : " + status;
		} else {
			str += "No classification data received";
		}
		return str;
	}

	public void setId(String uuid) {
		this.id = uuid;
	}

	public void setStatus(ClassificationStatus status) {
		this.status = status;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Date getCompletionDate() {
		return completionDate;
	}

	public void setCompletionDate(Date completionDate) {
		this.completionDate = completionDate;
	}

	public Date getLastCommitDate() {
		return lastCommitDate;
	}

	public void setLastCommitDate(Date lastCommitDate) {
		this.lastCommitDate = lastCommitDate;
	}

	public Date getSaveDate() {
		return saveDate;
	}

	public void setSaveDate(Date saveDate) {
		this.saveDate = saveDate;
	}

	public Boolean getInferredRelationshipChangesFound() {
		return inferredRelationshipChangesFound;
	}

	public void setInferredRelationshipChangesFound(Boolean inferredRelationshipChangesFound) {
		this.inferredRelationshipChangesFound = inferredRelationshipChangesFound;
	}

	public Boolean getRedundantStatedRelationshipsFound() {
		return redundantStatedRelationshipsFound;
	}

	public void setRedundantStatedRelationshipsFound(Boolean redundantStatedRelationshipsFound) {
		this.redundantStatedRelationshipsFound = redundantStatedRelationshipsFound;
	}

	public Boolean getEquivalentConceptsFound() {
		return equivalentConceptsFound;
	}

	public void setEquivalentConceptsFound(Boolean equivalentConceptsFound) {
		this.equivalentConceptsFound = equivalentConceptsFound;
	}
}
