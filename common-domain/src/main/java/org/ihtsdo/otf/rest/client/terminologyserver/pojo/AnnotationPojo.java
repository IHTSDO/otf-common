package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotationPojo implements SnomedComponent {

    private String annotationId;

    private boolean active;

    private String effectiveTime;

    private boolean released;

    private String moduleId;

    private String languageDialectCode;

    private String referencedComponentId;
    private String refsetId;

    private String typeId;

    private String value;

    @Override
    public String getId() {
        return getAnnotationId();
    }

    @Override
    public String getConceptId() {
        return getReferencedComponentId();
    }

    public String getAnnotationId() {
        return annotationId;
    }

    public void setAnnotationId(String annotationId) {
        this.annotationId = annotationId;
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

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public String getLanguageDialectCode() {
        return languageDialectCode;
    }

    public void setLanguageDialectCode(String languageDialectCode) {
        this.languageDialectCode = languageDialectCode;
    }

    public String getReferencedComponentId() {
        return referencedComponentId;
    }

    public void setReferencedComponentId(String referencedComponentId) {
        this.referencedComponentId = referencedComponentId;
    }

    public String getRefsetId() {
        return refsetId;
    }

    public void setRefsetId(String refsetId) {
        this.refsetId = refsetId;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnnotationPojo that)) return false;
        return isActive() == that.isActive() && isReleased() == that.isReleased() && Objects.equals(getAnnotationId(), that.getAnnotationId()) && Objects.equals(getEffectiveTime(), that.getEffectiveTime()) && Objects.equals(getModuleId(), that.getModuleId()) && Objects.equals(getLanguageDialectCode(), that.getLanguageDialectCode()) && Objects.equals(getReferencedComponentId(), that.getReferencedComponentId()) && Objects.equals(getRefsetId(), that.getRefsetId()) && Objects.equals(getTypeId(), that.getTypeId()) && Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAnnotationId(), isActive(), getEffectiveTime(), isReleased(), getModuleId(), getLanguageDialectCode(), getReferencedComponentId(), getRefsetId(), getTypeId(), getValue());
    }

    @Override
    public String toString() {
        return "AnnotationPojo{" +
                "annotationId='" + annotationId + '\'' +
                ", active=" + active +
                ", effectiveTime='" + effectiveTime + '\'' +
                ", released=" + released +
                ", moduleId='" + moduleId + '\'' +
                ", languageDialectCode='" + languageDialectCode + '\'' +
                ", referencedComponentId='" + referencedComponentId + '\'' +
                ", refsetId='" + refsetId + '\'' +
                ", typeId='" + typeId + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
