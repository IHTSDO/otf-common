package org.snomed.module.storage;

import org.ihtsdo.otf.exception.ScriptException;
import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.script.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.ihtsdo.otf.RF2Constants.SCTID_CORE_MODULE;

/**
 * Write, read & update RF2 packages and their metadata from either a remote or local filesystem.
 */
public class ModuleStorageCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleStorageCoordinator.class);
    public static final String SLASH = "/";
    public static final String CACHE = "cache";

    private final ResourceManager resourceManagerStorage;
    private final ResourceManager resourceManagerCache;
    private final RF2Service rf2Service;
    private final String writeDirectory;
    private final List<String> readDirectories;
    private final boolean allowArchive;

    /**
     * Constructor.
     *
     * @param resourceManagerStorage ResourceManager to use to communicate with either external or local storage system.
     * @param resourceManagerCache   ResourceManager to use to communicate with either external or local cache.
     * @param rf2Service             RF2Service to use to process RF2 package.
     * @param writeDirectory         Directory to write files to.
     * @param readDirectories        Collection of directories to read files from. If a file is not found in one of the read directories,
     *                               then the next read directory will be checked.
     * @param allowArchive           Control whether the archive method supported.
     */
    public ModuleStorageCoordinator(ResourceManager resourceManagerStorage, ResourceManager resourceManagerCache, RF2Service rf2Service, String writeDirectory, List<String> readDirectories, boolean allowArchive) {
        this.resourceManagerStorage = resourceManagerStorage;
        this.resourceManagerCache = resourceManagerCache;
        this.rf2Service = rf2Service;
        this.writeDirectory = writeDirectory;
        this.readDirectories = readDirectories;
        this.allowArchive = allowArchive;

        if (readDirectories.size() == 1 && readDirectories.get(0).contains(",")) {
            throw new IllegalArgumentException("Invalid read directories"); // e.g. List.of("a,b,c") instead of List.of("a", "b", "c")
        }
    }

    /**
     * Constructor.
     *
     * @param resourceManagerStorage ResourceManager to use to communicate with either external or local storage system.
     * @param rf2Service             RF2Service to use to process RF2 package.
     * @param writeDirectory         Directory to write files to.
     * @param readDirectories        Collection of directories to read files from. If a file is not found in one of the read directories,
     *                               then the next read directory will be checked.
     * @param allowArchive           Control whether the archive method supported.
     */
    public ModuleStorageCoordinator(ResourceManager resourceManagerStorage, RF2Service rf2Service, String writeDirectory, List<String> readDirectories, boolean allowArchive) {
        this(resourceManagerStorage, null, rf2Service, writeDirectory, readDirectories, allowArchive);
    }

    /**
     * Instantiate with Dev-environment configuration. This configuration allows for reading files from "dev" and "prod",
     * but only writing to "dev". With this configuration, caching and archiving are enabled.
     *
     * @param resourceManagerStorage ResourceManager to use to communicate with either external or local storage system.
     * @return Instantiated class with Dev-environment configuration.
     */
    public static ModuleStorageCoordinator initDev(ResourceManager resourceManagerStorage) {
        ResourceManager resourceManagerCache = new ResourceManager(new ManualResourceConfiguration(false, false, new ResourceConfiguration.Local(CACHE + SLASH + resourceManagerStorage.getBucketNamePath().orElse("")), null), null);
        return new ModuleStorageCoordinator(resourceManagerStorage, resourceManagerCache, new RF2Service(), "dev", List.of("dev", "prod"), true);
    }

    /**
     * Instantiate with Dev-environment configuration. This configuration allows for reading files from "uat" and "prod",
     * but only writing to "uat". With this configuration, caching and archiving are enabled.
     *
     * @param resourceManagerStorage ResourceManager to use to communicate with either external or local storage system.
     * @return Instantiated class with Uat-environment configuration.
     */
    public static ModuleStorageCoordinator initUat(ResourceManager resourceManagerStorage) {
        ResourceManager resourceManagerCache = new ResourceManager(new ManualResourceConfiguration(false, false, new ResourceConfiguration.Local(CACHE + SLASH + resourceManagerStorage.getBucketNamePath().orElse("")), null), null);
        return new ModuleStorageCoordinator(resourceManagerStorage, resourceManagerCache, new RF2Service(), "uat", List.of("uat", "prod"), true);
    }

    /**
     * Instantiate with Uat-environment configuration. This configuration allows for reading files from "prod" and "prod",
     * but only writing to "prod". With this configuration, caching is enabled and archiving is disabled.
     *
     * @param resourceManagerStorage ResourceManager to use to communicate with either external or local storage system.
     * @return Instantiated class with Prod-environment configuration.
     */
    public static ModuleStorageCoordinator initProd(ResourceManager resourceManagerStorage) {
        ResourceManager resourceManagerCache = new ResourceManager(new ManualResourceConfiguration(false, false, new ResourceConfiguration.Local(CACHE + SLASH + resourceManagerStorage.getBucketNamePath().orElse("")), null), null);
        return new ModuleStorageCoordinator(resourceManagerStorage, resourceManagerCache, new RF2Service(), "prod", List.of("prod"), false);
    }

    /**
     * Upload RF2 package to location computed from given arguments. If no exception has been thrown, the method can be considered successful. To handle specific unsuccessful scenarios, catch exceptions that
     * extend ModuleStorageCoordinatorException, i.e. InvalidArgumentsException. To handle all unsuccessful scenarios, catch the generic ModuleStorageCoordinatorException.
     *
     * @param codeSystem    CodeSystem of RF2 package, e.g. INT or XX.
     * @param moduleId      Most important, or identifying, module id of RF2 package.
     * @param effectiveTime Effective time of RF2 package.
     * @param rf2Package    File to upload.
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException  if any argument is null or empty.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException  if dependency cannot be found for given RF2 package.
     * @throws ModuleStorageCoordinatorException.DuplicateResourceException if resource already exists for computed location
     * @throws ModuleStorageCoordinatorException.OperationFailedException   if any other operation fails, for example, failing to confirm the RF2 package has been uploaded.
     */
    public void upload(String codeSystem, String moduleId, String effectiveTime, File rf2Package) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.DuplicateResourceException, ModuleStorageCoordinatorException.OperationFailedException, ScriptException {
        LOGGER.debug("Attempting to upload to location {}_{}/{}", codeSystem, moduleId, effectiveTime);

        // Validate arguments
        throwIfInvalid(codeSystem, moduleId, effectiveTime, rf2Package);

        // Check if metadata already exists
        String metadataResourcePath = getMetadataResourcePath(writeDirectory, codeSystem, moduleId, effectiveTime);
        boolean existingMetadata = resourceManagerStorage.doesObjectExist(metadataResourcePath);
        if (existingMetadata) {
            throw new ModuleStorageCoordinatorException.DuplicateResourceException("Metadata already exists at location: " + metadataResourcePath);
        }

        // Check if RF2 package already exists
        String rf2PackageResourcePath = getPackageResourcePath(writeDirectory, codeSystem, moduleId, effectiveTime, rf2Package.getName());
        boolean existingRF2Package = resourceManagerStorage.doesObjectExist(rf2PackageResourcePath);
        if (existingRF2Package) {
            throw new ModuleStorageCoordinatorException.DuplicateResourceException("Package already exists at location: " + metadataResourcePath);
        }

        // Build metadata object
        ModuleMetadata moduleMetadata = this.generateMetadata(codeSystem, moduleId, effectiveTime, rf2Package);

        // Write metadata to local temporary file
        File tmpMetadataFile = null;
        try {
            tmpMetadataFile = FileUtils.doCreateTempFile("metadata.json");
            FileUtils.writeToFile(tmpMetadataFile, moduleMetadata);
            // Upload metadata
            resourceManagerStorage.doWriteResource(metadataResourcePath, asFileInputStream(tmpMetadataFile));


            // Check if metadata uploaded
            boolean newMetadata = resourceManagerStorage.doesObjectExist(metadataResourcePath);
            if (!newMetadata) {
                throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to upload metadata to location: " + metadataResourcePath + " reason unknown - no other error reported.");
            }

            // Upload RF2 package
            resourceManagerStorage.doWriteResource(rf2PackageResourcePath, asFileInputStream(rf2Package));
        } catch (IOException e) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to write metadata " + moduleMetadata + " to local temporary file " + tmpMetadataFile, e);
        }

        // Check if RF2 package uploaded
        boolean newRF2Package = resourceManagerStorage.doesObjectExist(rf2PackageResourcePath);
        if (!newRF2Package) {
            boolean deleteResource = resourceManagerStorage.doDeleteResource(metadataResourcePath);
            if (!deleteResource) {
                LOGGER.debug("Cannot delete previously uploaded metadata; manual clean up required.");
            }

            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to upload package to location: " + rf2PackageResourcePath);
        }
    }

    /**
     * Upload RF2 package to location computed from given arguments. If no exception has been thrown, the method can be considered successful. To handle specific unsuccessful scenarios, catch exceptions that
     * extend ModuleStorageCoordinatorException, i.e. InvalidArgumentsException. To handle all unsuccessful scenarios, catch the generic ModuleStorageCoordinatorException.
     *
     * @param codeSystem    CodeSystem of RF2 package, e.g. INT or XX.
     * @param moduleId      Most important, or identifying, module id of RF2 package.
     * @param effectiveTime Effective time of RF2 package.
     * @param rf2Package    File to upload.
     * @param md5File       MD5 file
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException  if any argument is null or empty.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException  if dependency cannot be found for given RF2 package.
     * @throws ModuleStorageCoordinatorException.DuplicateResourceException if resource already exists for computed location
     * @throws ModuleStorageCoordinatorException.OperationFailedException   if any other operation fails, for example, failing to confirm the RF2 package has been uploaded.
     */
    public void upload(String codeSystem, String moduleId, String effectiveTime, File rf2Package, File md5File) throws ScriptException, ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.DuplicateResourceException, IOException {
        this.upload(codeSystem, moduleId, effectiveTime, rf2Package);
        if (md5File != null) {
            // Check if MD5 file already exists
            String md5ResourcePath = getPackageResourcePath(writeDirectory, codeSystem, moduleId, effectiveTime, md5File.getName());
            boolean existingMD5File = resourceManagerStorage.doesObjectExist(md5ResourcePath);
            if (existingMD5File) {
                throw new ModuleStorageCoordinatorException.DuplicateResourceException("MD5 file already exists at location: " + md5ResourcePath);
            }
            // Upload MD5 file
            resourceManagerStorage.doWriteResource(md5ResourcePath, asFileInputStream(md5File));
        }
    }
    
    /**
     * Overload for Generate ModuleMetadata for given RF2 package, allowing a MetaData object to be used as it contains
     * all the values passed in the original method.
     * See {@link #generateMetadata(String, String, String, File)} for more details.
     */
    public ModuleMetadata generateMetadata(ModuleMetadata mm) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException, ScriptException {
    	return generateMetadata(mm.getCodeSystemShortName(),
    							mm.getIdentifyingModuleId(),
    							mm.getEffectiveTimeString(),
    							mm.getFile());
    }

    /**
     * Generate ModuleMetadata for given RF2 package. To handle specific unsuccessful scenarios, catch exceptions that extend ModuleStorageCoordinatorException, i.e. InvalidArgumentsException. To handle all unsuccessful
     * scenarios, catch the generic ModuleStorageCoordinatorException.
     *
     * @param codeSystem    CodeSystem of RF2 package, e.g. INT or XX.
     * @param moduleId      Most important, or identifying, module id of RF2 package.
     * @param effectiveTime Effective time of RF2 package.
     * @param rf2Package    File to upload.
     * @return generated ModuleMetadata for given RF2 package.
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException if any argument is null or empty.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if dependency cannot be found for given RF2 package.
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if any other operation fails, for example, failing to generate MD5 for the given RF2 package.
     */
    public ModuleMetadata generateMetadata(String codeSystem, String moduleId, String effectiveTime, File rf2Package) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException, ScriptException {
        LOGGER.debug("Attempting to generate metadata for location {}_{}/{}", codeSystem, moduleId, effectiveTime);

        // Validate arguments
        throwIfInvalid(codeSystem, moduleId, effectiveTime, rf2Package);

        Set<String> uniqueModuleIds = rf2Service.getUniqueModuleIds(rf2Package, false);
        if (uniqueModuleIds.isEmpty()) {
            String message = String.format("Failed to generate metadata for %s as no composition modules found.", rf2Package.getName());
            throw new ModuleStorageCoordinatorException.OperationFailedException(message);
        }

        List<ModuleMetadata> dependencies = new ArrayList<>(getDependencies(rf2Package, uniqueModuleIds));

        ModuleMetadata moduleMetadata = new ModuleMetadata();
        moduleMetadata.setFilename(rf2Package.getName());
        moduleMetadata.setCodeSystemShortName(codeSystem);
        moduleMetadata.setIdentifyingModuleId(moduleId);
        moduleMetadata.setCompositionModuleIds(new ArrayList<>(uniqueModuleIds));
        moduleMetadata.setEffectiveTime(asInteger(effectiveTime));
        moduleMetadata.setFileTimeStamp(new Date(rf2Package.lastModified()));
        moduleMetadata.setFileMD5(FileUtils.getMD5(rf2Package).orElseThrow(() -> new ModuleStorageCoordinatorException.OperationFailedException("Failed to generate MD5 for " + rf2Package.getName())));
        moduleMetadata.setPublished(false); // Cannot infer from File alone; subsequent manual updates to metadata required.
        moduleMetadata.setEdition(uniqueModuleIds.contains(SCTID_CORE_MODULE));
        moduleMetadata.setDependencies(dependencies);

        return moduleMetadata;
    }

    /**
     * Download ModuleMetadata stored for given arguments. The RF2 package will be included in the response. To handle specific unsuccessful scenarios, catch exceptions that extend
     * ModuleStorageCoordinatorException, i.e. InvalidArgumentsException. To handle all unsuccessful scenarios, catch the generic ModuleStorageCoordinatorException.
     *
     * @param codeSystem    CodeSystem of RF2 package, e.g. INT or XX.
     * @param moduleId      Most important, or identifying, module id of RF2 package.
     * @param effectiveTime Effective time of RF2 package.
     * @return ModuleMetadata stored for given arguments.
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException if any argument is null or empty.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if metadata or package cannot be found for computed location.
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if any other operation fails, for example, failing to de-serialise.
     */
    public ModuleMetadata getMetadata(String codeSystem, String moduleId, String effectiveTime) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException {
        return getMetadata(codeSystem, moduleId, effectiveTime, true);
    }

    /**
     * Download ModuleMetadata stored for given arguments. The RF2 package can be optionally included in the response. To handle specific unsuccessful scenarios, catch exceptions that extend
     * ModuleStorageCoordinatorException, i.e. InvalidArgumentsException. To handle all unsuccessful scenarios, catch the generic ModuleStorageCoordinatorException.
     *
     * @param codeSystem    CodeSystem of RF2 package, e.g. INT or XX.
     * @param moduleId      Most important, or identifying, module id of RF2 package.
     * @param effectiveTime Effective time of RF2 package.
     * @return ModuleMetadata stored for given arguments.
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException if any argument is null or empty.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if metadata or package cannot be found for computed location.
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if any other operation fails, for example, failing to de-serialise.
     */
    public ModuleMetadata getMetadata(String codeSystem, String moduleId, String effectiveTime, boolean includeFile) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException {
        return doGetMetadata(codeSystem, moduleId, effectiveTime, includeFile);
    }

    /**
     * Archive ModuleMetadata stored for given arguments by moving appropriate files to an "archive" subdirectory. To handle specific unsuccessful scenarios, catch exceptions that extend
     * ModuleStorageCoordinatorException, i.e. InvalidArgumentsException. To handle all unsuccessful scenarios, catch the generic ModuleStorageCoordinatorException.
     *
     * @param codeSystem    CodeSystem of RF2 package, e.g. INT or XX.
     * @param moduleId      Most important, or identifying, module id of RF2 package.
     * @param effectiveTime Effective time of RF2 package.
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException if any argument is null or empty.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if metadata or package cannot be found for computed location.
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if any other operation fails, for example, failing to copy a resource from original location to the archive location.
     */
    public void archive(String codeSystem, String moduleId, String effectiveTime) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException, IOException {
        LOGGER.debug("Attempting to archive location {}_{}/{}", codeSystem, moduleId, effectiveTime);

        if (!allowArchive) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Support for archiving disabled");
        }

        // Validate arguments
        throwIfInvalid(codeSystem, moduleId, effectiveTime);

        String metadataResourcePath = getMetadataResourcePath(writeDirectory, codeSystem, moduleId, effectiveTime);
        if (!resourceManagerStorage.doesObjectExist(metadataResourcePath)) {
            throw new ModuleStorageCoordinatorException.ResourceNotFoundException("Metadata not found with resource path " + metadataResourcePath);
        }

        ModuleMetadata moduleMetadata;
        File metedataFile = null;
        try {
            metedataFile = resourceManagerStorage.doReadResourceFile(metadataResourcePath);
            moduleMetadata = FileUtils.convertToObject(metedataFile, ModuleMetadata.class);
        } catch (IOException | ScriptException e) {
            throw new ModuleStorageCoordinatorException.ResourceNotFoundException("Malformed Metadata found with resource path " + metadataResourcePath, e);
        } finally {
            if (metedataFile != null) {
                Files.delete(metedataFile.toPath());
            }
        }

        String packageResourcePath = getPackageResourcePath(writeDirectory, codeSystem, moduleId, effectiveTime, moduleMetadata.getFilename());
        if (!resourceManagerStorage.doesObjectExist(packageResourcePath)) {
            throw new ModuleStorageCoordinatorException.ResourceNotFoundException("Package not found with resource path " + packageResourcePath);
        }

        String epochSecond = Long.toString(Instant.now().getEpochSecond());
        String metadataArchivePath = asArchivePath(metadataResourcePath, epochSecond);
        String packageArchivePath = asArchivePath(packageResourcePath, epochSecond);
        boolean metadataCopied = resourceManagerStorage.doCopyResource(metadataResourcePath, metadataArchivePath);
        if (!metadataCopied) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to copy metadata from " + metadataResourcePath + " to " + metadataArchivePath);
        }

        boolean packageCopied = resourceManagerStorage.doCopyResource(packageResourcePath, packageArchivePath);
        if (!packageCopied) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to copy package from " + packageResourcePath + " to " + packageArchivePath);
        }

        boolean metadataDeleted = resourceManagerStorage.doDeleteResource(metadataResourcePath);
        if (!metadataDeleted) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to metadata package from " + metadataResourcePath);
        }

        boolean packageDeleted = resourceManagerStorage.doDeleteResource(packageResourcePath);
        if (!packageDeleted) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to delete package from " + packageResourcePath);
        }
    }

    /**
     * Download ModuleMetadata stored for given arguments, as well as optionally downloading dependent ModuleMetadata. To handle specific unsuccessful scenarios, catch exceptions that extend
     * ModuleStorageCoordinatorException, i.e. InvalidArgumentsException. To handle all unsuccessful scenarios, catch the generic ModuleStorageCoordinatorException.
     *
     * @param codeSystem          CodeSystem of RF2 package, e.g. INT or XX.
     * @param moduleId            Most important, or identifying, module id of RF2 package.
     * @param effectiveTime       Effective time of RF2 package.
     * @param includeFile         Whether to download RF2 package.
     * @param includeDependencies Whether to download dependent ModuleMetadata.
     * @return Collection of dependent ModuleMetadata, sorted by effective time.
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException if any argument is null or empty.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if given RF2 package cannot be found or if any dependencies cannot be found.
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if any other operation fails, for example, failing to de-serialise.
     */
    public List<ModuleMetadata> getRelease(String codeSystem, String moduleId, String effectiveTime, boolean includeFile, boolean includeDependencies) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException {
        return doGetRelease(codeSystem, moduleId, effectiveTime, includeFile, includeDependencies);
    }

    /**
     * Download ModuleMetadata stored for given arguments, as well as downloading dependent ModuleMetadata. To handle specific unsuccessful scenarios, catch exceptions that extend
     * ModuleStorageCoordinatorException, i.e. InvalidArgumentsException. To handle all unsuccessful scenarios, catch the generic ModuleStorageCoordinatorException.
     *
     * @param codeSystem    CodeSystem of RF2 package, e.g. INT or XX.
     * @param moduleId      Most important, or identifying, module id of RF2 package.
     * @param effectiveTime Effective time of RF2 package.
     * @return Collection of dependent ModuleMetadata, sorted by effective time.
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException if any argument is null or empty.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if given RF2 package cannot be found or if any dependencies cannot be found.
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if any other operation fails, for example, failing to de-serialise.
     */
    public List<ModuleMetadata> getRelease(String codeSystem, String moduleId, String effectiveTime) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException {
        return doGetRelease(codeSystem, moduleId, effectiveTime, true, true);
    }

    /**
     * Update ModuleMetadata stored for given arguments. To handle specific unsuccessful scenarios, catch exceptions that extends
     * ModuleStorageCoordinatorException, i.e. InvalidArgumentsException. To handle all unsuccessful scenarios, catch the generic ModuleStorageCoordinatorException.
     *
     * @param codeSystem    CodeSystem of RF2 package, e.g. INT or XX.
     * @param moduleId      Most important, or identifying, module id of RF2 package.
     * @param effectiveTime Effective time of RF2 package.
     * @param published     The new value of the ModuleMetadata published property.
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException if any argument is null or empty.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if RF2 package cannot be found from computed location.
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if any other operation fails, for example, attempting to archive when flag has been disabled.
     */
    public void setPublished(String codeSystem, String moduleId, String effectiveTime, boolean published) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException {
        if (!allowArchive) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Support for archiving disabled");
        }

        throwIfInvalid(codeSystem, moduleId, effectiveTime);

        ModuleMetadata moduleMetadata = getMetadata(codeSystem, moduleId, effectiveTime, false);
        moduleMetadata.setPublished(published);
        doUpdateModuleMetadata(codeSystem, moduleId, effectiveTime, moduleMetadata);
    }

    /**
     * Update ModuleMetadata stored for given arguments. To handle specific unsuccessful scenarios, catch exceptions that extends
     * ModuleStorageCoordinatorException, i.e. InvalidArgumentsException. To handle all unsuccessful scenarios, catch the generic ModuleStorageCoordinatorException.
     *
     * @param codeSystem    CodeSystem of RF2 package, e.g. INT or XX.
     * @param moduleId      Most important, or identifying, module id of RF2 package.
     * @param effectiveTime Effective time of RF2 package.
     * @param edition       The new value of the ModuleMetadata edition property.
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException if any argument is null or empty.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if RF2 package cannot be found from computed location.
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if any other operation fails, for example, attempting to archive when flag has been disabled.
     */
    public void setEdition(String codeSystem, String moduleId, String effectiveTime, boolean edition) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException {
        if (!allowArchive) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Support for archiving disabled");
        }

        throwIfInvalid(codeSystem, moduleId, effectiveTime);
        ModuleMetadata moduleMetadata = getMetadata(codeSystem, moduleId, effectiveTime, false);
        moduleMetadata.setEdition(edition);
        doUpdateModuleMetadata(codeSystem, moduleId, effectiveTime, moduleMetadata);
    }

    /**
     * Download all ModuleMetadata stored. RF2 package is excluded. To handle specific unsuccessful
     * scenarios, catch exceptions that extends ModuleStorageCoordinatorException, i.e. InvalidArgumentsException. To handle all
     * unsuccessful scenarios, catch the generic ModuleStorageCoordinatorException.
     *
     * @return Collection of all stored ModuleMetadata
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if any other operation fails, for example, de-serialising fails.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if RF2 package cannot be found from metadata.
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException if any argument in resource path is invalid.
     */
    public Map<String, List<ModuleMetadata>> getAllReleases() throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException {
        return doGetAllReleases();
    }

    /**
     * Download all ModuleMetadata stored. RF2 package is excluded. To handle specific unsuccessful
     * scenarios, catch exceptions that extends ModuleStorageCoordinatorException, i.e. InvalidArgumentsException. To handle all
     * unsuccessful scenarios, catch the generic ModuleStorageCoordinatorException. If page & size are greater than 0, the
     * ModuleMetadata will be paged.
     *
     * @param page Page number of ModuleMetadata to return.
     * @param size Page size of ModuleMetadata to return.
     * @return Collection of all stored ModuleMetadata
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if any other operation fails, for example, de-serialising fails.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if RF2 package cannot be found from metadata.
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException if any argument in resource path is invalid.
     */
    public Map<String, List<ModuleMetadata>> getAllReleases(int page, int size) throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException {
        Map<String, List<ModuleMetadata>> releases = getAllReleases();

        boolean paging = page >= 1 && size >= 1;
        if (paging) {
            for (Map.Entry<String, List<ModuleMetadata>> entrySet : releases.entrySet()) {
                List<ModuleMetadata> moduleMetadataList = entrySet.getValue();
                if (moduleMetadataList == null || moduleMetadataList.isEmpty()) {
                    continue;
                }

                entrySet.setValue(subList(moduleMetadataList, page, size));
            }
        }

        return releases;
    }

    /**
     * Download all ModuleMetadata stored for the given CodeSystem. RF2 package is excluded. To handle specific unsuccessful
     * scenarios, catch exceptions that extends ModuleStorageCoordinatorException, i.e. InvalidArgumentsException. To handle all
     * unsuccessful scenarios, catch the generic ModuleStorageCoordinatorException.
     *
     * @param codeSystem CodeSystem of RF2 package to filter by, e.g. INT or XX.
     * @return Collection of all stored ModuleMetadata for the given CodeSystem.
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if any other operation fails, for example, de-serialising fails.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if RF2 package(s) cannot be found for given CodeSystem.
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException if given CodeSystem is invalid.
     */
    public List<ModuleMetadata> getAllReleases(String codeSystem) throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException {
        return doGetAllReleasesByCodeSystem(codeSystem);
    }

    /**
     * Download all stored CodeSystems, i.e. INT or XX. To handle specific unsuccessful scenarios, catch exceptions that extends ModuleStorageCoordinatorException, i.e. InvalidArgumentsException.
     * To handle all unsuccessful scenarios, catch the generic ModuleStorageCoordinatorException.
     *
     * @return Collection of stored CodeSystems.
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if an internal operation fails, for example, de-serialising fails.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if an internal operation fails, for example, RF2 package cannot be found.
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException if an internal operation fails, for example, CodeSystem format is invalid.
     */
    public List<String> getCodeSystems() throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException {
        Map<String, List<ModuleMetadata>> releases = doGetAllReleases();
        if (releases.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> sortedCodeSystems = new ArrayList<>(releases.keySet());
        Collections.sort(sortedCodeSystems);
        return sortedCodeSystems;
    }

    /**
     * Download all release dates for the given CodeSystem, i.e. INT or XX. To handle specific unsuccessful scenarios, catch exceptions that extends ModuleStorageCoordinatorException, i.e. InvalidArgumentsException.
     * To handle all unsuccessful scenarios, catch the generic ModuleStorageCoordinatorException.
     *
     * @param codeSystem CodeSystem to find release dates for.
     * @return Collection of release dates for given CodeSystem.
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if an internal operation fails, for example, de-serialising fails.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if an internal operation fails, for example, RF2 package cannot be found.
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException if an internal operation fails, for example, CodeSystem format is invalid.
     */
    public List<Integer> getReleaseDates(String codeSystem) throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException {
        if (codeSystem == null || codeSystem.isEmpty()) {
            throw new ModuleStorageCoordinatorException.InvalidArgumentsException("CodeSystem invalid (null or empty)");
        }

        return doGetAllReleasesByCodeSystem(codeSystem).stream().map(ModuleMetadata::getEffectiveTime).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
    }

    /**
     * Download all dependencies stored for the given RF2 Package.
     * *
     * @param rf2Package RF2 package file
     * @param rf2DeltaOnly Delta or full release
     * @param includeFile Whether or not the dependency package is downloaded
     * @return Collection of all stored ModuleMetadata for given RF2 package.
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if any other operation fails, for example, de-serialising fails.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if RF2 package(s) cannot be found for given CodeSystem.
     */
    public Set<ModuleMetadata> getRequiredDependencies(File rf2Package, boolean rf2DeltaOnly,  boolean includeFile) throws ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException {
        Set<RF2Row> mdrsRows = rf2Service.getMDRS(rf2Package, rf2DeltaOnly);
        Set<String> uniqueModuleIds = rf2Service.getUniqueModuleIds(rf2Package, rf2DeltaOnly);
        return getRequiredDependencies(mdrsRows, uniqueModuleIds, includeFile);
    }

    /**
     * Download all dependencies stored for the given MDRS records.
     * *
     * @param mdrsRows Collection of MDRS rows
     * @param excludedModuleIds Collection of Module which will be excluded
     * @param includeFile Whether or not the release package is downloaded
     * @return Collection of all stored ModuleMetadata for the MDRS records.
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if any other operation fails, for example, de-serialising fails.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if RF2 package(s) cannot be found for given CodeSystem.
     */
    public Set<ModuleMetadata> getRequiredDependencies(Set<RF2Row> mdrsRows, Set<String> excludedModuleIds, boolean includeFile) throws ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException {
        Set<ModuleMetadata> dependencies = getDependencies(mdrsRows, excludedModuleIds ,null);
        if (!includeFile) return dependencies;
        addFile(dependencies);
        return dependencies;
    }

    /**
     * Return dependencies stored for given MDRS entries.
     *
     * @param mdrsRows    MDRS entries to process.
     * @param includeFile Whether to include RF2 file.
     * @return Dependencies stored for given MDRS entries.
     */
    public Set<ModuleMetadata> getDependencies(Set<RF2Row> mdrsRows, boolean includeFile) {
        if (mdrsRows == null || mdrsRows.isEmpty()) {
            return Collections.emptySet();
        }

        // Collect available rf2 packages
        Set<ModuleMetadata> rf2Packages = getRF2Packages();

        // Remove where MODULE not in MDRS
        rf2Packages = filterByMDRS(rf2Packages, mdrsRows, true);

        // No dependencies when MDRS references single CodeSystem
        if (isSingleCodeSystem(rf2Packages)) {
            return Collections.emptySet();
        }

        // Remove where EFFECTIVE TIME not in MDRS
        rf2Packages = filterByMDRS(rf2Packages, mdrsRows, false);

        // Remove Extensions packaged as Editions
        if (!isSingleCodeSystem(rf2Packages)) {
            // Note, this can remove INT as a dependency when given transitive effectiveTimes,
            // i.e. MDRS references unavailable & unpublished package.
            // Therefore, hidden within previous if-statement
            rf2Packages = filterByExtensionsPackagedAsEditions(rf2Packages);
        }

        // Group by CodeSystem
        Map<String, Set<ModuleMetadata>> byCodeSystem = sortByCodeSystem(rf2Packages);

        // Flatten into single collection with latest or specified version
        Set<ModuleMetadata> moduleMetadata = flattenByLatest(byCodeSystem);

        if (!includeFile) {
            return moduleMetadata;
        }

        addFile(moduleMetadata);
        return moduleMetadata;
    }

    /**
     * Return composition for given MDRS entries.
     *
     * @param mdrsRows    MDRS entries to process.
     * @param includeFile Whether to include RF2 file.
     * @return Composition for given MDRS entries.
     */
    public Set<ModuleMetadata> getComposition(Set<RF2Row> mdrsRows, boolean includeFile) {
        if (mdrsRows == null || mdrsRows.isEmpty()) {
            return Collections.emptySet();
        }

        // Collect available rf2 packages
        Set<ModuleMetadata> rf2Packages = getRF2Packages();

        // Remove those not specified in MDRS
        rf2Packages = filterByMDRS(rf2Packages, mdrsRows, false);

        // Group by CodeSystem
        Map<String, Set<ModuleMetadata>> byCodeSystem = sortByCodeSystem(rf2Packages);

        // Flatten into single collection with latest or specified version
        Set<ModuleMetadata> moduleMetadata = flattenByLatest(byCodeSystem);

        if (!includeFile) {
            return moduleMetadata;
        }

        addFile(moduleMetadata);
        return moduleMetadata;
    }

    private boolean isSingleCodeSystem(Set<ModuleMetadata> moduleMetadata) {
        if (moduleMetadata == null || moduleMetadata.isEmpty()) {
            return true;
        }

        return moduleMetadata.stream().map(ModuleMetadata::getCodeSystemShortName).collect(Collectors.toSet()).size() == 1;
    }

    private Set<ModuleMetadata> filterByExtensionsPackagedAsEditions(Set<ModuleMetadata> rf2Packages) {
        Map<String, List<ModuleMetadata>> byIdentifyingModule = new HashMap<>();
        for (ModuleMetadata rf2Package : rf2Packages) {
            List<ModuleMetadata> value = byIdentifyingModule.get(rf2Package.getIdentifyingModuleId());
            if (value == null) {
                value = new ArrayList<>();
            }
            value.add(rf2Package);
            byIdentifyingModule.put(rf2Package.getIdentifyingModuleId(), value);
        }

        Set<ModuleMetadata> filtered = new HashSet<>();
        for (ModuleMetadata rf2Package : rf2Packages) {
            List<String> compositionModuleIds = rf2Package.getCompositionModuleIds();
            compositionModuleIds.remove(rf2Package.getIdentifyingModuleId());

            for (String compositionModuleId : compositionModuleIds) {
                List<ModuleMetadata> value = byIdentifyingModule.get(compositionModuleId);
                if (value != null) {
                    filtered.addAll(value);
                }
            }
        }

        return filtered;
    }

	private Set<ModuleMetadata> getRF2Packages() {
        Set<ModuleMetadata> rf2Packages = new HashSet<>();
        for (String readDirectory : readDirectories) {
            Set<String> rf2PackagePaths = resourceManagerStorage.doListFilenames(readDirectory, ".zip");
            if (rf2PackagePaths.isEmpty()) {
                return Collections.emptySet();
            }

            for (String rfPackagePath : rf2PackagePaths) {
                ModuleMetadata moduleMetadata = asModuleMetadata(asMetadataResourcePath(rfPackagePath));
                if (moduleMetadata != null && !Objects.equals("SIMPLEX", moduleMetadata.getCodeSystemShortName())) {
                    rf2Packages.add(moduleMetadata);
                }
            }
        }

        return rf2Packages;
    }

    private Set<ModuleMetadata> flattenByLatest(Map<String, Set<ModuleMetadata>> byCodeSystem) {
        Set<ModuleMetadata> dependencies = new HashSet<>();
        for (Map.Entry<String, Set<ModuleMetadata>> entrySet : byCodeSystem.entrySet()) {
            dependencies.add(entrySet.getValue().iterator().next());
        }

        return dependencies;
    }

    private Map<String, Set<ModuleMetadata>> sortByCodeSystem(Set<ModuleMetadata> rf2Packages) {
        Map<String, Set<ModuleMetadata>> versionsByCodeSystem = new HashMap<>();
        for (ModuleMetadata rf2Package : rf2Packages) {
            String key = rf2Package.getCodeSystemShortName();
            Set<ModuleMetadata> value = versionsByCodeSystem.get(key);
            if (value == null) {
                value = new TreeSet<>((o1, o2) -> o2.getEffectiveTime().compareTo(o1.getEffectiveTime()));
            }
            value.add(rf2Package);
            versionsByCodeSystem.put(key, value);
        }

        return versionsByCodeSystem;
    }

    private Set<ModuleMetadata> filterByMDRS(Set<ModuleMetadata> rf2Packages, Set<RF2Row> mdrs, boolean moduleOnly) {
        Set<ModuleMetadata> filtered = new HashSet<>();
        for (ModuleMetadata rf2Package : rf2Packages) {
            for (RF2Row row : mdrs) {
                if (inScope(moduleOnly, rf2Package, row)) {
                    filtered.add(rf2Package);
                }
            }
        }

        return filtered;
    }

    private boolean inScope(boolean moduleOnly, ModuleMetadata rf2Package, RF2Row row) {
        String identifyingModuleId = rf2Package.getIdentifyingModuleId();
        String effectiveTimeString = rf2Package.getEffectiveTimeString();
        String moduleId = row.getColumn(RF2Service.MODULE_ID);
        String referencedComponentId = row.getColumn(RF2Service.REFERENCED_COMPONENT_ID);
        String sourceEffectiveTime = row.getColumn(RF2Service.SOURCE_EFFECTIVE_TIME);
        String targetEffectiveTime = row.getColumn(RF2Service.TARGET_EFFECTIVE_TIME);

        if (moduleOnly) {
            boolean a = Objects.equals(identifyingModuleId, referencedComponentId);
            boolean b = Objects.equals(identifyingModuleId, moduleId);

            return a || b;
        } else {
            boolean a = Objects.equals(identifyingModuleId, referencedComponentId);
            boolean d = targetEffectiveTime.isEmpty() || Objects.equals(effectiveTimeString, targetEffectiveTime);

            boolean b = Objects.equals(identifyingModuleId, moduleId);
            boolean c = sourceEffectiveTime.isEmpty() || Objects.equals(effectiveTimeString, sourceEffectiveTime);

            // Dependency found
            if (a && d) {
                return true;
            }

            // Self found
            if (b && c) {
                return true;
            }
        }

        return false;
    }

    private void addFile(Set<ModuleMetadata> moduleMetadata) {
        try {
            for (ModuleMetadata dependency : moduleMetadata) {
                for (String readDirectory : this.readDirectories) {
                    String baseResourcePath = getBaseResourcePath(readDirectory, dependency.getCodeSystemShortName(), dependency.getIdentifyingModuleId(), dependency.getEffectiveTimeString());
                    String metadataResourcePath = getMetadataResourcePath(readDirectory, dependency.getCodeSystemShortName(), dependency.getIdentifyingModuleId(), dependency.getEffectiveTimeString());

                    if (resourceManagerStorage.doesObjectExist(metadataResourcePath)) {
                        String rf2ResourcePath = baseResourcePath + SLASH + dependency.getFilename();
                        boolean cacheEnabled = resourceManagerCache != null;
                        if (cacheEnabled) {
                            doGetMetadataFromCacheWithRemoteFallBack(rf2ResourcePath, dependency);
                        } else {
                            doGetMetadataFromRemote(rf2ResourcePath, dependency);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private List<ModuleMetadata> doGetAllReleasesByCodeSystem(String codeSystem) throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException {
        if (codeSystem == null || codeSystem.isEmpty()) {
            throw new ModuleStorageCoordinatorException.InvalidArgumentsException("CodeSystem invalid (null or empty)");
        }

        Map<String, List<ModuleMetadata>> releases = doGetAllReleases();
        if (!releases.isEmpty()) {
            List<ModuleMetadata> moduleMetadata = releases.get(codeSystem);
            if (moduleMetadata == null) {
                throw new ModuleStorageCoordinatorException.ResourceNotFoundException("Cannot any find releases for CodeSystem " + codeSystem);
            }

            return moduleMetadata;
        }

        throw new ModuleStorageCoordinatorException.ResourceNotFoundException("Cannot find any releases for CodeSystem " + codeSystem);
    }

    private Map<String, List<ModuleMetadata>> doGetAllReleases() throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException {
        Map<String, List<ModuleMetadata>> releases = new HashMap<>();
        for (String readDirectory : readDirectories) {
            // List all json resource paths in readDirectory
            Set<String> metadataResourcePaths = resourceManagerStorage.doListFilenames(readDirectory, ".json");
            for (String metadataResourcePath : metadataResourcePaths) {
                // Check resource path has expected format, i.e. rogue files are ignored
                boolean isExpectedFormat = isExpectedFormat(metadataResourcePath);
                if (!isExpectedFormat) {
                    LOGGER.debug("Ignoring resource path {}", metadataResourcePath);
                    continue;
                }

                // Parse from resource path
                String codeSystem = parseCodeSystem(metadataResourcePath);
                String moduleId = parseModuleId(metadataResourcePath);
                String effectiveTime = parseEffectiveTime(metadataResourcePath);

                if (codeSystem == null || moduleId == null || effectiveTime == null) {
                    LOGGER.debug("Cannot parse codeSystem, moduleId and effectiveTime from resource path: {}", metadataResourcePath);
                    continue;
                }

                // Has readDirectory been overwritten by a previous readDirectory?
                List<ModuleMetadata> moduleMetadatas = releases.getOrDefault(codeSystem, new ArrayList<>());
                boolean overwritten = moduleMetadatas.stream().anyMatch(metadata -> Objects.equals(metadata.getEffectiveTimeString(), effectiveTime));
                if (overwritten) {
                    continue;
                }

                // Get from local cache or remote
                ModuleMetadata moduleMetadata = doGetMetadata(codeSystem, moduleId, effectiveTime, false);
                moduleMetadatas.add(moduleMetadata);
                releases.put(codeSystem, moduleMetadatas);
            }
        }

        for (Map.Entry<String, List<ModuleMetadata>> entrySet : releases.entrySet()) {
            entrySet.getValue().sort(Comparator.comparing(ModuleMetadata::getEffectiveTime, Comparator.reverseOrder()));
        }

        return releases;
    }

    private String parseCodeSystem(String resourcePath) {
        String[] splitBySlash = resourcePath.split(SLASH);
        if (splitBySlash.length >= 2) {
            String[] splitByUnderscore = splitBySlash[1].split("_");
            if (splitByUnderscore.length >= 2) {
                return splitByUnderscore[0];
            }
        }

        return null;
    }

    private String parseModuleId(String resourcePath) {
        String[] splitBySlash = resourcePath.split(SLASH);
        if (splitBySlash.length >= 2) {
            String[] splitByUnderscore = splitBySlash[1].split("_");
            if (splitByUnderscore.length >= 2) {
                return splitByUnderscore[1];
            }
        }

        return null;
    }

    private String parseEffectiveTime(String resourcePath) {
        String[] splitBySlash = resourcePath.split(SLASH);
        if (splitBySlash.length >= 3) {
            return splitBySlash[2];
        }

        return null;
    }

    private boolean isExpectedFormat(String resourcePath) {
        String regex = "^(dev|uat|prod)/[A-Za-z]+_[0-9]+/\\d{8}/(\\w+\\.zip|metadata\\.json)$";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(resourcePath).find();
    }

    private void throwIfInvalid(String codeSystem, String moduleId, String effectiveTime) throws ModuleStorageCoordinatorException.InvalidArgumentsException {
        if (codeSystem == null || codeSystem.isBlank()) {
            throw new ModuleStorageCoordinatorException.InvalidArgumentsException("CodeSystem invalid (null or empty)");
        }

        if (moduleId == null || moduleId.isBlank()) {
            throw new ModuleStorageCoordinatorException.InvalidArgumentsException("ModuleId invalid (null or empty)");
        }

        if (effectiveTime == null || effectiveTime.isBlank()) {
            throw new ModuleStorageCoordinatorException.InvalidArgumentsException("EffectiveTime invalid (null or empty)");
        }

        boolean expectedFormat = effectiveTime.matches("^\\d{4}\\d{2}\\d{2}$");
        if (!expectedFormat) {
            throw new ModuleStorageCoordinatorException.InvalidArgumentsException("EffectiveTime invalid format: " + effectiveTime);
        }
    }

    private void throwIfInvalid(String codeSystem, String moduleId, String effectiveTime, File rf2Package) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException {
        throwIfInvalid(codeSystem, moduleId, effectiveTime);

        if (rf2Package == null) {
            throw new ModuleStorageCoordinatorException.InvalidArgumentsException("No RF2 package specified");
        }

        if (!rf2Package.exists() || !rf2Package.isFile() || !rf2Package.canRead()) {
            throw new ModuleStorageCoordinatorException.ResourceNotFoundException("Unable to read RF2 package: " + rf2Package.getName());
        }
    }

    private String getMetadataResourcePath(String directory, String codeSystem, String moduleId, String effectiveTime) {
        return getBaseResourcePath(directory, codeSystem, moduleId, effectiveTime) + "/metadata.json";
    }

    private String getPackageResourcePath(String directory, String codeSystem, String moduleId, String effectiveTime, String rf2PackageFileName) {
        return getBaseResourcePath(directory, codeSystem, moduleId, effectiveTime) + SLASH + rf2PackageFileName;
    }

    private FileInputStream asFileInputStream(File file) throws ModuleStorageCoordinatorException.OperationFailedException {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Cannot convert File to FileInputStream.");
        }
    }

    private String getBaseResourcePath(String directory, String codeSystem, String moduleId, String effectiveTime) {
        return directory + SLASH + codeSystem + "_" + moduleId + SLASH + effectiveTime;
    }

    private Set<ModuleMetadata> getDependencies(File rf2Package, Set<String> excludedModuleIds) throws ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException {
        Set<RF2Row> rf2Rows = rf2Service.getMDRS(rf2Package, false);
        return getDependencies(rf2Rows, excludedModuleIds, rf2Package.getName());
    }
    private Set<ModuleMetadata> getDependencies(Set<RF2Row> mdrsRows, Set<String> excludedModuleIds, String rf2Filename) throws ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException {
        Set<String> dependentTargetEffectiveTimes = new HashSet<>();
        Iterator<RF2Row> iterator = mdrsRows.iterator();
        while (iterator.hasNext()) {
            RF2Row rf2Row = iterator.next();
            // Remove if "dependency" module found in own package
            boolean dependencyInOwnPackage = excludedModuleIds.contains(rf2Row.getColumn(RF2Service.REFERENCED_COMPONENT_ID));
            if (dependencyInOwnPackage) {
                iterator.remove();
            } else {
                dependentTargetEffectiveTimes.add(rf2Row.getColumn(RF2Service.TARGET_EFFECTIVE_TIME));
            }
        }

        // No external dependencies
        if (mdrsRows.isEmpty()) {
            return Collections.emptySet();
        }

        int found = 0;
        for (String readDirectory : readDirectories) {
            Set<String> availableRF2Packages = resourceManagerStorage.doListFilenames(readDirectory, ".zip");
            if (availableRF2Packages.isEmpty()) {
                continue;
            }

            for (RF2Row rf2Row : mdrsRows) {
                if (rf2Row.isFound()) {
                    continue;
                }

                // Filter available RF2 packages on matching effective time
                Set<String> possibleRF2Packages = new HashSet<>();
                for (String availableRF2Package : availableRF2Packages) {
                    String[] resourcePathSegments = availableRF2Package.split(SLASH);
                    String effectiveTime = resourcePathSegments[2];
                    if (dependentTargetEffectiveTimes.contains(effectiveTime)) {
                        possibleRF2Packages.add(availableRF2Package);
                    }
                }

                if (possibleRF2Packages.isEmpty()) {
                    continue;
                }

                for (String possibleRF2PackagePath : possibleRF2Packages) {
                    File possibleRF2Package = null;
                    try {
                        possibleRF2Package = resourceManagerStorage.doReadResourceFile(possibleRF2PackagePath);
                        Set<String> uniqueModuleIds = rf2Service.getUniqueModuleIds(possibleRF2Package, false);
                        boolean owningPackageFound = uniqueModuleIds.contains(rf2Row.getColumn(RF2Service.REFERENCED_COMPONENT_ID));
                        if (owningPackageFound) {
                            rf2Row.setFound(true);
                            found = found + 1;
                            rf2Row.setMetadataResourcePath(asMetadataResourcePath(possibleRF2PackagePath));
                        }
                    } catch (IOException e) {
                        throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to read RF2 package " + possibleRF2PackagePath, e);
                    } finally {
                        if (possibleRF2Package != null) {
                            try {
                                Files.delete(possibleRF2Package.toPath());
                            } catch (IOException e) {
                                LOGGER.warn("Failed to delete RF2 package {}", possibleRF2PackagePath, e);
                            }
                        }
                    }
                }
            }

            if (found == mdrsRows.size()) {
                break;
            }
        }

        Set<String> metadataResourcePaths = new HashSet<>();
        for (RF2Row rf2Row : mdrsRows) {
            if (!rf2Row.isFound()) {
                String message = String.format("Cannot generate metadata %s as dependency '%s' cannot be found. Ensure dependent packages are uploaded first.", (rf2Filename == null ? "" : "for " + rf2Filename), rf2Row.getColumn(RF2Service.REFERENCED_COMPONENT_ID));
                throw new ModuleStorageCoordinatorException.ResourceNotFoundException(message);
            } else {
                metadataResourcePaths.add(rf2Row.getMetadataResourcePath());
            }
        }

        Set<ModuleMetadata> moduleMetadatas = new TreeSet<>(Comparator.comparingInt(ModuleMetadata::getEffectiveTime));
        for (String dep : metadataResourcePaths) {
            try {
                File file = resourceManagerStorage.doReadResourceFile(dep);
                ModuleMetadata moduleMetadata = FileUtils.convertToObject(file, ModuleMetadata.class);
                moduleMetadatas.add(moduleMetadata);
            } catch (IOException | ScriptException e) {
                throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to de-serialize metadata.json at location " + dep, e);
            }
        }

        return moduleMetadatas;
    }

    private Integer asInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

    private String asMetadataResourcePath(String packageResourcePath) {
        String[] splits = packageResourcePath.split(SLASH);
        splits = Arrays.copyOf(splits, splits.length - 1); // Remove last segment, i.e. a/b/c/d => a/b/c

        return String.join(SLASH, splits) + "/metadata.json";
    }

    private ModuleMetadata doGetMetadata(String codeSystem, String moduleId, String effectiveTime, boolean includeFile) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException {
        LOGGER.debug("Attempting to download from location {}_{}/{}", codeSystem, moduleId, effectiveTime);

        throwIfInvalid(codeSystem, moduleId, effectiveTime);

        for (String readDirectory : this.readDirectories) {
            String baseResourcePath = getBaseResourcePath(readDirectory, codeSystem, moduleId, effectiveTime);
            String metadataResourcePath = getMetadataResourcePath(readDirectory, codeSystem, moduleId, effectiveTime);
            if (resourceManagerStorage.doesObjectExist(metadataResourcePath)) {
                try {
                    ModuleMetadata moduleMetadata = FileUtils.convertToObject(resourceManagerStorage.readResourceStream(metadataResourcePath), ModuleMetadata.class);
                    String rf2ResourcePath = baseResourcePath + SLASH + moduleMetadata.getFilename();
                    if (resourceManagerStorage.doesObjectExist(rf2ResourcePath)) {
                        if (includeFile) {
                            boolean cacheEnabled = resourceManagerCache != null;
                            if (cacheEnabled) {
                                doGetMetadataFromCacheWithRemoteFallBack(rf2ResourcePath, moduleMetadata);
                            } else {
                                doGetMetadataFromRemote(rf2ResourcePath, moduleMetadata);
                            }
                        }
                        return moduleMetadata;
                    }
                } catch (ScriptException | IOException e) {
                    throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to de-serialize metadata.json at location " + metadataResourcePath, e);
                }
            }
        }

        String message = String.format("Cannot find package for %s_%s/%s", codeSystem, moduleId, effectiveTime);
        throw new ModuleStorageCoordinatorException.ResourceNotFoundException(message);
    }

    private void doGetMetadataFromCacheWithRemoteFallBack(String rf2ResourcePath, ModuleMetadata moduleMetadata) throws ModuleStorageCoordinatorException.OperationFailedException {
        try {
            File localRF2Package = null;
            try {
                localRF2Package =  resourceManagerCache.doReadResourceFile(rf2ResourcePath);
            } catch (FileNotFoundException e) {
                //This is expected if the file is not in the cache
            }

            if (localRF2Package != null) {
                boolean localMD5MatchesRemote = Objects.equals(FileUtils.getMD5(localRF2Package).orElse(null), moduleMetadata.getFileMD5());
                if (localMD5MatchesRemote) {
                    // Local copy is same as remote; use local
                    moduleMetadata.setFile(localRF2Package);
                } else {
                    // Local copy differs from remote; use remote
                    File remoteRF2Package = resourceManagerStorage.doReadResourceFile(rf2ResourcePath);
                    moduleMetadata.setFile(remoteRF2Package);

                    // Attempt to replace local copy with remote
                    boolean success = resourceManagerCache.doDeleteResource(rf2ResourcePath);
                    if (success) {
                       resourceManagerCache.doWriteResource(rf2ResourcePath, asFileInputStream(remoteRF2Package));
                    } else {
                        LOGGER.debug("Failed to delete {} from cache (location:  {}).", remoteRF2Package.getPath(), rf2ResourcePath);
                    }
                }
            } else {
                File remoteRF2Package = resourceManagerStorage.doReadResourceFile(rf2ResourcePath);
                moduleMetadata.setFile(remoteRF2Package);

                // Add remote copy to cache
                resourceManagerCache.doWriteResource(rf2ResourcePath, asFileInputStream(remoteRF2Package));
            }
        } catch (ScriptException | IOException e) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to get metadata from cache " + rf2ResourcePath + " with remote fallback", e);
        }
    }

    private void doGetMetadataFromRemote(String rf2ResourcePath, ModuleMetadata moduleMetadata) throws ModuleStorageCoordinatorException.OperationFailedException {
        try {
            moduleMetadata.setFile(resourceManagerStorage.doReadResourceFile(rf2ResourcePath));
        } catch (IOException e) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to get metadata from remote " + rf2ResourcePath, e);
        }
    }

    private String asArchivePath(String resourcePath, String epochSecond) {
        // otf-common/files/ABC_12345/20240101/metadata.json => // otf-common/files/ABC_12345/archive/$epochSecond/metadata.json
        return resourcePath.replaceAll("/\\d{8}/", "/archive/" + epochSecond + SLASH);
    }

    private void doUpdateModuleMetadata(String codeSystem, String moduleId, String effectiveTime, ModuleMetadata moduleMetadata) throws ModuleStorageCoordinatorException.OperationFailedException {
        LOGGER.debug("Attempting to update metadata at location {}_{}/{}", codeSystem, moduleId, effectiveTime);
        File tmpMetadataFile = null;
        try {
            // Write new to local temporary file
            tmpMetadataFile = FileUtils.doCreateTempFile("metadata.json");
            FileUtils.writeToFile(tmpMetadataFile, moduleMetadata);
        } catch (IOException | ScriptException e) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to write metadata.json", e);
        }

        // Copy old to archive
        String resourcePathMetadata = getMetadataResourcePath(writeDirectory, codeSystem, moduleId, effectiveTime);
        String epochSecond = Long.toString(Instant.now().getEpochSecond());
        String metadataArchivePath = asArchivePath(resourcePathMetadata, epochSecond);
        boolean metadataCopied = resourceManagerStorage.doCopyResource(resourcePathMetadata, metadataArchivePath);
        if (!metadataCopied) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to copy metadata from " + resourcePathMetadata + " to " + metadataArchivePath);
        }

        // Delete old
        boolean metadataDeleted = resourceManagerStorage.doDeleteResource(resourcePathMetadata);
        if (!metadataDeleted) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to delete metadata from " + resourcePathMetadata);
        }

        // Upload new
        try {
            resourceManagerStorage.doWriteResource(resourcePathMetadata, asFileInputStream(tmpMetadataFile));
        } catch (IOException e) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to upload metadata to " + resourcePathMetadata, e);
        }

        // Check if new uploaded
        boolean newMetadata = resourceManagerStorage.doesObjectExist(resourcePathMetadata);
        if (!newMetadata) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to upload metadata to " + resourcePathMetadata);
        }
    }

    private List<ModuleMetadata> doGetRelease(String codeSystem, String moduleId, String effectiveTime, boolean includeFile, boolean includeDependencies) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException {
        throwIfInvalid(codeSystem, moduleId, effectiveTime);

        Set<ModuleMetadata> moduleMetadata = new LinkedHashSet<>();
        appendModuleMetadataRecursive(codeSystem, moduleId, effectiveTime, includeFile, includeDependencies, moduleMetadata);
        return new ArrayList<>(moduleMetadata);
    }

    private void appendModuleMetadataRecursive(String codeSystem, String moduleId, String effectiveTime, boolean includeFile, boolean includeDependencies, Set<ModuleMetadata> moduleMetadatas) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException {
        ModuleMetadata moduleMetadata = getMetadata(codeSystem, moduleId, effectiveTime, includeFile);

        if (includeDependencies) {
            for (ModuleMetadata dependency : moduleMetadata.getDependencies()) {
                appendModuleMetadataRecursive(dependency.getCodeSystemShortName(), dependency.getIdentifyingModuleId(), dependency.getEffectiveTimeString(), includeFile, includeDependencies, moduleMetadatas);
            }
        }

        moduleMetadatas.add(moduleMetadata);
    }

    private List<ModuleMetadata> subList(List<ModuleMetadata> list, int page, int size) {
        if (size <= 0 || page <= 0) {
            return list;
        }

        int fromIndex = (page - 1) * size;
        if (list == null || list.isEmpty() || list.size() <= fromIndex) {
            return Collections.emptyList();
        }

        int toIndex = (fromIndex == 0 ? size : fromIndex * size);
        return list.subList(fromIndex, Math.min(toIndex, list.size()));
    }

    private ModuleMetadata asModuleMetadata(String metadataResourcePath) {
        try {
            return FileUtils.convertToObject(resourceManagerStorage.readResourceStream(metadataResourcePath), ModuleMetadata.class);
        } catch (Exception e) {
            return null;
        }
    }
}
