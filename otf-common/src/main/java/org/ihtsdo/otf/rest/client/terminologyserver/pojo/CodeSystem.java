package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

public class CodeSystem {

	private String shortName;
	private String name;
	private String countryCode;
	private String maintainerType;
	private String branchPath;

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
}
