package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

public class ClassificationResults {

	private String id;
	private String classificationLocation;
	private ClassificationStatus status;
	private String userId;
	private boolean inferredRelationshipChangesFound;
	private boolean equivalentConceptsFound;

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getClassificationId() {
		return id;
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

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public boolean isInferredRelationshipChangesFound() {
		return inferredRelationshipChangesFound;
	}

	public void setInferredRelationshipChangesFound(boolean inferredRelationshipChangesFound) {
		this.inferredRelationshipChangesFound = inferredRelationshipChangesFound;
	}

	public boolean isEquivalentConceptsFound() {
		return equivalentConceptsFound;
	}

	public void setEquivalentConceptsFound(boolean equivalentConceptsFound) {
		this.equivalentConceptsFound = equivalentConceptsFound;
	}

	@Override
	public String toString() {
		return "ClassificationResults{" +
				"id='" + id + '\'' +
				", classificationLocation='" + classificationLocation + '\'' +
				", status='" + status + '\'' +
				", userId='" + userId + '\'' +
				", inferredRelationshipChangesFound=" + inferredRelationshipChangesFound +
				", equivalentConceptsFound=" + equivalentConceptsFound +
				'}';
	}
}
