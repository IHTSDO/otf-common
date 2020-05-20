package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DescriptionPojo implements SnomedComponent {

	public enum Type {

		FSN("900000000000003001"), SYNONYM("900000000000013009"), TEXT_DEFINITION("900000000000550004");

		private final String conceptId;

		Type(String conceptId) {
			this.conceptId = conceptId;
		}

		public static Type fromConceptId(String conceptId) {
			for (Type value : values()) {
				if (value.conceptId.equals(conceptId)) {
					return value;
				}
			}
			return null;
		}

		public String getConceptId() {
			return conceptId;
		}
	}

	public enum CaseSignificance {

		ENTIRE_TERM_CASE_SENSITIVE("900000000000017005"), CASE_INSENSITIVE("900000000000448009"), INITIAL_CHARACTER_CASE_INSENSITIVE("900000000000020002");

		private final String conceptId;

		CaseSignificance(String conceptId) {
			this.conceptId = conceptId;
		}

		public static CaseSignificance fromConceptId(String conceptId) {
			for (CaseSignificance value : values()) {
				if (value.conceptId.equals(conceptId)) {
					return value;
				}
			}
			return null;
		}

		public String getConceptId() {
			return conceptId;
		}
	}

	public enum Acceptability {

		ACCEPTABLE("900000000000549004"), PREFERRED("900000000000548007");

		private final String conceptId;

		Acceptability(String conceptId) {
			this.conceptId = conceptId;
		}

		public static Acceptability fromConceptId(String conceptId) {
			for (Acceptability value : values()) {
				if (value.conceptId.equals(conceptId)) {
					return value;
				}
			}
			return null;
		}

		public String getConceptId() {
			return conceptId;
		}
	}

	private String descriptionId;

	private boolean active;
	
	private String effectiveTime;
	
	private boolean released;
	
	private String term;

	private String conceptId;

	private String moduleId;

	private String lang;

	private Type type;

	private CaseSignificance caseSignificance;

	private Map<String, Acceptability> acceptabilityMap;

	private ConceptPojo.InactivationIndicator inactivationIndicator;

	private Map<ConceptPojo.HistoricalAssociation, Set<String>> associationTargets;

	public DescriptionPojo() {
		active = true;
		lang = "en";
		type = Type.FSN;
		caseSignificance = CaseSignificance.CASE_INSENSITIVE;
		acceptabilityMap = new HashMap<>();
	}

	public DescriptionPojo(String term) {
		this();
		this.term = term;
	}

	@Override
	public String getId() {
		return descriptionId;
	}

	public String getDescriptionId() {
		return descriptionId;
	}

	public DescriptionPojo setDescriptionId(String descriptionId) {
		this.descriptionId = descriptionId;
		return this;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	public String getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(String effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	public boolean isReleased() {
		return released;
	}

	public void setReleased(boolean released) {
		this.released = released;
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public String getConceptId() {
		return conceptId;
	}

	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public CaseSignificance getCaseSignificance() {
		return caseSignificance;
	}

	public void setCaseSignificance(CaseSignificance caseSignificance) {
		this.caseSignificance = caseSignificance;
	}

	public Map<String, Acceptability> getAcceptabilityMap() {
		return acceptabilityMap;
	}

	public void setAcceptabilityMap(Map<String, Acceptability> acceptabilityMap) {
		this.acceptabilityMap = acceptabilityMap;
	}

	public ConceptPojo.InactivationIndicator getInactivationIndicator() {
		return inactivationIndicator;
	}

	public void setInactivationIndicator(ConceptPojo.InactivationIndicator inactivationIndicator) {
		this.inactivationIndicator = inactivationIndicator;
	}

	public Map<ConceptPojo.HistoricalAssociation, Set<String>> getAssociationTargets() {
		return associationTargets;
	}

	public void setAssociationTargets(Map<ConceptPojo.HistoricalAssociation, Set<String>> associationTargets) {
		this.associationTargets = associationTargets;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DescriptionPojo that = (DescriptionPojo) o;
		return active == that.active &&
				released == that.released &&
				Objects.equals(descriptionId, that.descriptionId) &&
				Objects.equals(effectiveTime, that.effectiveTime) &&
				Objects.equals(term, that.term) &&
				Objects.equals(conceptId, that.conceptId) &&
				Objects.equals(moduleId, that.moduleId) &&
				Objects.equals(lang, that.lang) &&
				Objects.equals(type, that.type) &&
				Objects.equals(caseSignificance, that.caseSignificance) &&
				Objects.equals(acceptabilityMap, that.acceptabilityMap) &&
				Objects.equals(inactivationIndicator, that.inactivationIndicator) &&
				Objects.equals(associationTargets, that.associationTargets);
	}

	@Override
	public int hashCode() {
		return Objects.hash(descriptionId, active, effectiveTime, released, term, conceptId, moduleId, lang, type, caseSignificance, acceptabilityMap, inactivationIndicator, associationTargets);
	}

	@Override
	public String toString() {
		return "DescriptionPojo{" +
				"descriptionId='" + descriptionId + '\'' +
				", active=" + active +
				", effectiveTime='" + effectiveTime + '\'' +
				", released=" + released +
				", term='" + term + '\'' +
				", conceptId='" + conceptId + '\'' +
				", moduleId='" + moduleId + '\'' +
				", lang='" + lang + '\'' +
				", type='" + type + '\'' +
				", caseSignificance='" + caseSignificance + '\'' +
				", acceptabilityMap=" + acceptabilityMap +
				", inactivationIndicator='" + inactivationIndicator + '\'' +
				", associationTargets='" + associationTargets + '\'' +
				'}';
	}
}
