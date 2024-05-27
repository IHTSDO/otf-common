package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConceptMiniPojo implements IConcept {
	
	private String conceptId;

	private String effectiveTime;

	private Boolean active;

	private DescriptionMiniPojo fsn;
	
	private DescriptionMiniPojo pt;
	
	private String moduleId;
	
	private String definitionStatus;
	
	public ConceptMiniPojo() {
	}

	public ConceptMiniPojo(String conceptId) {
		this.conceptId = conceptId;
	}

	public String getConceptId() {
		return conceptId;
	}

	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}

	public String getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(String effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public DescriptionMiniPojo getFsn() {
		return fsn;
	}

	public DescriptionMiniPojo getPt() {
		return pt;
	}

	public void setPt(DescriptionMiniPojo pt) {
		this.pt = pt;
	}

	public void setFsn(DescriptionMiniPojo fsn) {
		this.fsn = fsn;
	}

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	public String getDefinitionStatus() {
		return definitionStatus;
	}

	public void setDefinitionStatus(String definitionStatus) {
		this.definitionStatus = definitionStatus;
	}

	public String toStringFull() {
		StringBuilder builder = new StringBuilder();
		builder.append("ConceptMiniPojo [");
		if (conceptId != null)
			builder.append("conceptId=").append(conceptId).append(", ");
		if (fsn != null)
			builder.append("fsn=").append(fsn);
		builder.append("]");
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return conceptId + "|" + (fsn==null?null:fsn.getTerm()) + "|";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ConceptMiniPojo that = (ConceptMiniPojo) o;
		return Objects.equals(conceptId, that.conceptId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(conceptId);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class DescriptionMiniPojo {
		private String term;
		
		private String lang;
		
		public DescriptionMiniPojo() {
			
		}

		public DescriptionMiniPojo (String term, String lang) {
			this.term = term;
			this.lang = lang;
		}
		
		public String getTerm() {
			return term;
		}

		public void setTerm(String term) {
			this.term = term;
		}

		public String getLang() {
			return lang;
		}

		public void setLang(String lang) {
			this.lang = lang;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((lang == null) ? 0 : lang.hashCode());
			result = prime * result + ((term == null) ? 0 : term.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DescriptionMiniPojo other = (DescriptionMiniPojo) obj;
			if (lang == null) {
				if (other.lang != null)
					return false;
			} else if (!lang.equals(other.lang))
				return false;
			if (term == null) {
				if (other.term != null)
					return false;
			} else if (!term.equals(other.term))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "DescriptionMiniPojo [term=" + term + ", lang=" + lang + "]";
		}
	}

	@Override
	public String getFsnTerm() {
		return fsn.getTerm();
	}
}
