package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Collection;
import java.util.Set;

public class CodeSystem {

	@SerializedName("shortName")
	@Expose
	private String shortName;

	@SerializedName("name")
	@Expose
	private String name;

	@SerializedName("countryCode")
	@Expose
	private String countryCode;

	@SerializedName("maintainerType")
	@Expose
	private String maintainerType;

	@SerializedName("branchPath")
	@Expose
	private String branchPath;

	@SerializedName("userRoles")
	@Expose
	private Set <String> userRoles;

	@SerializedName("modules")
	@Expose
	private Collection<ConceptMiniPojo> modules;

	@SerializedName("dependantVersionEffectiveTime")
	@Expose
	private Integer dependantVersionEffectiveTime;

	@SerializedName("latestVersion")
	@Expose
	private CodeSystemVersion latestVersion;

	public String getShortName() {
		return shortName;
	}

	public String getName() {
		return name;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public String getMaintainerType() {
		return maintainerType;
	}

	public String getBranchPath() {
		return branchPath;
	}

	public Set <String> getUserRoles() {
		return userRoles;
	}

	public void setUserRoles(Set<String> userRoles) {
		this.userRoles = userRoles;
	}

	public void setModules(Collection<ConceptMiniPojo> modules) {
		this.modules = modules;
	}

	public Collection<ConceptMiniPojo> getModules() {
		return modules;
	}

	public Integer getDependantVersionEffectiveTime() {
		return dependantVersionEffectiveTime;
	}

	public void setDependantVersionEffectiveTime(Integer dependantVersionEffectiveTime) {
		this.dependantVersionEffectiveTime = dependantVersionEffectiveTime;
	}

	public void setLatestVersion(CodeSystemVersion latestVersion) {
		this.latestVersion = latestVersion;
	}

	public CodeSystemVersion getLatestVersion() {
		return latestVersion;
	}
}
