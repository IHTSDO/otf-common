package org.ihtsdo.otf.rest.client.snowowl.pojo;

public class ClassificationResults {

	private String classificationId;
	private String classificationLocation;
	private String status;

	public void setClassificationId(String classificationId) {
		this.classificationId = classificationId;
	}

	public String getClassificationId() {
		return classificationId;
	}

	public String getClassificationLocation() {
		return classificationLocation;
	}

	public void setClassificationLocation(String classificationLocation) {
		this.classificationLocation = classificationLocation;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
