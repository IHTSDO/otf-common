package org.snomed.module.storage;

public class CurrentPreviousModuleMetadataPair {
	private ModuleMetadata currentRelease;
	private ModuleMetadata previousRelease;

	public CurrentPreviousModuleMetadataPair(ModuleMetadata currentRelease, ModuleMetadata previousRelease) {
		this.currentRelease = currentRelease;
		this.previousRelease = previousRelease;
	}

	public ModuleMetadata getCurrentRelease() {
		return currentRelease;
	}

	public ModuleMetadata getPreviousRelease() {
		return previousRelease;
	}
}
