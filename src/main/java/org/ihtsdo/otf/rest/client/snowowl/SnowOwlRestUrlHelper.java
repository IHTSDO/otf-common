package org.ihtsdo.otf.rest.client.snowowl;

public class SnowOwlRestUrlHelper {

	private static final String MAIN = "MAIN";

	private final String snomedUrl;

	public SnowOwlRestUrlHelper(String snowOwlUrl) {
		this.snomedUrl = removeTrailingSlash(snowOwlUrl) + "/snomed-ct/v2";
	}

	public String getBranchesUrl() {
		return snomedUrl + "/branches";
	}

	public String getBranchUrl(String branchPath) {
		return snomedUrl + "/branches/" + branchPath;
	}

	public String getBranchChildrenUrl(String branchPath) {
		return snomedUrl + "/branches/" + branchPath + "/children";
	}

	public String getBranchUrlRelativeToMain(String pathRelativeToMain) {
		return snomedUrl + "/" + pathRelativeToMain;
	}

	public String getImportsUrl() {
		return snomedUrl + "/imports";
	}

	public String getImportUrl(String importId) {
		return snomedUrl + "/imports/" + importId;
	}

	public String getImportArchiveUrl(String importId) {
		return getImportUrl(importId) + "/archive";
	}

	public String getClassificationsUrl(String branchPath) {
		return snomedUrl + "/" + branchPath + "/classifications";
	}

	public String getClassificationUrl(String projectName, String taskName, String classificationId) {
		return getClassificationsUrl(getBranchPath(projectName, taskName)) + "/" + classificationId;
	}

	public String getEquivalentConceptsUrl(String classificationLocation) {
		return classificationLocation + "/equivalent-concepts";
	}

	public String getRelationshipChangesFirstTenThousand(String classificationLocation) {
		return classificationLocation + "/relationship-changes?limit=10000";
	}

	public String getExportsUrl() {
		return snomedUrl + "/exports";
	}

	public String getMergesUrl() {
		return snomedUrl + "/merges";
	}

	public String getMainBranchPath() {
		return MAIN;
	}

	public String getBranchPath(String projectName) {
		return getBranchPath(projectName, null);
	}

	public String getBranchPath(String projectName, String taskName) {
		String s = getMainBranchPath();
		if (projectName != null) {
			s += "/" + projectName;
			if (taskName != null) {
				s += "/" + taskName;
			}
		}
		return s;
	}

	public static String removeTrailingSlash(String snowOwlUrl) {
		int i = snowOwlUrl.lastIndexOf("/");
		if (i == snowOwlUrl.length() - 1) {
			snowOwlUrl = snowOwlUrl.substring(0, snowOwlUrl.length() - 1);
		}
		return snowOwlUrl;
	}

	public String getConceptsUrl(String branchPath) {
		return "/browser/" + branchPath + "/concepts";
	}
}