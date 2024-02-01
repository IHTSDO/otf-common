package org.snomed.module.storage;

import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.script.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.*;

import static org.ihtsdo.otf.RF2Constants.SCTID_CORE_MODULE;

/**
 * Write, read & update RF2 packages and their metadata from either a remote or local filesystem.
 */
public class ModuleStorageCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleStorageCoordinator.class);

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
        ResourceManager resourceManagerCache = new ResourceManager(new ManualResourceConfiguration(false, false, new ResourceConfiguration.Local("cache" + "/" + resourceManagerStorage.getBucketNamePath().orElse("")), null), null);
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
        ResourceManager resourceManagerCache = new ResourceManager(new ManualResourceConfiguration(false, false, new ResourceConfiguration.Local("cache" + "/" + resourceManagerStorage.getBucketNamePath().orElse("")), null), null);
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
        ResourceManager resourceManagerCache = new ResourceManager(new ManualResourceConfiguration(false, false, new ResourceConfiguration.Local("cache" + "/" + resourceManagerStorage.getBucketNamePath().orElse("")), null), null);
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
    public void upload(String codeSystem, String moduleId, String effectiveTime, File rf2Package) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.DuplicateResourceException, ModuleStorageCoordinatorException.OperationFailedException {
        doUploadRelease(codeSystem, moduleId, effectiveTime, rf2Package);
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
    public ModuleMetadata generateMetadata(String codeSystem, String moduleId, String effectiveTime, File rf2Package) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException {
        return doGenerateMetadata(codeSystem, moduleId, effectiveTime, rf2Package);
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
    public void archive(String codeSystem, String moduleId, String effectiveTime) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException {
        doArchiveRelease(codeSystem, moduleId, effectiveTime);
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

    private void doUploadRelease(String codeSystem, String moduleId, String effectiveTime, File rf2Package) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.DuplicateResourceException, ModuleStorageCoordinatorException.OperationFailedException {
        LOGGER.trace("Attempting to upload to location {}_{}/{}", codeSystem, moduleId, effectiveTime);

        // Validate arguments
        throwIfInvalid(codeSystem, moduleId, effectiveTime, rf2Package);

        // Check if metadata already exists
        String metadataResourcePath = getMetadataResourcePath(writeDirectory, codeSystem, moduleId, effectiveTime);
        boolean existingMetadata = resourceManagerStorage.doDoesObjectExist(metadataResourcePath);
        if (existingMetadata) {
            throw new ModuleStorageCoordinatorException.DuplicateResourceException("Metadata already exists at location: " + metadataResourcePath);
        }

        // Check if RF2 package already exists
        String rf2PackageResourcePath = getPackageResourcePath(writeDirectory, codeSystem, moduleId, effectiveTime, rf2Package.getName());
        boolean existingRF2Package = resourceManagerStorage.doDoesObjectExist(rf2PackageResourcePath);
        if (existingRF2Package) {
            throw new ModuleStorageCoordinatorException.DuplicateResourceException("Package already exists at location: " + metadataResourcePath);
        }

        // Build metadata object
        ModuleMetadata moduleMetadata = this.generateMetadata(codeSystem, moduleId, effectiveTime, rf2Package);

        // Write metadata to local temporary file
        File tmpMetadataFile = FileUtils.doCreateTempFile("metadata.json");
        FileUtils.writeToFile(tmpMetadataFile, moduleMetadata);

        // Upload metadata
        resourceManagerStorage.doWriteResource(metadataResourcePath, asFileInputStream(tmpMetadataFile));

        // Check if metadata uploaded
        boolean newMetadata = resourceManagerStorage.doDoesObjectExist(metadataResourcePath);
        if (!newMetadata) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to upload metadata to location: " + metadataResourcePath);
        }

        // Upload RF2 package
        resourceManagerStorage.doWriteResource(rf2PackageResourcePath, asFileInputStream(rf2Package));

        // Check if RF2 package uploaded
        boolean newRF2Package = resourceManagerStorage.doDoesObjectExist(rf2PackageResourcePath);
        if (!newRF2Package) {
            boolean deleteResource = resourceManagerStorage.doDeleteResource(metadataResourcePath);
            if (!deleteResource) {
                LOGGER.trace("Cannot delete previously uploaded metadata; manual clean up required.");
            }

            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to upload package to location: " + rf2PackageResourcePath);
        }
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

    private void throwIfInvalid(String codeSystem, String moduleId, String effectiveTime, File rf2Package) throws ModuleStorageCoordinatorException.InvalidArgumentsException {
        throwIfInvalid(codeSystem, moduleId, effectiveTime);

        if (rf2Package == null) {
            throw new ModuleStorageCoordinatorException.InvalidArgumentsException("EffectiveTime invalid (null)");
        }
    }

    private String getMetadataResourcePath(String directory, String codeSystem, String moduleId, String effectiveTime) {
        return getBaseResourcePath(directory, codeSystem, moduleId, effectiveTime) + "/metadata.json";
    }

    private String getPackageResourcePath(String directory, String codeSystem, String moduleId, String effectiveTime, String rf2PackageFileName) {
        return getBaseResourcePath(directory, codeSystem, moduleId, effectiveTime) + "/" + rf2PackageFileName;
    }

    private FileInputStream asFileInputStream(File file) throws ModuleStorageCoordinatorException.OperationFailedException {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Cannot convert File to FileInputStream.");
        }
    }

    private String getBaseResourcePath(String directory, String codeSystem, String moduleId, String effectiveTime) {
        return directory + "/" + codeSystem + "_" + moduleId + "/" + effectiveTime;
    }

    private ModuleMetadata doGenerateMetadata(String codeSystem, String moduleId, String effectiveTime, File rf2Package) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException {
        LOGGER.trace("Attempting to generate metadata for to location {}_{}/{}", codeSystem, moduleId, effectiveTime);

        // Validate arguments
        throwIfInvalid(codeSystem, moduleId, effectiveTime, rf2Package);

        Set<String> uniqueModuleIds = rf2Service.getUniqueModuleIds(rf2Package);
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

    private Set<ModuleMetadata> getDependencies(File rf2Package, Set<String> excludedModuleIds) throws ModuleStorageCoordinatorException.ResourceNotFoundException {
        Set<RF2Row> rf2Rows = rf2Service.getMDRS(rf2Package);
        Set<String> dependentTargetEffectiveTimes = new HashSet<>();

        Iterator<RF2Row> iterator = rf2Rows.iterator();
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
        if (rf2Rows.isEmpty()) {
            return Collections.emptySet();
        }

        int found = 0;
        for (String readDirectory : readDirectories) {
            Set<String> availableRF2Packages = resourceManagerStorage.doListFilenames(readDirectory, ".zip");
            if (availableRF2Packages.isEmpty()) {
                continue;
            }

            for (RF2Row rf2Row : rf2Rows) {
                if (rf2Row.isFound()) {
                    continue;
                }

                // Filter available RF2 packages on matching effective time
                Set<String> possibleRF2Packages = new HashSet<>();
                for (String availableRF2Package : availableRF2Packages) {
                    String[] resourcePathSegments = availableRF2Package.split("/");
                    String effectiveTime = resourcePathSegments[2];
                    if (dependentTargetEffectiveTimes.contains(effectiveTime)) {
                        possibleRF2Packages.add(availableRF2Package);
                    }
                }

                if (possibleRF2Packages.isEmpty()) {
                    continue;
                }

                for (String possibleRF2PackagePath : possibleRF2Packages) {
                    File possibleRF2Package = resourceManagerStorage.doReadResourceFile(possibleRF2PackagePath);
                    Set<String> uniqueModuleIds = rf2Service.getUniqueModuleIds(possibleRF2Package);
                    boolean owningPackageFound = uniqueModuleIds.contains(rf2Row.getColumn(RF2Service.REFERENCED_COMPONENT_ID));
                    if (owningPackageFound) {
                        rf2Row.setFound(true);
                        found = found + 1;
                        rf2Row.setMetadataResourcePath(asMetadataResourcePath(possibleRF2PackagePath));
                    }
                }
            }

            if (found == rf2Rows.size()) {
                break;
            }
        }

        Set<String> metadataResourcePaths = new HashSet<>();
        for (RF2Row rf2Row : rf2Rows) {
            if (!rf2Row.isFound()) {
                String message = String.format("Cannot generate metadata for %s as dependency cannot be found at location %s", rf2Package.getName(), rf2Row.getMetadataResourcePath());
                throw new ModuleStorageCoordinatorException.ResourceNotFoundException(message);
            } else {
                metadataResourcePaths.add(rf2Row.getMetadataResourcePath());
            }
        }

        Set<ModuleMetadata> moduleMetadatas = new TreeSet<>(Comparator.comparingInt(ModuleMetadata::getEffectiveTime));
        for (String dep : metadataResourcePaths) {
            File file = resourceManagerStorage.doReadResourceFile(dep);
            ModuleMetadata moduleMetadata = FileUtils.convertToObject(file, ModuleMetadata.class);
            moduleMetadatas.add(moduleMetadata);
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
        String[] splits = packageResourcePath.split("/");
        splits = Arrays.copyOf(splits, splits.length - 1); // Remove last segment, i.e. a/b/c/d => a/b/c

        return String.join("/", splits) + "/metadata.json";
    }

    private ModuleMetadata doGetMetadata(String codeSystem, String moduleId, String effectiveTime, boolean includeFile) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException {
        LOGGER.trace("Attempting to download from location {}_{}/{}", codeSystem, moduleId, effectiveTime);

        throwIfInvalid(codeSystem, moduleId, effectiveTime);

        for (String readDirectory : this.readDirectories) {
            String baseResourcePath = getBaseResourcePath(readDirectory, codeSystem, moduleId, effectiveTime);
            String metadataResourcePath = getMetadataResourcePath(readDirectory, codeSystem, moduleId, effectiveTime);
            if (resourceManagerStorage.doDoesObjectExist(metadataResourcePath)) {
                ModuleMetadata moduleMetadata = FileUtils.convertToObject(resourceManagerStorage.doReadResourceFile(metadataResourcePath), ModuleMetadata.class);
                if (moduleMetadata != null) {
                    String rf2ResourcePath = baseResourcePath + "/" + moduleMetadata.getFilename();
                    if (resourceManagerStorage.doDoesObjectExist(rf2ResourcePath)) {
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
                } else {
                    throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to de-serialize metadata.json at location " + metadataResourcePath);
                }
            }
        }

        String message = String.format("Cannot find package for %s_%s/%s", codeSystem, moduleId, effectiveTime);
        throw new ModuleStorageCoordinatorException.ResourceNotFoundException(message);
    }

    private void doGetMetadataFromCacheWithRemoteFallBack(String rf2ResourcePath, ModuleMetadata moduleMetadata) throws ModuleStorageCoordinatorException.OperationFailedException {
        File localRF2Package = resourceManagerCache.doReadResourceFile(rf2ResourcePath);
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
                    success = resourceManagerCache.doWriteResource(rf2ResourcePath, asFileInputStream(remoteRF2Package));
                    if (!success) {
                        LOGGER.trace("Failed to write {} to cache (location: {}).", remoteRF2Package.getPath(), rf2ResourcePath);
                    }
                } else {
                    LOGGER.trace("Failed to delete {} from cache (location:  {}).", remoteRF2Package.getPath(), rf2ResourcePath);
                }
            }
        } else {
            File remoteRF2Package = resourceManagerStorage.doReadResourceFile(rf2ResourcePath);
            moduleMetadata.setFile(remoteRF2Package);

            // Add remote copy to cache
            boolean success = resourceManagerCache.doWriteResource(rf2ResourcePath, asFileInputStream(remoteRF2Package));
            if (!success) {
                LOGGER.trace("Failed to write {} to cache (location: {}).", remoteRF2Package.getPath(), rf2ResourcePath);
            }
        }
    }

    private void doGetMetadataFromRemote(String rf2ResourcePath, ModuleMetadata moduleMetadata) {
        moduleMetadata.setFile(resourceManagerStorage.doReadResourceFile(rf2ResourcePath));
    }

    private void doArchiveRelease(String codeSystem, String moduleId, String effectiveTime) throws ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.OperationFailedException {
        LOGGER.trace("Attempting to archive location {}_{}/{}", codeSystem, moduleId, effectiveTime);

        if (!allowArchive) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Support for archiving disabled");
        }

        // Validate arguments
        throwIfInvalid(codeSystem, moduleId, effectiveTime);

        String metadataResourcePath = getMetadataResourcePath(writeDirectory, codeSystem, moduleId, effectiveTime);
        if (!resourceManagerStorage.doDoesObjectExist(metadataResourcePath)) {
            throw new ModuleStorageCoordinatorException.ResourceNotFoundException("Metadata not found with resource path " + metadataResourcePath);
        }

        ModuleMetadata moduleMetadata = FileUtils.convertToObject(resourceManagerStorage.doReadResourceFile(metadataResourcePath), ModuleMetadata.class);
        if (moduleMetadata == null) { // De-serialisation failed
            throw new ModuleStorageCoordinatorException.ResourceNotFoundException("Malformed Metadata found with resource path " + metadataResourcePath);
        }

        String packageResourcePath = getPackageResourcePath(writeDirectory, codeSystem, moduleId, effectiveTime, moduleMetadata.getFilename());
        if (!resourceManagerStorage.doDoesObjectExist(packageResourcePath)) {
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

    private String asArchivePath(String resourcePath, String epochSecond) {
        // otf-common/files/ABC_12345/20240101/metadata.json => // otf-common/files/ABC_12345/archive/$epochSecond/metadata.json
        return resourcePath.replaceAll("/\\d{8}/", "/archive/" + epochSecond + "/");
    }

    private void doUpdateModuleMetadata(String codeSystem, String moduleId, String effectiveTime, ModuleMetadata moduleMetadata) throws ModuleStorageCoordinatorException.OperationFailedException {
        LOGGER.trace("Attempting to update metadata at location {}_{}/{}", codeSystem, moduleId, effectiveTime);

        // Write new to local temporary file
        File tmpMetadataFile = FileUtils.doCreateTempFile("metadata.json");
        boolean deserialized = FileUtils.writeToFile(tmpMetadataFile, moduleMetadata);
        if (!deserialized) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to write metadata.json");
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
        boolean metadataUploaded = resourceManagerStorage.doWriteResource(resourcePathMetadata, asFileInputStream(tmpMetadataFile));
        if (!metadataUploaded) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to upload metadata to " + resourcePathMetadata);
        }

        // Check if new uploaded
        boolean newMetadata = resourceManagerStorage.doDoesObjectExist(resourcePathMetadata);
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
}
