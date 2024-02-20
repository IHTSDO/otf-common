package org.snomed.module.storage;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.File;
import java.util.*;

public class ModuleMetadata {

	@JsonIgnore
	public static final String INT = "INT";

	private String filename;
	private String codeSystemShortName;
	private String identifyingModuleId;
	private List<String> compositionModuleIds;
	private Integer effectiveTime;
	private Date fileTimeStamp;
	private String fileMD5;
	private Boolean published;
	private Boolean edition;
	private List<ModuleMetadata> dependencies;

	@JsonIgnore
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
	
	public ModuleMetadata withCodeSystemShortName(String codeSystemShortName) {
		this.codeSystemShortName = codeSystemShortName;
		return this;
	}

	public String getIdentifyingModuleId() {
		return identifyingModuleId;
	}

	public void setIdentifyingModuleId(String identifyingModuleId) {
		this.identifyingModuleId = identifyingModuleId;
	}
	
	public ModuleMetadata withIdentifyingModuleId(String identifyingModuleId) {
		this.identifyingModuleId = identifyingModuleId;
		return this;
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
	
	public ModuleMetadata withEffectiveTime(Integer effectiveTime) {
		this.effectiveTime = effectiveTime;
		return this;
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
		return published;
	}

	public void setPublished(Boolean published) {
		this.published = published;
	}

	public Boolean getEdition() {
		return edition;
	}

	public void setEdition(Boolean edition) {
		this.edition = edition;
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

	public ModuleMetadata withFile(File file) {
		this.file = file;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ModuleMetadata that = (ModuleMetadata) o;
		return Objects.equals(filename, that.filename) && Objects.equals(codeSystemShortName, that.codeSystemShortName) && Objects.equals(identifyingModuleId, that.identifyingModuleId) && Objects.equals(compositionModuleIds, that.compositionModuleIds) && Objects.equals(effectiveTime, that.effectiveTime) && Objects.equals(fileTimeStamp, that.fileTimeStamp) && Objects.equals(fileMD5, that.fileMD5) && Objects.equals(published, that.published) && Objects.equals(edition, that.edition) && Objects.equals(dependencies, that.dependencies);
	}

	@Override
	public int hashCode() {
		return Objects.hash(filename, codeSystemShortName, identifyingModuleId, compositionModuleIds, effectiveTime, fileTimeStamp, fileMD5, published, edition, dependencies);
	}

	@Override
	public String toString() {
		return "ModuleMetadata: " + codeSystemShortName + "_" + identifyingModuleId + "/" + effectiveTime;
	}

	public static void sortByCS(List<ModuleMetadata> metadataList, boolean etDescending) {
		//We need to sort the metadata by code system, then effective time, but INT needs to be first
		//so that it's found first when we're looking for dependencies
		Collections.sort(metadataList, new Comparator<ModuleMetadata>() {
			public int compare(ModuleMetadata mm1, ModuleMetadata mm2) {
				if (mm1.isInt() && !mm2.isInt()) {
					return -1;
				} else if (!mm1.isInt() && mm2.isInt()) {
					return 1;
				} else {
					if (mm1.getCodeSystemShortName().equals(mm2.getCodeSystemShortName())) {
						if (etDescending) {
							return mm1.getEffectiveTime().compareTo(mm2.getEffectiveTime());
						} else {
							return mm2.getEffectiveTime().compareTo(mm1.getEffectiveTime());
						}
					} else {
						return mm1.getCodeSystemShortName().compareTo(mm2.getCodeSystemShortName());
					}
				}
			}
		});
	}

	@JsonIgnore
	public boolean isInt() {
		return this.getCodeSystemShortName().equals(INT);
	}
}

