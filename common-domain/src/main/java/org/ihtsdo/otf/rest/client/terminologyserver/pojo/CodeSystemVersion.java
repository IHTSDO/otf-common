package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.Date;

public class CodeSystemVersion {

	private String shortName;

	private Date importDate;

	private String parentBranchPath;

	private Integer effectiveDate;

	private String version;

	private String description;

	private String releasePackage;

	private Integer dependantVersionEffectiveTime;

	private String branchPath;

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public Date getImportDate() {
		return importDate;
	}

	public void setImportDate(Date importDate) {
		this.importDate = importDate;
	}

	public String getParentBranchPath() {
		return parentBranchPath;
	}

	public void setParentBranchPath(String parentBranchPath) {
		this.parentBranchPath = parentBranchPath;
	}

	public Integer getEffectiveDate() {
		return effectiveDate;
	}

	public void setEffectiveDate(Integer effectiveDate) {
		this.effectiveDate = effectiveDate;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getReleasePackage() {
		return releasePackage;
	}

	public void setReleasePackage(String releasePackage) {
		this.releasePackage = releasePackage;
	}

	public Integer getDependantVersionEffectiveTime() {
		return dependantVersionEffectiveTime;
	}

	public void setDependantVersionEffectiveTime(Integer dependantVersionEffectiveTime) {
		this.dependantVersionEffectiveTime = dependantVersionEffectiveTime;
	}

	public String getBranchPath() {
		return branchPath;
	}

	public void setBranchPath(String branchPath) {
		this.branchPath = branchPath;
	}
}
