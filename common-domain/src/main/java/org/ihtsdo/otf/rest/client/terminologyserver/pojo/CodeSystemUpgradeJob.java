package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

public class CodeSystemUpgradeJob {

	public enum UpgradeStatus {
		RUNNING, COMPLETED, FAILED;
	}

	private Integer newDependantVersion;

	private String codeSystemShortname;

	private UpgradeStatus status;

	private String errorMessage;

	public Integer getNewDependantVersion() {
		return newDependantVersion;
	}

	public void setNewDependantVersion(Integer newDependantVersion) {
		this.newDependantVersion = newDependantVersion;
	}

	public String getCodeSystemShortname() {
		return codeSystemShortname;
	}

	public void setCodeSystemShortname(String codeSystemShortname) {
		this.codeSystemShortname = codeSystemShortname;
	}

	public UpgradeStatus getStatus() {
		return status;
	}

	public void setStatus(UpgradeStatus status) {
		this.status = status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
