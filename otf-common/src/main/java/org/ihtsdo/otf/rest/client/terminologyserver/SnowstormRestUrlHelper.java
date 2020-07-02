package org.ihtsdo.otf.rest.client.terminologyserver;

import java.net.URI;
import java.net.URISyntaxException;

public class SnowstormRestUrlHelper {

	private static final String MAIN = "MAIN";

	private final String snowstormUrl;

	public SnowstormRestUrlHelper(String snowstormUrl) {
		this.snowstormUrl = removeTrailingSlash(snowstormUrl);
	}

	public String getBranchesUrl() {
		return snowstormUrl + "/branches";
	}

	public String getBranchUrl(String branchPath) {
		return snowstormUrl + "/branches/" + branchPath;
	}

	public URI getBranchUri(String branchPath) {
		return getUri(snowstormUrl + "/branches/" + branchPath);
	}

	private URI getUri(String uri) {
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			throw new RuntimeException("URI Syntax Error '" + uri + "'", e);
		}
	}

	public URI getMembersUrl(String branchPath, String referenceSet, int limit) {
		return getUri (snowstormUrl + "/" + branchPath + "/members" + "?referenceSet=" + referenceSet + "&limit=" + limit);
	}

	public String getBranchChildrenUrl(String branchPath) {
		return snowstormUrl + "/branches/" + branchPath + "/children";
	}

	public String getBranchUrlRelativeToMain(String pathRelativeToMain) {
		return snowstormUrl + "/" + pathRelativeToMain;
	}

	public String getImportsUrl() {
		return snowstormUrl + "/imports";
	}

	public String getImportUrl(String importId) {
		return snowstormUrl + "/imports/" + importId;
	}

	public String getImportArchiveUrl(String importId) {
		return getImportUrl(importId) + "/archive";
	}

	public String getClassificationsUrl(String branchPath) {
		// ?sort=creationDate is needed on the Complete OWL version of Snow Owl.
		return snowstormUrl + "/" + branchPath + "/classifications?sort=creationDate";
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
		return snowstormUrl + "/exports";
	}

	public URI getExportsUri() {
		return getUri(getExportsUrl());
	}

	public String getMergesUrl() {
		return snowstormUrl + "/merges";
	}
	
	public String getMergeReviewsUrl() {
		return snowstormUrl + "/merge-reviews";
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
		return snowstormUrl + "/browser/" + branchPath + "/concepts";
	}

	public URI getBrowserConceptUri(String branchPath, String conceptId) {
		return getUri(getBrowserConceptsUrl(branchPath) + "/" + conceptId);
	}

	public String getSimpleConceptsUrl(String branchPath) {
		return snowstormUrl + "/" + branchPath + "/concepts";
	}

	public URI getSimpleConceptUri(String branchPath, String conceptId) {
		return getUri(getSimpleConceptsUrl(branchPath) + "/" + conceptId);
	}
	
	public String getBulkConceptsUrl(String branchPath) {
		return snowstormUrl + "/browser/" + branchPath + "/concepts/bulk-load";
	}
	
	public URI getBulkConceptsUri(String branchPath) {
		return getUri(getBulkConceptsUrl(branchPath));
	}

	public String getCodeSystemsUrl() {
		return snowstormUrl + "/codesystems";
	}
}
