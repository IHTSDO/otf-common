package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotationPojo extends RefsetMemberPojo implements SnomedComponent {
    private String annotationId;

    private String languageDialectCode;

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


    public String getLanguageDialectCode() {
        return languageDialectCode;
    }

    public void setLanguageDialectCode(String languageDialectCode) {
        this.languageDialectCode = languageDialectCode;
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
        return isActive() == that.isActive() && getReleased() == that.getReleased() && Objects.equals(getAnnotationId(), that.getAnnotationId()) && Objects.equals(getEffectiveTime(), that.getEffectiveTime()) && Objects.equals(getModuleId(), that.getModuleId()) && Objects.equals(getLanguageDialectCode(), that.getLanguageDialectCode()) && Objects.equals(getReferencedComponentId(), that.getReferencedComponentId()) && Objects.equals(getRefsetId(), that.getRefsetId()) && Objects.equals(getTypeId(), that.getTypeId()) && Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAnnotationId(), isActive(), getEffectiveTime(), getReleased(), getModuleId(), getLanguageDialectCode(), getReferencedComponentId(), getRefsetId(), getTypeId(), getValue());
    }

    @Override
    public String toString() {
        return "AnnotationPojo{" +
                "annotationId='" + annotationId + '\'' +
                ", active=" + isActive() +
                ", effectiveTime='" + getEffectiveTime() + '\'' +
                ", released=" + getReleased() +
                ", moduleId='" + getModuleId() + '\'' +
                ", languageDialectCode='" + languageDialectCode + '\'' +
                ", referencedComponentId='" + getReferencedComponentId() + '\'' +
                ", refsetId='" + getRefsetId() + '\'' +
                ", typeId='" + typeId + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
