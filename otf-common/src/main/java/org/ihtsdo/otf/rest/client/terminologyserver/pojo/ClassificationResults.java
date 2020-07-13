package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.io.File;

import com.google.gson.annotations.Expose;

public class ClassificationResults {

	@Expose
	private String classificationId;
	
	@Expose
	private ClassificationStatus status;
	
	private boolean equivalentConceptsFound;
	private boolean inferredRelationshipChangesFound;

	private int relationshipChangesCount;
	private String equivalentConceptsJson;
	private File relationshipChangesFile;
	private String classificationLocation;

	public void setEquivalentConceptsFound(boolean equivalentConceptsFound) {
		this.equivalentConceptsFound = equivalentConceptsFound;
	}

	public boolean isEquivalentConceptsFound() {
		return equivalentConceptsFound;
	}

	public boolean isInferredRelationshipChangesFound() {
		return inferredRelationshipChangesFound;
	}

	public void setInferredRelationshipChangesFound(boolean inferredRelationshipChangesFound) {
		this.inferredRelationshipChangesFound = inferredRelationshipChangesFound;
	}

	public void setRelationshipChangesCount(int relationshipChangesCount) {
		this.relationshipChangesCount = relationshipChangesCount;
	}

	public int getRelationshipChangesCount() {
		return relationshipChangesCount;
	}

	public void setClassificationId(String classificationId) {
		this.classificationId = classificationId;
	}

	public String getClassificationId() {
		return classificationId;
	}

	public void setEquivalentConceptsJson(String equivalentConceptsJson) {
		this.equivalentConceptsJson = equivalentConceptsJson;
	}

	public String getEquivalentConceptsJson() {
		return equivalentConceptsJson;
	}

	public void setRelationshipChangesFile(File relationshipChangesFile) {
		this.relationshipChangesFile = relationshipChangesFile;
	}

	public File getRelationshipChangesFile() {
		return relationshipChangesFile;
	}

	public String getClassificationLocation() {
		return classificationLocation;
	}

	public void setClassificationLocation(String classificationLocation) {
		this.classificationLocation = classificationLocation;
	}

	public ClassificationStatus getStatus() {
		return status;
	}

	public void setStatus(ClassificationStatus status) {
		this.status = status;
	}

}

