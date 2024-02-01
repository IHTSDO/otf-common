package org.snomed.module.storage;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class ModuleMetadata {

	private String filename;
	private String codeSystemShortName;
	private String identifyingModuleId;
	private List<String> compositionModuleIds;
	private Integer effectiveTime;
	private Date fileTimeStamp;
	private String fileMD5;
	private Boolean isPublished;
	private Boolean isEdition;
	private List<ModuleMetadata> dependencies;
	private transient File file;

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getCodeSystemShortName() {
		return codeSystemShortName;
	}

	public void setCodeSystemShortName(String codeSystemShortName) {
		this.codeSystemShortName = codeSystemShortName;
	}

	public String getIdentifyingModuleId() {
		return identifyingModuleId;
	}

	public void setIdentifyingModuleId(String identifyingModuleId) {
		this.identifyingModuleId = identifyingModuleId;
	}

	public List<String> getCompositionModuleIds() {
		return compositionModuleIds;
	}

	public void setCompositionModuleIds(List<String> compositionModuleIds) {
		this.compositionModuleIds = compositionModuleIds;
	}

	public Integer getEffectiveTime() {
		return effectiveTime;
	}

	@JsonIgnore
	public String getEffectiveTimeString() {
		if (effectiveTime == null) {
			return null;
		}

		return String.valueOf(effectiveTime);
	}

	public void setEffectiveTime(Integer effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	public Date getFileTimeStamp() {
		return fileTimeStamp;
	}

	public void setFileTimeStamp(Date fileTimeStamp) {
		this.fileTimeStamp = fileTimeStamp;
	}

	public String getFileMD5() {
		return fileMD5;
	}

	public void setFileMD5(String fileMD5) {
		this.fileMD5 = fileMD5;
	}

	public Boolean getPublished() {
		return isPublished;
	}

	public void setPublished(Boolean published) {
		isPublished = published;
	}

	public Boolean getEdition() {
		return isEdition;
	}

	public void setEdition(Boolean edition) {
		isEdition = edition;
	}

	public List<ModuleMetadata> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<ModuleMetadata> dependencies) {
		this.dependencies = dependencies;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ModuleMetadata that = (ModuleMetadata) o;
		return Objects.equals(filename, that.filename) && Objects.equals(codeSystemShortName, that.codeSystemShortName) && Objects.equals(identifyingModuleId, that.identifyingModuleId) && Objects.equals(compositionModuleIds, that.compositionModuleIds) && Objects.equals(effectiveTime, that.effectiveTime) && Objects.equals(fileTimeStamp, that.fileTimeStamp) && Objects.equals(fileMD5, that.fileMD5) && Objects.equals(isPublished, that.isPublished) && Objects.equals(isEdition, that.isEdition) && Objects.equals(dependencies, that.dependencies);
	}

	@Override
	public int hashCode() {
		return Objects.hash(filename, codeSystemShortName, identifyingModuleId, compositionModuleIds, effectiveTime, fileTimeStamp, fileMD5, isPublished, isEdition, dependencies);
	}
}

