
package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Metadata {

    @SerializedName("assertionGroupNames")
    @Expose
    private String assertionGroupNames;
    @SerializedName("defaultNamespace")
    @Expose
    private String defaultNamespace;
    @SerializedName("previousRelease")
    @Expose
    private String previousRelease;
    @SerializedName("previousPackage")
    @Expose
    private String previousPackage;
    @SerializedName("dependencyRelease")
    @Expose
    private String dependencyRelease;
    @SerializedName("dependencyPackage")
    @Expose
    private String dependencyPackage;
    @SerializedName("previousDependencyPackage")
    @Expose
    private String previousDependencyPackage;
    @SerializedName("codeSystemShortName")
    @Expose
    private String codeSystemShortName;
    @SerializedName("languageSearch")
    @Expose
    private String languageSearch;
    @SerializedName("defaultModuleId")
    @Expose
    private String defaultModuleId;
    @SerializedName("shortname")
    @Expose
    private String shortname;
    @SerializedName("requiredLanguageRefsets")
    @Expose
    private List<Map<String, String>> requiredLanguageRefsets;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Metadata() {
    }

    /**
     * 
     * @param languageSearch
     * @param assertionGroupNames
     * @param previousRelease
     * @param codeSystemShortName
     * @param defaultModuleId
     * @param defaultNamespace
     * @param shortname
     * @param dependencyRelease
     */
    public Metadata(String assertionGroupNames, String defaultNamespace, String previousRelease, String dependencyRelease, String codeSystemShortName, String languageSearch, String defaultModuleId, String shortname) {
        super();
        this.assertionGroupNames = assertionGroupNames;
        this.defaultNamespace = defaultNamespace;
        this.previousRelease = previousRelease;
        this.dependencyRelease = dependencyRelease;
        this.codeSystemShortName = codeSystemShortName;
        this.languageSearch = languageSearch;
        this.defaultModuleId = defaultModuleId;
        this.shortname = shortname;
    }

    public String getAssertionGroupNames() {
        return assertionGroupNames;
    }

    public void setAssertionGroupNames(String assertionGroupNames) {
        this.assertionGroupNames = assertionGroupNames;
    }

    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    public void setDefaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    public String getPreviousRelease() {
        return previousRelease;
    }

    public void setPreviousRelease(String previousRelease) {
        this.previousRelease = previousRelease;
    }
    
    public String getPreviousPackage() {
        return previousPackage;
    }

    public void setPreviousPackagee(String previousPackage) {
        this.previousPackage = previousPackage;
    }

    public String getDependencyRelease() {
        return dependencyRelease;
    }

    public void setDependencyRelease(String dependencyRelease) {
        this.dependencyRelease = dependencyRelease;
    }
    
    public String getDependencyPackage() {
        return dependencyPackage;
    }

    public void setDependencyPackage(String dependencyPackage) {
        this.dependencyPackage = dependencyPackage;
    }
    
    public String getPreviousDependencyPackage() {
        return previousDependencyPackage;
    }

    public void setPreviousDependencyPackage(String previousDependencyPackage) {
        this.previousDependencyPackage = previousDependencyPackage;
    }

    public String getCodeSystemShortName() {
        return codeSystemShortName;
    }

    public void setCodeSystemShortName(String codeSystemShortName) {
        this.codeSystemShortName = codeSystemShortName;
    }

    public String getLanguageSearch() {
        return languageSearch;
    }

    public void setLanguageSearch(String languageSearch) {
        this.languageSearch = languageSearch;
    }

    public String getDefaultModuleId() {
        return defaultModuleId;
    }

    public void setDefaultModuleId(String defaultModuleId) {
        this.defaultModuleId = defaultModuleId;
    }

    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

	public List<Map<String, String>> getRequiredLanguageRefsets() {
		return requiredLanguageRefsets;
	}

	public void setRequiredLanguageRefsets(List<Map<String, String>> requiredLanguageRefsets) {
		this.requiredLanguageRefsets = requiredLanguageRefsets;
	}
	
	public Map<String, String> getLangLangRefsetMapping() {
		Map<String, String> langLangRefsetMapping = new HashMap<>();
		if (getRequiredLanguageRefsets() != null) {
			for (Map<String, String> langEntry : getRequiredLanguageRefsets()) {
				for (Map.Entry<String, String> langItem : langEntry.entrySet()) {
					if (langItem.getKey().length() == 2) {
						//We're assuming two lettered entries are language codes.  Oh dear.
						langLangRefsetMapping.put(langItem.getKey(), langItem.getValue());
					}
				}
			}
		}
		
		return langLangRefsetMapping;
	}
	
	public String getDefaultLangRefset() {
		if (getRequiredLanguageRefsets() != null) {
			for (Map<String, String> langEntry : getRequiredLanguageRefsets()) {
				if(langEntry.containsKey("default") && langEntry.get("default").equals("true")) {
					return extractLangRefset(langEntry);
				}
			}
		}
		throw new IllegalStateException("Unable to determine default language refset from : " + getRequiredLanguageRefsets());
	}
	
	private String extractLangRefset(Map<String, String> langEntry) {
		for (Map.Entry<String, String> langItem : langEntry.entrySet()) {
			if (langItem.getKey().length() == 2) {
				return langItem.getValue();
			}
		}
		throw new IllegalStateException("Unable to determine lang refset in metadata item: " + langEntry);
	}

}
