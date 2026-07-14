package org.snomed.module.storage;

import org.ihtsdo.otf.exception.ScriptException;
import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.Environment;
import org.snomed.otf.script.utils.FileUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.ihtsdo.otf.RF2Constants.SCTID_CORE_MODULE;
import static org.ihtsdo.otf.RF2Constants.SCTID_MODEL_MODULE;
import static org.snomed.module.storage.ModuleMetadataFilterer.*;

/**
 * Write, read & update RF2 packages and their metadata from either a remote or local filesystem.
 */
public class ModuleStorageCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleStorageCoordinator.class);
    public static final String SLASH = "/";
    public static final String CACHE = "cache";
    public static final String ADDITIONAL_DELIVERABLES = "deliverables";

    private final ResourceManager remoteStorageManager;
    private final ResourceManager localStorageManager;
    private final RF2Service rf2Service;
    private final String writeDirectory;
    private final List<String> readDirectories;
    private final boolean allowArchive;

    public enum SearchRequirement {ALLOW_NONE_FOUND, ENSURE_ONE_FOUND, ENSURE_ALL_FOUND}

    private static final List<String> ROGUE_PACKAGES = List.of("SIMPLEX_900000000000012004");

    /**
     * Constructor.
     *
     * @param remoteStorageManager ResourceManager to use to communicate with either external or local storage system.
     * @param localStorageManager   ResourceManager to use to communicate with either external or local cache.
     * @param rf2Service             RF2Service to use to process RF2 package.
     * @param writeDirectory         Directory to write files to.
     * @param readDirectories        Collection of directories to read files from. If a file is not found in one of the read directories,
     *                               then the next read directory will be checked.
     * @param allowArchive           Control whether the archive method supported.
     */
    public ModuleStorageCoordinator(ResourceManager remoteStorageManager, ResourceManager localStorageManager, RF2Service rf2Service, String writeDirectory, List<String> readDirectories, boolean allowArchive) {
        this.remoteStorageManager = remoteStorageManager;
        this.localStorageManager = localStorageManager;
        this.rf2Service = rf2Service;
        this.writeDirectory = writeDirectory;
        this.readDirectories = readDirectories;
        this.allowArchive = allowArchive;

        if (readDirectories.size() == 1 && readDirectories.getFirst().contains(",")) {
            throw new IllegalArgumentException("Invalid read directories"); // e.g. List.of("a,b,c") instead of List.of("a", "b", "c")
        }
    }

    /**
     * Constructor.
     *
     * @param remoteStorageManager ResourceManager to use to communicate with either external or local storage system.
     * @param rf2Service             RF2Service to use to process RF2 package.
     * @param writeDirectory         Directory to write files to.
     * @param readDirectories        Collection of directories to read files from. If a file is not found in one of the read directories,
     *                               then the next read directory will be checked.
     * @param allowArchive           Control whether the archive method supported.
     */
    public ModuleStorageCoordinator(ResourceManager remoteStorageManager, RF2Service rf2Service, String writeDirectory, List<String> readDirectories, boolean allowArchive) {
        this(remoteStorageManager, null, rf2Service, writeDirectory, readDirectories, allowArchive);
    }

    public ModuleStorageCoordinator(Environment env, ResourceManager remoteStorageManager, ResourceManager localStorageManager, RF2Service rf2Service, boolean allowArchive) {
        String writeDirectory = env.getEnvironmentName();
        List<String> readDirectories = env.getReadFallback();
        this(remoteStorageManager, localStorageManager, rf2Service, writeDirectory, readDirectories, allowArchive);
    }

    /**
     * Instantiate with environment passed in.
     *
     * @param remoteStorageManager ResourceManager to use to communicate with either external or local storage system.
     * @return Instantiated class with Dev-environment configuration.
     */
    public static ModuleStorageCoordinator create(Environment env, ResourceManager remoteStorageManager) {
        ResourceConfiguration.Local localConfig = new ResourceConfiguration.Local(remoteStorageManager.getCachePath());
        ResourceManager localStorageManager = new ResourceManager(new ManualResourceConfiguration(false, false, localConfig, null), null);
        return new ModuleStorageCoordinator(env, remoteStorageManager, localStorageManager, new RF2Service(), true);
    }

    /**
     * Instantiate with Dev-environment configuration. This configuration allows for reading files from "dev" and "prod",
     * but only writing to "dev". With this configuration, caching and archiving are enabled.
     *
     * @param remoteStorageManager ResourceManager to use to communicate with either external or local storage system.
     * @return Instantiated class with Dev-environment configuration.
     */
    public static ModuleStorageCoordinator initDev(ResourceManager remoteStorageManager) {
        ResourceManager localStorageManager = new ResourceManager(new ManualResourceConfiguration(false, false, new ResourceConfiguration.Local(CACHE + SLASH + remoteStorageManager.getBucketNamePath().orElse("")), null), null);
        return new ModuleStorageCoordinator(remoteStorageManager, localStorageManager, new RF2Service(), "dev", List.of("dev", "prod"), true);
    }

    /**
     * Instantiate with Dev-environment configuration. This configuration allows for reading files from "uat" and "prod",
     * but only writing to "uat". With this configuration, caching and archiving are enabled.
     *
     * @param remoteStorageManager ResourceManager to use to communicate with either external or local storage system.
     * @return Instantiated class with Uat-environment configuration.
     */
    public static ModuleStorageCoordinator initUat(ResourceManager remoteStorageManager) {
        ResourceManager localStorageManager = new ResourceManager(new ManualResourceConfiguration(false, false, new ResourceConfiguration.Local(CACHE + SLASH + remoteStorageManager.getBucketNamePath().orElse("")), null), null);
        return new ModuleStorageCoordinator(remoteStorageManager, localStorageManager, new RF2Service(), "uat", List.of("uat", "prod"), true);
    }

    /**
     * Instantiate with Uat-environment configuration. This configuration allows for reading files from "prod" and "prod",
     * but only writing to "prod". With this configuration, caching is enabled and archiving is disabled.
     *
     * @param remoteStorageManager ResourceManager to use to communicate with either external or local storage system.
     * @return Instantiated class with Prod-environment configuration.
     */
    public static ModuleStorageCoordinator initProd(ResourceManager remoteStorageManager) {
        ResourceManager localStorageManager = new ResourceManager(new ManualResourceConfiguration(false, false, new ResourceConfiguration.Local(CACHE + SLASH + remoteStorageManager.getBucketNamePath().orElse("")), null), null);
        return new ModuleStorageCoordinator(remoteStorageManager, localStorageManager, new RF2Service(), "prod", List.of("prod"), false);
    }

    /**
     * Upload RF2 package to a location computed from given arguments. If no exception has been thrown, the method can be considered successful. To handle specific unsuccessful scenarios, catch exceptions that
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
        String baseResourcePath = getBaseResourcePath(writeDirectory, codeSystem, moduleId, effectiveTime);
        String metadataResourcePath = getMetadataResourcePath(baseResourcePath);
        boolean existingMetadata = remoteStorageManager.doesObjectExist(metadataResourcePath);
        if (existingMetadata) {
            throw new ModuleStorageCoordinatorException.DuplicateResourceException("Metadata already exists at location: " + metadataResourcePath);
        }

        // Check if an RF2 package already exists
        String rf2PackageResourcePath = getPackageResourcePath(baseResourcePath, rf2Package.getName());
        boolean existingRF2Package = remoteStorageManager.doesObjectExist(rf2PackageResourcePath);
        if (existingRF2Package) {
            throw new ModuleStorageCoordinatorException.DuplicateResourceException("Package already exists at location: " + metadataResourcePath);
        }

        // Check if an additional deliverables folder already exists
        String additionalResourcesPath = getAdditionalResourcesPath(baseResourcePath);
        boolean existingAdditionalResources = remoteStorageManager.doesObjectExist(additionalResourcesPath);
        if (existingAdditionalResources) {
            throw new ModuleStorageCoordinatorException.DuplicateResourceException("Additional deliverables already exists at location: " + metadataResourcePath);
        }

        // Build metadata object
        ModuleMetadata moduleMetadata = this.generateMetadata(codeSystem, moduleId, effectiveTime, rf2Package);

        // Write metadata to local temporary file
        File tmpMetadataFile = null;
        try {
            tmpMetadataFile = FileUtils.doCreateTempFile("metadata.json");
            FileUtils.writeToFile(tmpMetadataFile, moduleMetadata);
            // Upload metadata
            remoteStorageManager.doWriteResource(metadataResourcePath, asFileInputStream(tmpMetadataFile));


            // Check if metadata uploaded
            boolean newMetadata = remoteStorageManager.doesObjectExist(metadataResourcePath);
            if (!newMetadata) {
                throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to upload metadata to location: " + metadataResourcePath + " reason unknown - no other error reported.");
            }

            // Upload RF2 package
            remoteStorageManager.doWriteResource(rf2PackageResourcePath, asFileInputStream(rf2Package));
        } catch (IOException e) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to write metadata " + moduleMetadata + " to local temporary file " + tmpMetadataFile, e);
        }

        // Check if RF2 package uploaded
        boolean newRF2Package = remoteStorageManager.doesObjectExist(rf2PackageResourcePath);
        if (!newRF2Package) {
            boolean deleteResource = remoteStorageManager.doDeleteResource(metadataResourcePath);
            if (!deleteResource) {
                LOGGER.debug("Cannot delete previously uploaded metadata; manual clean up required.");
            }

            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to upload package to location: " + rf2PackageResourcePath);
        }

        // Create blank additional resources folder
        remoteStorageManager.writeFolder(additionalResourcesPath);
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
            String baseResourcePath = getBaseResourcePath(writeDirectory, codeSystem, moduleId, effectiveTime);
            String md5ResourcePath = getPackageResourcePath(baseResourcePath, md5File.getName());
            boolean existingMD5File = remoteStorageManager.doesObjectExist(md5ResourcePath);
            if (existingMD5File) {
                throw new ModuleStorageCoordinatorException.DuplicateResourceException("MD5 file already exists at location: " + md5ResourcePath);
            }
            // Upload MD5 file
            remoteStorageManager.doWriteResource(md5ResourcePath, asFileInputStream(md5File));
        }
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
        LOGGER.debug("Generating metadata for location {}_{}/{}", codeSystem, moduleId, effectiveTime);

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
     * @param codeSystem    CodeSystem of RF2 package, e.g. INT or XX.  Hopefully just there for debug
     * @param moduleId      Most important, or identifying, module id of RF2 package.
     * @param effectiveTime Effective time of RF2 package.
     * @return ModuleMetadata stored for given arguments.
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException if any argument is null or empty.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if metadata or package cannot be found for computed location.
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if any other operation fails, for example, failing to de-serialise.
     */
    public ModuleMetadata getMetadata(String codeSystem, String moduleId, String effectiveTime) throws ModuleStorageCoordinatorException {
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
    public ModuleMetadata getMetadata(String codeSystem, String moduleId, String effectiveTime, boolean includeFile) throws ModuleStorageCoordinatorException {
        return doGetMetadata(codeSystem, moduleId, effectiveTime, includeFile);
    }

    /**
     * Download ModuleMetadata for each URI in the list. Each URI must be of the form
     * {@code http://snomed.info/sct/<moduleId>} (returns the latest version from the primary read directory)
     * or {@code http://snomed.info/sct/<moduleId>/version/<effectiveTime>} (searches all read directories).
     * The directory listing is performed once per read directory, not once per URI.
     *
     * @param releaseURIs List of SNOMED module URIs.
     * @return List of ModuleMetadata, one per URI, in the same order.
     * @throws ModuleStorageCoordinatorException.InvalidArgumentsException if any URI is null or unrecognised.
     * @throws ModuleStorageCoordinatorException.ResourceNotFoundException if no matching package can be found for a URI.
     * @throws ModuleStorageCoordinatorException.OperationFailedException  if de-serialisation fails.
     */
    public List<ModuleMetadata> getMetadata(List<URI> releaseURIs, SearchRequirement searchRequirement, boolean obtainFilesLocally) throws ModuleStorageCoordinatorException {
        if (releaseURIs == null || releaseURIs.isEmpty()) {
            return Collections.emptyList();
        }

        // List each read directory once upfront; use a LinkedHashMap to preserve priority order
        Map<String, Set<String>> pathsByDirectory = new LinkedHashMap<>();
        for (String readDirectory : readDirectories) {
            pathsByDirectory.put(readDirectory, remoteStorageManager.doListFilenames(readDirectory, "metadata.json"));
        }

        //Let's move any Core URI to be first in the list, so we're not continually searching for model and ICD modules
        //which are contained within the package identified with the core module id
        makeCoreURIsFirst(releaseURIs);

        List<ModuleMetadata> results = new ArrayList<>();
        //URIs with versions specified are expected to exist.  But they might be compositions of other packages
        //So, we can't know until we've checked all of them
        Set<URI> modulesAccountedFor = new HashSet<>();
        for (URI uri : releaseURIs) {
            if (modulesAccountedFor.contains(uri)) {
                continue;
            }
            ModuleMetadata metadata = obtainPopulatedMetadata(uri, pathsByDirectory, obtainFilesLocally);
            if (metadata != null) {
                //Now any composition of this package is effectively found also, so we don't need
                //to look for it separately
                modulesAccountedFor.addAll(MSCUtils.getURIsContained(metadata));
                results.add(metadata);
            }
        }

        //Can't do a count here because - eg with the ICD-10 module - we might find more modules
        //than we're looking for due to additional items in the composition
        if (searchRequirement == SearchRequirement.ENSURE_ALL_FOUND && failedToFindAllModules(releaseURIs, modulesAccountedFor)) {
            throw new ModuleStorageCoordinatorException.ResourceNotFoundException("Cannot find metadata for all releases " + releaseURIs);
        }
        return results;
    }

    private boolean failedToFindAllModules(List<URI> releaseURIs, Set<URI> modulesAccountedFor) {
        for (URI uri : releaseURIs) {
            if (!modulesAccountedFor.contains(uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reorders releaseURIs in place so that any URI identifying the core module comes first.
     * This avoids repeatedly re-scanning for model/ICD modules that are already accounted for
     * as dependencies of the core module's package.
     *
     * @param releaseURIs List of SNOMED module URIs to reorder.
     */
    private void makeCoreURIsFirst(List<URI> releaseURIs) {
        releaseURIs.sort(Comparator.comparing(uri -> !isCoreModuleUri(uri)));
    }

    private boolean isCoreModuleUri(URI uri) {
        String path = uri.getPath();
        String corePath = MSCUtils.CORE_MODULE_URI.getPath();
        return path.equals(corePath) || path.startsWith(corePath + SLASH);
    }

    private ModuleMetadata obtainPopulatedMetadata(URI uri, Map<String, Set<String>> pathsByDirectory, boolean obtainRF2ArchiveLocally) throws ModuleStorageCoordinatorException {
        ModuleMetadata requestedMetadata = MSCUtils.asModuleMetadata(uri);

        String moduleIdSuffix = "_" + requestedMetadata.getIdentifyingModuleId();
        // When effectiveTime is null, restrict to the primary read directory only (no fallback)
        List<String> directoriesToSearch = requestedMetadata.getEffectiveTime() == null
                ? List.of(readDirectories.getFirst())
                : new ArrayList<>(pathsByDirectory.keySet());

        for (String readDirectory : directoriesToSearch) {
            String directoryPrefix = readDirectory + SLASH;
            Set<String> metadataPaths = pathsByDirectory.get(readDirectory);

            final String resolvedEffectiveTime = requestedMetadata.getEffectiveTimeString();
            Optional<String> match = metadataPaths.stream()
                    .filter(path -> {
                        if (!path.startsWith(directoryPrefix)) {
                            return false;
                        }
                        String[] segments = path.substring(directoryPrefix.length()).split(SLASH);
                        // segments: [codeSystem_moduleId, effectiveTime, metadata.json]
                        if (segments.length < 3 || ROGUE_PACKAGES.contains(segments[0])) {
                            return false;
                        }
                        boolean moduleMatches = segments[0].endsWith(moduleIdSuffix);
                        boolean effectiveTimeMatches = resolvedEffectiveTime == null || segments[1].equals(resolvedEffectiveTime);
                        return moduleMatches && effectiveTimeMatches;
                    })
                    .max(Comparator.comparing(path -> path.substring(directoryPrefix.length()).split(SLASH)[1]));

            if (match.isPresent()) {
                String baseResourcePath = MSCUtils.getBaseResourcePath(match.get());
                return downloadMetadataFromPath(baseResourcePath, obtainRF2ArchiveLocally);
            }
        }

        return null;
    }

    private ModuleMetadata downloadMetadataFromPath(String baseResourcePath, boolean obtainRF2ArchiveLocally) throws ModuleStorageCoordinatorException {
        String metadataPath = getMetadataResourcePath(baseResourcePath);
        try {
            if (remoteStorageManager.doesObjectExist(metadataPath)) {
                ModuleMetadata obtainedMetadata = FileUtils.convertToObject(remoteStorageManager.readResourceStream(metadataPath), ModuleMetadata.class);
                String readDirectory = metadataPath.substring(0, metadataPath.indexOf(SLASH));
                //verifyUriMatches(requestedMetadata, obtainedMetadata);
                if (obtainRF2ArchiveLocally) {
                    LOGGER.debug("Ensuring {} exists locally...", obtainedMetadata.getFilename());
                    populateFileLocally(baseResourcePath, obtainedMetadata);
                }
                return obtainedMetadata;
            }
        } catch (ScriptException | IOException e) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to de-serialize metadata.json at location " + metadataPath, e);
        }

        return null;
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

        String baseResourcePath = getBaseResourcePath(writeDirectory, codeSystem, moduleId, effectiveTime);
        String metadataResourcePath = getMetadataResourcePath(baseResourcePath);
        if (!remoteStorageManager.doesObjectExist(metadataResourcePath)) {
            throw new ModuleStorageCoordinatorException.ResourceNotFoundException("Metadata not found with resource path " + metadataResourcePath);
        }

        ModuleMetadata moduleMetadata;
        File metedataFile = null;
        try {
            metedataFile = remoteStorageManager.doReadResourceFile(metadataResourcePath);
            moduleMetadata = FileUtils.convertToObject(metedataFile, ModuleMetadata.class);
        } catch (IOException | ScriptException e) {
            throw new ModuleStorageCoordinatorException.ResourceNotFoundException("Malformed Metadata found with resource path " + metadataResourcePath, e);
        } finally {
            if (metedataFile != null) {
                Files.delete(metedataFile.toPath());
            }
        }

        String packageResourcePath = getPackageResourcePath(baseResourcePath, moduleMetadata.getFilename());
        if (!remoteStorageManager.doesObjectExist(packageResourcePath)) {
            throw new ModuleStorageCoordinatorException.ResourceNotFoundException("Package not found with resource path " + packageResourcePath);
        }

        String epochSecond = Long.toString(Instant.now().getEpochSecond());
        String metadataArchivePath = asArchivePath(metadataResourcePath, epochSecond);
        String packageArchivePath = asArchivePath(packageResourcePath, epochSecond);
        boolean metadataCopied = remoteStorageManager.doCopyResource(metadataResourcePath, metadataArchivePath);
        if (!metadataCopied) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to copy metadata from " + metadataResourcePath + " to " + metadataArchivePath);
        }

        boolean packageCopied = remoteStorageManager.doCopyResource(packageResourcePath, packageArchivePath);
        if (!packageCopied) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to copy package from " + packageResourcePath + " to " + packageArchivePath);
        }

        String additionalResourcesPath = getAdditionalResourcesPath(baseResourcePath);
        Set<String> additionalResourcesPaths = remoteStorageManager.listFilenames(additionalResourcesPath);
        for (String i : additionalResourcesPaths) {
            remoteStorageManager.doCopyResource(i, asArchivePath(i, epochSecond));
        }

        boolean metadataDeleted = remoteStorageManager.doDeleteResource(metadataResourcePath);
        if (!metadataDeleted) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to metadata package from " + metadataResourcePath);
        }

        boolean packageDeleted = remoteStorageManager.doDeleteResource(packageResourcePath);
        if (!packageDeleted) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to delete package from " + packageResourcePath);
        }

        for (String string : additionalResourcesPaths) {
            remoteStorageManager.doDeleteResource(string);
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
    public List<ModuleMetadata> getRelease(String codeSystem, String moduleId, String effectiveTime, boolean includeFile, boolean includeDependencies) throws ModuleStorageCoordinatorException {
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
    public List<ModuleMetadata> getRelease(String codeSystem, String moduleId, String effectiveTime) throws ModuleStorageCoordinatorException {
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
    public void setPublished(String codeSystem, String moduleId, String effectiveTime, boolean published) throws ModuleStorageCoordinatorException {
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
    public void setEdition(String codeSystem, String moduleId, String effectiveTime, boolean edition) throws ModuleStorageCoordinatorException {
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
    public Map<String, List<ModuleMetadata>> getAllReleases() throws ModuleStorageCoordinatorException {
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
    public Map<String, List<ModuleMetadata>> getAllReleases(int page, int size) throws ModuleStorageCoordinatorException {
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
    public List<ModuleMetadata> getAllReleases(String codeSystem) throws ModuleStorageCoordinatorException {
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
    public List<String> getCodeSystems() throws ModuleStorageCoordinatorException {
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
    public List<Integer> getReleaseDates(String codeSystem) throws ModuleStorageCoordinatorException {
        if (codeSystem == null || codeSystem.isEmpty()) {
            throw new ModuleStorageCoordinatorException.InvalidArgumentsException("CodeSystem invalid (null or empty)");
        }

        return doGetAllReleasesByCodeSystem(codeSystem).stream().map(ModuleMetadata::getEffectiveTime).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
    }


    /**
     * Return dependencies stored for given MDRS entries.
     *
     * @param mdrsRows    MDRS entries to process.
     * @param includeFile Whether to include RF2 file.
     * @return Dependencies stored for given MDRS entries.
     */
    public Set<ModuleMetadata> getDependencies(Set<RF2Row> mdrsRows, Set<String> expectedModules, boolean includeFile) throws ModuleStorageCoordinatorException {
        if (mdrsRows == null || mdrsRows.isEmpty() || expectedModules == null || expectedModules.isEmpty()) {
            return Collections.emptySet();
        }
        // Remove MDRS rows if they don't exist in the extension module
        mdrsRows.removeIf(item -> !expectedModules.contains(item.getColumn(RF2Service.MODULE_ID)));

        // Self-depending modules have no unknown, external dependencies
        removeSelfDependingModules(mdrsRows);

        // Collect available rf2 packages
        Set<ModuleMetadata> rf2Packages = getRF2Packages();

		// Keep those with matching referencedComponentId & targetEffectiveTime
		rf2Packages = keepReferencedComponentIdMatchingIdentifyingModuleIdAndTargetEffectiveTimeMatchingEffectiveTime(rf2Packages, mdrsRows);

        // Group by IdentifyingModule
        Map<String, Set<ModuleMetadata>> byIdentifyingModule = sortByIdentifyingModule(rf2Packages);

        // Flatten into single collection with latest or specified version
        Set<ModuleMetadata> moduleMetadata = flattenByLatest(byIdentifyingModule);

        if (!includeFile) {
            return moduleMetadata;
        }

        addFilesLocally(moduleMetadata);
        return moduleMetadata;
    }

    public Set<ModuleMetadata> getComposition(Set<RF2Row> mdrsRows, boolean includeFile) throws ModuleStorageCoordinatorException {
        return getComposition(mdrsRows, includeFile, null);
    }

    /**
     * Return composition for given MDRS entries.
     *
     * @param mdrsRows      MDRS entries to process.
     * @param includeFile   Whether to include RF2 file.
     * @return Composition for given MDRS entries.
     */
    public Set<ModuleMetadata> getComposition(Set<RF2Row> mdrsRows, boolean includeFile, Set<String> transientEffectiveTimes) throws ModuleStorageCoordinatorException {
        if (mdrsRows == null || mdrsRows.isEmpty()) {
            return Collections.emptySet();
        }

		// Replace blank sourceEffectiveTimes and targetEffectiveTimes with transientEffectiveTimes
		if (transientEffectiveTimes != null && !transientEffectiveTimes.isEmpty()) {
			mdrsRows = rf2Service.setTransientEffectiveTimes(mdrsRows, transientEffectiveTimes);
		}

        // Collect available rf2 packages
        Set<ModuleMetadata> rf2Packages = getRF2Packages();

        // Remove those not specified in MDRS
        rf2Packages = filterByModuleIdAndSourceEffectiveTimeOrReferencedComponentIdAndTargetEffectiveTime(rf2Packages, mdrsRows, transientEffectiveTimes);

        // Group by IdentifyingModule
        Map<String, Set<ModuleMetadata>> byIdentifyingModule = sortByIdentifyingModule(rf2Packages);

        // Flatten into single collection with latest or specified version
        Set<ModuleMetadata> moduleMetadata = flattenByLatest(byIdentifyingModule);

        if (!includeFile) {
            return moduleMetadata;
        }

        addFilesLocally(moduleMetadata);
        return moduleMetadata;
    }

    private Set<RF2Row> getMdrsRows(File archive) {
        // Try Snapshot folder first (full/published packages), fall back to Delta folder (delta-only zips)
        Set<RF2Row> mdrsRows = rf2Service.getMDRS(archive, false);
        if (mdrsRows.isEmpty()) {
            mdrsRows = rf2Service.getMDRS(archive, true);
        }
        return mdrsRows;
    }

    public CurrentPreviousModuleMetadataPair getCurrentAndPreviousMetadata(File archive, boolean obtainFilesLocally) throws ModuleStorageCoordinatorException {
        Set<RF2Row> mdrsRows =  getMdrsRows(archive);
        ModuleMetadata currentRelease = new ModuleMetadata().withFile(archive);
        ModuleMetadata previousRelease = null;

        //The current package can be determined by either the empty, or most recent, target effective times
        populateComposition(currentRelease, mdrsRows);
        populateDependencies(currentRelease, mdrsRows, obtainFilesLocally);

        //Find one of these modules in S3, otherwise our 'identifying' module will be random.
        //Any other modules with blank target effective times will remain part of our composition
        List<ModuleMetadata> previousReleases = getMetadata(currentRelease.getCompositionAsURIs(), SearchRequirement.ENSURE_ONE_FOUND, true);

        //This can be null if there are no previous releases, but we should only find one for a given composition
        //because there will be only one publication using the identifying module
        if (previousReleases.size() > 1) {
            String debugModules = previousReleases.stream()
                    .map(ModuleMetadata::getCompositionAsURIs)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            throw new ModuleStorageCoordinatorException.OperationFailedException("More than one previous release found for " + debugModules);
        } else if (previousReleases.size() == 1) {
            previousRelease = previousReleases.getFirst();
            //At this point, we know what our identifying module is, and we can promote it out of the composition
            currentRelease.setIdentifyingModuleId(previousRelease.getIdentifyingModuleId());
            currentRelease.getCompositionModuleIds().remove(previousRelease.getIdentifyingModuleId());
        }

        return new CurrentPreviousModuleMetadataPair(currentRelease, previousRelease);
    }

    private void populateComposition(ModuleMetadata currentRelease, Set<RF2Row> mdrsRows) {
        //The current package can be determined by either the empty, or most recent target effective times

        // Max SOURCE_EFFECTIVE_TIME across all rows identifies the current package version (null = in-progress delta)
        String currentET = mdrsRows.stream()
                .map(row -> row.getColumn(RF2Service.SOURCE_EFFECTIVE_TIME))
                .filter(StringUtils::hasLength)
                .max(Comparator.naturalOrder())
                .orElse(null);
        currentRelease.setEffectiveTime(currentET == null ? null : Integer.parseInt(currentET));

        // Composition modules authored the rows matching the current effective time (or blank for in-progress)
        List<String> compositionModuleIds = mdrsRows.stream()
                .filter(row -> currentET == null
                        ? !StringUtils.hasLength(row.getColumn(RF2Service.SOURCE_EFFECTIVE_TIME))
                        : currentET.equals(row.getColumn(RF2Service.SOURCE_EFFECTIVE_TIME)))
                .map(row -> row.getColumn(RF2Service.MODULE_ID))
                .distinct()
                .collect(Collectors.toList()); //Can't used toList() directly as we'll modify this list later

        //The model module is the only one that won't appear in the MDRS because it has no dependencies
        if (compositionModuleIds.contains(SCTID_CORE_MODULE) && !compositionModuleIds.contains(SCTID_MODEL_MODULE)) {
            compositionModuleIds.add(SCTID_MODEL_MODULE);
        }

        currentRelease.setCompositionModuleIds(compositionModuleIds);
    }

    private void populateDependencies(ModuleMetadata currentRelease, Set<RF2Row> mdrsRows, boolean obtainFilesLocally) throws ModuleStorageCoordinatorException {
        //Dependencies are the referenced component ids, that do NOT have the same ET as the
        //current release.   If we can't find that target, we'll exception out.
        List<URI> dependencyURIs = mdrsRows.stream()
                .filter(row -> isNotCurrentRelease(row, currentRelease))
                .map(this::targetModuleAsURI)
                .collect(Collectors.toList());
        List<ModuleMetadata> dependencies = getMetadata(dependencyURIs, SearchRequirement.ENSURE_ALL_FOUND, obtainFilesLocally);
        currentRelease.setDependencies(dependencies);
    }

    boolean isNotCurrentRelease(RF2Row row, ModuleMetadata currentRelease) {
        String thisReleaseET = currentRelease.getEffectiveTimeString();
        String targetET = row.getColumn(RF2Service.TARGET_EFFECTIVE_TIME);
        return StringUtils.hasLength(targetET) && !targetET.equals(thisReleaseET);
    }

    private URI targetModuleAsURI(RF2Row row) {
        String targetModuleId = row.getColumn(RF2Service.REFERENCED_COMPONENT_ID);
        String targetEffectiveTime = row.getColumn(RF2Service.TARGET_EFFECTIVE_TIME);
        return MSCUtils.editionUri(targetModuleId, targetEffectiveTime);
    }


    /**
     * Retrieves dependencies and previous version of packages based on MDRS rows.
     * We need to look at getCurrentAndPreviousMetadata and discuss in code conversation
     *
     * @param mdrsRows          MDRS rows to process.
     * @param includeFile       Whether to include RF2 file.
     * @param maxEffectiveTimes Maximum effective times for filtering packages.
     * @return Set of ModuleMetadata representing dependencies and previous versions.
     */
    public Set<ModuleMetadata> getDependenciesAndPreviousVersion(Set<RF2Row> mdrsRows, boolean includeFile, Set<String> maxEffectiveTimes) throws ModuleStorageCoordinatorException {
        if (mdrsRows == null || mdrsRows.isEmpty()) {
            return Collections.emptySet();
        }

        // Collect available rf2 packages
        Set<ModuleMetadata> rf2Packages = getRF2Packages();

        // Remove those versioned beyond upper boundary (prevents prod being available on dev)
        rf2Packages = removeVersionedBeyondUpperBoundary(mdrsRows, rf2Packages, maxEffectiveTimes);

        // Find dependent packages (determined by referencedComponentId)
        Set<ModuleMetadata> dependantPackages = getDependantPackages(rf2Packages, mdrsRows);

        // Find own packages (determined by moduleId)
        Set<ModuleMetadata> ownPackages = getOwnPackages(rf2Packages, mdrsRows, dependantPackages);

        // Join
        Set<ModuleMetadata> packages = Stream.of(dependantPackages, ownPackages).flatMap(Set::stream).collect(Collectors.toSet());

        if (!includeFile) {
            return packages;
        }

        addFilesLocally(packages);
        return packages;
    }

    private Set<ModuleMetadata> getRF2Packages() throws ModuleStorageCoordinatorException {
        Map<String, ModuleMetadata> rf2PackageMap = new HashMap<>();
        for (String readDirectory : readDirectories) {
            Set<String> rf2PackagePaths = remoteStorageManager.doListFilenames(readDirectory, ".zip");
            if (rf2PackagePaths.isEmpty()) {
                continue;
            }

            for (String rfPackagePath : rf2PackagePaths) {
                ModuleMetadata moduleMetadata = downloadMetadataFromPath(MSCUtils.convertArchivePathToMetadataPath(rfPackagePath), false) ;
                if (moduleMetadata != null && !Objects.equals("SIMPLEX", moduleMetadata.getCodeSystemShortName())) {
                    // Allow dev to overwrite prod
                    String filename = moduleMetadata.getFilename();
                    rf2PackageMap.put(filename, moduleMetadata);
                }
            }
        }

        return new HashSet<>(rf2PackageMap.values());
    }

    private Set<ModuleMetadata> flattenByLatest(Map<String, Set<ModuleMetadata>> byCodeSystem) {
        Set<ModuleMetadata> dependencies = new HashSet<>();
        for (Map.Entry<String, Set<ModuleMetadata>> entrySet : byCodeSystem.entrySet()) {
            dependencies.add(entrySet.getValue().iterator().next());
        }

        return dependencies;
    }

    private Map<String, Set<ModuleMetadata>> sortByIdentifyingModule(Set<ModuleMetadata> rf2Packages) {
        Map<String, Set<ModuleMetadata>> versionsByIdentifyingModule = new HashMap<>();
        for (ModuleMetadata rf2Package : rf2Packages) {
            String key = rf2Package.getIdentifyingModuleId();
            Set<ModuleMetadata> value = versionsByIdentifyingModule.get(key);
            if (value == null) {
                value = new TreeSet<>((o1, o2) -> o2.getEffectiveTime().compareTo(o1.getEffectiveTime()));
            }
            value.add(rf2Package);
            versionsByIdentifyingModule.put(key, value);
        }

        return versionsByIdentifyingModule;
    }

    private void addFilesLocally(Set<ModuleMetadata> moduleMetadataSet) {
        try {
            for (ModuleMetadata moduleMetadata : moduleMetadataSet) {
                addFileLocally(moduleMetadata);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public void addFileLocally(ModuleMetadata moduleMetadata) throws ModuleStorageCoordinatorException {
        for (String readDirectory : this.readDirectories) {
            String baseResourcePath = getBaseResourcePath(readDirectory, moduleMetadata);
            String metadataResourcePath = getMetadataResourcePath(baseResourcePath);
            if (remoteStorageManager.doesObjectExist(metadataResourcePath)) {
                populateFileLocally(baseResourcePath, moduleMetadata);
            } else {
                LOGGER.warn("Item not found in S3: {}", metadataResourcePath);
            }
        }
    }

    private List<ModuleMetadata> doGetAllReleasesByCodeSystem(String codeSystem) throws ModuleStorageCoordinatorException {
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

    private Map<String, List<ModuleMetadata>> doGetAllReleases() throws ModuleStorageCoordinatorException {
        Map<String, List<ModuleMetadata>> releases = new HashMap<>();
        for (String readDirectory : readDirectories) {
            // List all json resource paths in readDirectory
            Set<String> metadataResourcePaths = remoteStorageManager.doListFilenames(readDirectory, ".json");
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
                boolean overwritten = moduleMetadatas.stream().anyMatch(metadata -> Objects.equals(metadata.getEffectiveTimeString(), effectiveTime) && Objects.equals(metadata.getIdentifyingModuleId(), moduleId));
                if (overwritten) {
                    continue;
                }

                // Get from the local cache or remote
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

    private String getMetadataResourcePath(String baseResourcePath) {
        return baseResourcePath + "/metadata.json";
    }

    private String getPackageResourcePath(String baseResourcePath, String rf2PackageFileName) {
        return baseResourcePath + SLASH + rf2PackageFileName;
    }

    private String getAdditionalResourcesPath(String baseResourcePath) {
        return baseResourcePath + SLASH + ADDITIONAL_DELIVERABLES;
    }

    private FileInputStream asFileInputStream(File file) throws ModuleStorageCoordinatorException.OperationFailedException {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Cannot convert File to FileInputStream.");
        }
    }

    private String getBaseResourcePath(String directory, ModuleMetadata metadata) {
        return getBaseResourcePath(directory,
                metadata.getCodeSystemShortName(),
                metadata.getIdentifyingModuleId(),
                metadata.getEffectiveTimeString());
    }

    private String getBaseResourcePath(String directory, String codeSystem, String moduleId, String effectiveTime) {
        return directory + SLASH + codeSystem + "_" + moduleId + SLASH + effectiveTime;
    }

    private Set<ModuleMetadata> getDependencies(File rf2Package, Set<String> excludedModuleIds) throws ModuleStorageCoordinatorException.OperationFailedException {
        Set<RF2Row> rf2Rows = rf2Service.getMDRS(rf2Package, false);
        return getDependencies(rf2Rows, excludedModuleIds);
    }

    private Set<ModuleMetadata> getDependencies(Set<RF2Row> mdrsRows, Set<String> excludedModuleIds) throws ModuleStorageCoordinatorException.OperationFailedException {
        mdrsRows = filterLatestTargetEffectiveTime(mdrsRows);
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
            Set<String> availableRF2Packages = remoteStorageManager.doListFilenames(readDirectory, ".zip");
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
                        possibleRF2Package = remoteStorageManager.doReadResourceFile(possibleRF2PackagePath);
                        Set<RF2Row> rf2Rows = rf2Service.getUniqueModulesWithLatestEffectiveTime(possibleRF2Package, false);
                        boolean owningPackageFound = rf2Rows.stream().anyMatch(item -> item.getColumn(RF2Service.MODULE_ID).equals(rf2Row.getColumn(RF2Service.REFERENCED_COMPONENT_ID)) && item.getColumn(RF2Service.EFFECTIVE_TIME).equals(rf2Row.getColumn(RF2Service.TARGET_EFFECTIVE_TIME)));
                        if (owningPackageFound) {
                            rf2Row.setFound(true);
                            found = found + 1;
                            rf2Row.setMetadataResourcePath(MSCUtils.convertArchivePathToMetadataPath(possibleRF2PackagePath));
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
                String message = String.format("The referenced module %s with target effective time '%s' cannot be found. Ensure dependent packages are uploaded first.", rf2Row.getColumn(RF2Service.REFERENCED_COMPONENT_ID), rf2Row.getColumn(RF2Service.TARGET_EFFECTIVE_TIME));
                LOGGER.warn(message);
            } else {
                metadataResourcePaths.add(rf2Row.getMetadataResourcePath());
            }
        }

        Set<ModuleMetadata> moduleMetadatas = new TreeSet<>(Comparator.comparingInt(ModuleMetadata::getEffectiveTime));
        for (String dep : metadataResourcePaths) {
            try {
                File file = remoteStorageManager.doReadResourceFile(dep);
                ModuleMetadata moduleMetadata = FileUtils.convertToObject(file, ModuleMetadata.class);
                moduleMetadatas.add(moduleMetadata);
            } catch (IOException | ScriptException e) {
                throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to de-serialize metadata.json at location " + dep, e);
            }
        }

        return moduleMetadatas;
    }

    private Set<RF2Row> filterLatestTargetEffectiveTime(Set<RF2Row> mdrsRows) {
        if (mdrsRows == null || mdrsRows.isEmpty()) {
            return Collections.emptySet();
        }

        Map<String, RF2Row> latestByModuleAndReference = new HashMap<>();
        for (RF2Row mdrsRow : mdrsRows) {
            if (mdrsRow == null) {
                continue;
            }

            String moduleId = mdrsRow.getColumn(RF2Service.MODULE_ID);
            String referencedComponentId = mdrsRow.getColumn(RF2Service.REFERENCED_COMPONENT_ID);
            String key = (moduleId == null ? "" : moduleId) + "|" + (referencedComponentId == null ? "" : referencedComponentId);

            RF2Row existing = latestByModuleAndReference.get(key);
            if (existing == null) {
                latestByModuleAndReference.put(key, mdrsRow);
            } else {
                String candidateEffectiveTime = mdrsRow.getColumn(RF2Service.TARGET_EFFECTIVE_TIME);
                String existingEffectiveTime = existing.getColumn(RF2Service.TARGET_EFFECTIVE_TIME);

                if (isLaterTargetEffectiveTime(candidateEffectiveTime, existingEffectiveTime)) {
                    latestByModuleAndReference.put(key, mdrsRow);
                }
            }
        }

        return new HashSet<>(latestByModuleAndReference.values());
    }

    private boolean isLaterTargetEffectiveTime(String candidate, String current) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }

        if (current == null || current.isBlank()) {
            return true;
        }

        try {
            return asInteger(candidate) > asInteger(current);
        } catch (NumberFormatException e) {
            return candidate.compareTo(current) > 0;
        }
    }

    private Integer asInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

    private ModuleMetadata doGetMetadata(String codeSystem, String moduleId, String effectiveTime, boolean includeFile) throws ModuleStorageCoordinatorException {
        LOGGER.debug("Attempting to download from location {}_{}/{}", codeSystem, moduleId, effectiveTime);
        throwIfInvalid(codeSystem, moduleId, effectiveTime);

        for (String readDirectory : this.readDirectories) {
            String baseResourcePath = getBaseResourcePath(readDirectory, codeSystem, moduleId, effectiveTime);
            String metadataResourcePath = getMetadataResourcePath(baseResourcePath);
            ModuleMetadata obtainedMetadata = downloadMetadataFromPath(metadataResourcePath, includeFile);
            if (obtainedMetadata != null) {
                return obtainedMetadata;
            }
        }

        String message = String.format("Cannot find package for %s_%s/%s", codeSystem, moduleId, effectiveTime);
        throw new ModuleStorageCoordinatorException.ResourceNotFoundException(message);
    }

    private void populateFileLocally(String baseResourcePath, ModuleMetadata moduleMetadata) throws ModuleStorageCoordinatorException {
        boolean localStorageAvailable = localStorageManager != null;
        String rf2ArchiveResourcePath = baseResourcePath + SLASH + moduleMetadata.getFilename();
        if (remoteStorageManager.doesObjectExist(rf2ArchiveResourcePath)) {
            if (localStorageAvailable) {
                ensureRF2ArchiveAvailableLocally(rf2ArchiveResourcePath, moduleMetadata);
            } else {
                doGetMetadataFromRemote(rf2ArchiveResourcePath, moduleMetadata);
            }
        } else {
            LOGGER.warn("Item not found in S3: {}", rf2ArchiveResourcePath);
        }
    }

    private void ensureRF2ArchiveAvailableLocally(String rf2ResourcePath, ModuleMetadata moduleMetadata) throws ModuleStorageCoordinatorException {
		String localStoreRoot = localStorageManager.getCachePath();
		String localPathName = String.format("%s/%s", localStoreRoot, rf2ResourcePath);
		File localRF2Package = localStorageManager.getNullable(localPathName);
		moduleMetadata.setFile(localRF2Package);

		if (localRF2Package != null) {
            String localPackageMD5 = FileUtils.getMD5Nullable(localRF2Package);
			boolean localMD5MatchesRemote = Objects.equals(localPackageMD5, moduleMetadata.getFileMD5());
			if (!localMD5MatchesRemote) {
				// Local storage is invalid, ignore and re-download
				localRF2Package = null;
			}
		}

		// Remote fallback
		if (localRF2Package == null) {
            LOGGER.info("Downloading from S3 to local storage {}", localPathName);
			try (InputStream inputStream = remoteStorageManager.readResourceStream(rf2ResourcePath)) {
				localStorageManager.doWriteResource(rf2ResourcePath, inputStream);
			} catch (Exception e) {
				throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to download RF2 archive from S3 " + rf2ResourcePath, e);
			}

			localRF2Package = new File(localPathName);
			moduleMetadata.setFile(localRF2Package);
		}
	}

    private void doGetMetadataFromRemote(String rf2ResourcePath, ModuleMetadata moduleMetadata) throws ModuleStorageCoordinatorException.OperationFailedException {
        try {
            moduleMetadata.setFile(remoteStorageManager.doReadResourceFile(rf2ResourcePath));
        } catch (IOException e) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to get metadata from remote " + rf2ResourcePath, e);
        }
    }

    private String asArchivePath(String resourcePath, String epochSecond) {
        // otf-common/files/ABC_12345/20240101/metadata.json => // otf-common/files/ABC_12345/archive/$epochSecond/metadata.json
        return resourcePath.replaceAll("/\\d{8}/", "/archive/" + epochSecond + SLASH);
    }

    private void doUpdateModuleMetadata(String codeSystem, String moduleId, String effectiveTime, ModuleMetadata moduleMetadata) throws ModuleStorageCoordinatorException.OperationFailedException {
        String baseResourcePath = getBaseResourcePath(writeDirectory, codeSystem, moduleId, effectiveTime);
        LOGGER.debug("Attempting to update metadata at location {}", baseResourcePath);
        File tmpMetadataFile = null;
        try {
            // Write new to local temporary file
            tmpMetadataFile = FileUtils.doCreateTempFile("metadata.json");
            FileUtils.writeToFile(tmpMetadataFile, moduleMetadata);
        } catch (IOException | ScriptException e) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to write metadata.json", e);
        }

        // Copy old to archive
        String resourcePathMetadata = getMetadataResourcePath(baseResourcePath);
        String epochSecond = Long.toString(Instant.now().getEpochSecond());
        String metadataArchivePath = asArchivePath(resourcePathMetadata, epochSecond);
        boolean metadataCopied = remoteStorageManager.doCopyResource(resourcePathMetadata, metadataArchivePath);
        if (!metadataCopied) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to copy metadata from " + resourcePathMetadata + " to " + metadataArchivePath);
        }

        // Delete old
        boolean metadataDeleted = remoteStorageManager.doDeleteResource(resourcePathMetadata);
        if (!metadataDeleted) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to delete metadata from " + resourcePathMetadata);
        }

        // Upload new
        try {
            remoteStorageManager.doWriteResource(resourcePathMetadata, asFileInputStream(tmpMetadataFile));
        } catch (IOException e) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to upload metadata to " + resourcePathMetadata, e);
        }

        // Check if new uploaded
        boolean newMetadata = remoteStorageManager.doesObjectExist(resourcePathMetadata);
        if (!newMetadata) {
            throw new ModuleStorageCoordinatorException.OperationFailedException("Failed to upload metadata to " + resourcePathMetadata);
        }
    }

    private List<ModuleMetadata> doGetRelease(String codeSystem, String moduleId, String effectiveTime, boolean includeFile, boolean includeDependencies) throws ModuleStorageCoordinatorException {
        throwIfInvalid(codeSystem, moduleId, effectiveTime);

        Set<ModuleMetadata> moduleMetadata = new LinkedHashSet<>();
        appendModuleMetadataRecursive(codeSystem, moduleId, effectiveTime, includeFile, includeDependencies, moduleMetadata);
        return new ArrayList<>(moduleMetadata);
    }

    private void appendModuleMetadataRecursive(String codeSystem, String moduleId, String effectiveTime, boolean includeFile, boolean includeDependencies, Set<ModuleMetadata> moduleMetadatas) throws ModuleStorageCoordinatorException {
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

    private void removeSelfDependingModules(Set<RF2Row> rows) {
        List<String> moduleIds = rows.stream().map(r -> r.getColumn(RF2Service.MODULE_ID)).toList();
        List<String> referencedComponentIds = rows.stream().map(r -> r.getColumn(RF2Service.REFERENCED_COMPONENT_ID)).toList();
        List<String> diff = new ArrayList<>();
        for (String referencedComponentId : referencedComponentIds) {
            if (moduleIds.contains(referencedComponentId)) {
                diff.add(referencedComponentId);
            }
        }

        rows.removeIf(rf2Row -> diff.contains(rf2Row.getColumn(RF2Service.REFERENCED_COMPONENT_ID)));
    }

    private Set<ModuleMetadata> removeVersionedBeyondUpperBoundary(Set<RF2Row> mdrsRows, Set<ModuleMetadata> rf2Packages, Set<String> maxEffectiveTimes) {
        if (maxEffectiveTimes == null) {
            maxEffectiveTimes = new HashSet<>();
        }

        for (RF2Row mdrsRow : mdrsRows) {
            String effectiveTime = mdrsRow.getColumn(RF2Service.EFFECTIVE_TIME);
            if (StringUtils.hasLength(effectiveTime)) {
                maxEffectiveTimes.add(effectiveTime);
            }

            String sourceEffectiveTime = mdrsRow.getColumn(RF2Service.SOURCE_EFFECTIVE_TIME);
            if (StringUtils.hasLength(sourceEffectiveTime)) {
                maxEffectiveTimes.add(sourceEffectiveTime);
            }

            String targetEffectiveTime = mdrsRow.getColumn(RF2Service.TARGET_EFFECTIVE_TIME);
            if (StringUtils.hasLength(targetEffectiveTime)) {
                maxEffectiveTimes.add(targetEffectiveTime);
            }
        }

        if (maxEffectiveTimes.isEmpty()) {
            return rf2Packages;
        }

        int upperBoundary = maxEffectiveTimes.stream().mapToInt(Integer::parseInt).max().orElse(Integer.MAX_VALUE);
        return rf2Packages.stream().filter(pkg -> pkg.getEffectiveTime() <= upperBoundary).collect(Collectors.toSet());
    }

    private Set<ModuleMetadata> getDependantPackages(Set<ModuleMetadata> rf2Packages, Set<RF2Row> mdrsRows) {
        // Find by matching referencedComponentId and targetEffectiveTime
        Set<ModuleMetadata> moduleMetadata = filterByReferencedComponentIdAndTargetEffectiveTime(rf2Packages, mdrsRows);

        // Remove own package from dependencies
        // For scenarios where regression testing & 'republishing' a package
        Set<String> moduleIds = mdrsRows.stream().map(row -> row.getColumn(RF2Service.MODULE_ID)).collect(Collectors.toSet());
        moduleMetadata.removeIf(item -> moduleIds.contains(item.getIdentifyingModuleId()));

        return moduleMetadata;
    }

    private Map<String, String> getSourceEffectiveTimesByModuleId(Set<RF2Row> mdrsRows) {
        Map<String, String> sourceEffectiveTimesByModuleId = new HashMap<>();
        for (RF2Row mdrsRow : mdrsRows) {
            sourceEffectiveTimesByModuleId.put(mdrsRow.getColumn(RF2Service.MODULE_ID), mdrsRow.getColumn(RF2Service.SOURCE_EFFECTIVE_TIME));
        }
        return sourceEffectiveTimesByModuleId;
    }

    private Set<ModuleMetadata> getOwnPackages(Set<ModuleMetadata> rf2Packages, Set<RF2Row> mdrsRows, Set<ModuleMetadata> dependantPackages) {
        // Remove those not specified in MDRS
        Set<ModuleMetadata> modules = filterByModuleId(rf2Packages, mdrsRows);

        // Remove those previously captured in dependantPackages (e.g. removes International from Edition packages)
        List<String> dependantCodeSystemsCompositionModuleIds = dependantPackages.stream().map(ModuleMetadata::getCompositionModuleIds).flatMap(List::stream).toList();
        modules.removeIf(moduleMetadata -> moduleMetadata.getCompositionModuleIds().equals(dependantCodeSystemsCompositionModuleIds));

        // Sort by CodeSystem
        Map<String, Set<ModuleMetadata>> byIdentifyingModule = sortByIdentifyingModule(modules);
        // sourceEffectiveTime by moduleId
        Map<String, String> sourceEffectiveTimesByModuleId = getSourceEffectiveTimesByModuleId(mdrsRows);

        // Determine which package(s) to use
        Set<ModuleMetadata> ownPackages = new HashSet<>();
        for (Map.Entry<String, String> entrySet : sourceEffectiveTimesByModuleId.entrySet()) {
            String moduleId = entrySet.getKey();
            Set<ModuleMetadata> packages = byIdentifyingModule.get(moduleId);
            if (packages == null || packages.isEmpty()) {
                continue;
            }

            String sourceEffectiveTime = entrySet.getValue();
            if (sourceEffectiveTime == null || sourceEffectiveTime.isEmpty()) {
                // Use latest
                ownPackages.add(packages.iterator().next());
            } else {
                // Use previous version (i.e. latest - 1)
                Integer effectiveTime = Integer.parseInt(sourceEffectiveTime);
                Set<ModuleMetadata> packagesMinusSourceEffectiveTime =
                        packages.stream()
                                .filter(m -> m.getEffectiveTime() < effectiveTime)
                                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparingInt(ModuleMetadata::getEffectiveTime).reversed())));

                Iterator<ModuleMetadata> iterator = packagesMinusSourceEffectiveTime.iterator();
                if (iterator.hasNext()) {
                    ownPackages.add(iterator.next());
                }
            }
        }

        return ownPackages;
    }

    public ModuleMetadata getMetadata(URI codeSystemVersionURI) throws ModuleStorageCoordinatorException {
        if (codeSystemVersionURI == null) {
            throw new ModuleStorageCoordinatorException.InvalidArgumentsException("No URI specified");
        }
        // Parse URI path: /sct/{moduleId}/version/{effectiveTime}
        String[] segments = codeSystemVersionURI.getPath().split("/");
        if (segments.length < 5 || !"sct".equals(segments[1]) || !"version".equals(segments[3])) {
            throw new ModuleStorageCoordinatorException.InvalidArgumentsException("Unrecognised or insufficient URI format: " + codeSystemVersionURI);
        }
        String moduleId = segments[2];
        String effectiveTime = segments[4];

        //We won't know the codeSystem super-shortname, but it's not required.  So pass null.
        return getMetadata(null, moduleId, effectiveTime);
    }

    public ModuleMetadata getMetadata(File archiveFile) throws ModuleStorageCoordinatorException {
        if (archiveFile == null) {
            throw new ModuleStorageCoordinatorException.InvalidArgumentsException("No archive file specified");
        }
        if (!archiveFile.exists() || !archiveFile.isFile() || !archiveFile.canRead()) {
            throw new ModuleStorageCoordinatorException.ResourceNotFoundException("Unable to read archive file: " + archiveFile.getName());
        }

        // Check for a companion .metadata file written alongside the archive
        File companionMetadata = new File(archiveFile.getParent(), archiveFile.getName() + ".metadata");
        if (companionMetadata.exists() && companionMetadata.isFile()) {
            try {
                ModuleMetadata metadata = FileUtils.convertToObject(companionMetadata, ModuleMetadata.class);
                metadata.setFile(archiveFile);
                return metadata;
            } catch (ScriptException e) {
                LOGGER.warn("Failed to find/read companion metadata file {}, falling back to S3 lookup", companionMetadata.getName(), e);
            }
        }

        ModuleMetadata metadata = findPackageOrThrow(archiveFile.getName(), false);  //Don't need to get file locally, have already been given it
        metadata.setFile(archiveFile);
        return metadata;
   }


    public ModuleMetadata findPackageOrThrow(String packageName, boolean includeFile) throws ModuleStorageCoordinatorException {
        // Search S3 for a package whose filename matches
        for (String readDirectory : readDirectories) {
            for (String rf2PackagePath : remoteStorageManager.doListFilenames(readDirectory, ".zip")) {
                if (rf2PackagePath.endsWith(SLASH + packageName)) {
                    String baseResourcePath = MSCUtils.getBaseResourcePath(rf2PackagePath);
                    ModuleMetadata metadata = downloadMetadataFromPath(baseResourcePath, false);
                    if (includeFile) {
                        populateFileLocally(baseResourcePath, metadata);
                    }
                    return metadata;
                }
            }
        }
        throw new ModuleStorageCoordinatorException.ResourceNotFoundException("Cannot find metadata for archive file: " + packageName);
    }


}
