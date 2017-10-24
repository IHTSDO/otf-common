package org.ihtsdo.otf.rest.client.snowowl.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.json.JSONObject;

@JsonPropertyOrder({"domainTemplateForPostcoordination", "domainTemplateForPrecoordination", "proximalPrimitiveConstraint", "guideURL", "domainConstraint", "parentDomain", "proximalPrimitiveRefinement"})
@JsonIgnoreProperties(ignoreUnknown = false)
public class AdditionalFieldsPojo {

	private String domainTemplateForPostcoordination;

	private String domainTemplateForPrecoordination;

	private String proximalPrimitiveConstraint;

	private String guideURL;

	private String domainConstraint;

	private String parentDomain;

	private String proximalPrimitiveRefinement;

	public AdditionalFieldsPojo() {}

	public String getDomainTemplateForPostcoordination() {
		return domainTemplateForPostcoordination;
	}

	public void setDomainTemplateForPostcoordination(String domainTemplateForPostcoordination) {
		this.domainTemplateForPostcoordination = domainTemplateForPostcoordination;
	}

	public String getDomainTemplateForPrecoordination() {
		return domainTemplateForPrecoordination;
	}

	public void setDomainTemplateForPrecoordination(String domainTemplateForPrecoordination) {
		this.domainTemplateForPrecoordination = domainTemplateForPrecoordination;
	}

	public String getProximalPrimitiveConstraint() {
		return proximalPrimitiveConstraint;
	}

	public void setProximalPrimitiveConstraint(String proximalPrimitiveConstraint) {
		this.proximalPrimitiveConstraint = proximalPrimitiveConstraint;
	}

	public String getGuideURL() {
		return guideURL;
	}

	public void setGuideURL(String guideURL) {
		this.guideURL = guideURL;
	}

	public String getDomainConstraint() {
		return domainConstraint;
	}

	public void setDomainConstraint(String domainConstraint) {
		this.domainConstraint = domainConstraint;
	}

	public String getParentDomain() {
		return parentDomain;
	}

	public void setParentDomain(String parentDomain) {
		this.parentDomain = parentDomain;
	}

	public String getProximalPrimitiveRefinement() {
		return proximalPrimitiveRefinement;
	}

	public void setProximalPrimitiveRefinement(String proximalPrimitiveRefinement) {
		this.proximalPrimitiveRefinement = proximalPrimitiveRefinement;
	}
}
