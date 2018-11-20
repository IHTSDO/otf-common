package org.ihtsdo.otf.rest.client.snowowl;

import java.net.URI;
import java.net.URISyntaxException;

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

	public URI getBranchUri(String branchPath) {
		return getUri(snomedUrl + "/branches/" + branchPath);
	}

	private URI getUri(String uri) {
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			throw new RuntimeException("URI Syntax Error '" + uri + "'", e);
		}
	}

	public URI getMembersUrl(String branchPath, String referenceSet, int limit) {
		return getUri (snomedUrl + "/" + branchPath + "/members" + "?referenceSet=" + referenceSet + "&limit=" + limit);
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
	
	public String getMergeReviewsUrl() {
		return snomedUrl + "/merge-reviews";
	}

	public URI getMergesUri() {
		return getUri(getMergesUrl());
	}

	public URI getMergeReviewsUri() {
		return getUri(getMergeReviewsUrl());
	}
	
	public URI getMergeReviewsUri(String mergeId) {
		return getUri(getMergeReviewsUrl() + "/" + mergeId);
	}
	
	public URI getMergeReviewsDetailsUri(String mergeId) {
		return getUri(getMergeReviewsUrl() + "/" + mergeId + "/details");
	}

	public URI getMergeUri(String mergeId) {
		return getUri(getMergesUrl() + "/" + mergeId);
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

	public String getBrowserConceptsUrl(String branchPath) {
		return snomedUrl + "/browser/" + branchPath + "/concepts";
	}

	public URI getBrowserConceptUri(String branchPath, String conceptId) {
		return getUri(getBrowserConceptsUrl(branchPath) + "/" + conceptId);
	}

	public String getSimpleConceptsUrl(String branchPath) {
		return snomedUrl + "/" + branchPath + "/concepts";
	}

	public URI getSimpleConceptUri(String branchPath, String conceptId) {
		return getUri(getSimpleConceptsUrl(branchPath) + "/" + conceptId);
	}
	
	public String getBulkConceptsUrl(String branchPath) {
		return snomedUrl + "/browser/" + branchPath + "/concepts/bulk-load";
	}
	
	public URI getBulkConceptsUri(String branchPath) {
		return getUri(getBulkConceptsUrl(branchPath));
	}
}
