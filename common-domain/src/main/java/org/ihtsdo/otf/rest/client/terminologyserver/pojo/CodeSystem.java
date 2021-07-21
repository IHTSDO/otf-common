package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.Set;

public class CodeSystem {

	private String shortName;
	private String name;
	private String countryCode;
	private String maintainerType;
	private String branchPath;
	private Set <String> userRoles;

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

	public void setUserRoles(Set <String> userRoles) {
		this.userRoles = userRoles;
	}
}
