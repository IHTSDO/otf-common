
package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.ihtsdo.otf.utils.StringUtils;

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
    
    @SerializedName("optionalLanguageRefsets")
    @Expose
    private List<Map<String, String>> optionalLanguageRefsets;
    
    @SerializedName("expectedExtensionModules")
    @Expose
    private List<String> expectedExtensionModules;

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
    
	public List<Map<String, String>> getRequiredLanguageRefsets(boolean createIfRequired) {
		if (requiredLanguageRefsets == null && createIfRequired) {
			requiredLanguageRefsets = new ArrayList<>();
		}
		return getRequiredLanguageRefsets();
	}

	public List<Map<String, String>> getRequiredLanguageRefsets() {
		if (requiredLanguageRefsets == null) {
			throw new IllegalStateException("Metadata element 'requiredLanguageRefsets' has not been populated");
		}
		return requiredLanguageRefsets;
	}

	public void setRequiredLanguageRefsets(List<Map<String, String>> requiredLanguageRefsets) {
		this.requiredLanguageRefsets = requiredLanguageRefsets;
	}
	
	public List<Map<String, String>> getOptionalLanguageRefsets() {
		return optionalLanguageRefsets;
	}

	public void setOptionalLanguageRefsets(List<Map<String, String>> optionalLanguageRefsets) {
		this.optionalLanguageRefsets = optionalLanguageRefsets;
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
	
	public Map<String, String> getLangRefsetLangMapping() {
		Map<String, String> langRefsetLangMapping = new HashMap<>();
		if (getRequiredLanguageRefsets() != null) {
			for (Map<String, String> langEntry : getRequiredLanguageRefsets()) {
				for (Map.Entry<String, String> langItem : langEntry.entrySet()) {
					if (langItem.getKey().length() == 2) {
						//We're assuming two lettered entries are language codes.  Oh dear.
						langRefsetLangMapping.put(langItem.getValue(), langItem.getKey());
					}
				}
			}
		}
		
		//The optional LangRefsets however, are also unreliable
		if (getOptionalLanguageRefsets() != null) {
			for (Map<String, String> langEntry : getOptionalLanguageRefsets()) {
				langRefsetLangMapping.put(langEntry.get("refsetId"), normalizeDialectToLanguage(langEntry.get("language")));
			}
		}
		
		return langRefsetLangMapping;
	}

	private String normalizeDialectToLanguage(String dialect) {
		if (StringUtils.isEmpty(dialect)) {
			return null;
		}
		return dialect.substring(0, 2);
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

	public List<String> getExpectedExtensionModules() {
		return expectedExtensionModules;
	}

	public void setExpectedExtensionModules(List<String> expectedExtensionModules) {
		this.expectedExtensionModules = expectedExtensionModules;
	}

}
