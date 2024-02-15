package org.snomed.module.storage;

import org.ihtsdo.otf.exception.ScriptException;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.junit.jupiter.api.Test;
import org.snomed.otf.script.utils.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ModuleStorageCoordinatorIntegrationTest extends IntegrationTest {
    @Test
    public void upload_ShouldThrowExpected_WhenGivenInvalidCodeSystem() {
        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.upload(" ", "12345", "20240101", getLocalFile("test-rf2-edition.zip"));
        });
    }

    @Test
    public void upload_ShouldThrowExpected_WhenGivenInvalidModuleId() {
        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.upload("INT", " ", "20240101", getLocalFile("test-rf2-edition.zip"));
        });
    }

    @Test
    public void upload_ShouldThrowExpected_WhenGivenInvalidEffectiveTime() {
        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.upload("INT", "12345", "2024-01-01", getLocalFile("test-rf2-edition.zip"));
        });
    }

    @Test
    public void upload_ShouldThrowExpected_WhenGivenInvalidFile() {
        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.upload("INT", "12345", "20240101", null);
        });
    }

    @Test
    public void upload_ShouldDoExpected_WhenGivenEdition() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // when
        moduleStorageCoordinatorDev.upload("INT", "12345", "20240101", getLocalFile("test-rf2-edition.zip"));
        boolean metadata = doDoesObjectExist("dev/INT_12345/20240101/metadata.json");
        boolean rf2Package = doDoesObjectExist("dev/INT_12345/20240101/test-rf2-edition.zip");

        // then
        assertTrue(metadata);
        assertTrue(rf2Package);
    }

    @Test
    public void upload_ShouldThrowExpected_WhenMetadataFails() throws IOException {
        // given
        File unknown = FileUtils.doCreateTempFile("unknown.zip");

        // then
        assertThrows(ModuleStorageCoordinatorException.OperationFailedException.class, () -> {
            moduleStorageCoordinatorDev.upload("INT", "12345", "20240101", unknown);
        });
    }

    @Test
    public void upload_ShouldThrowExpected_WhenMetadataAlreadyExists() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenArchive("dev/INT_12345/20240101", "metadata.json");

        // then
        assertThrows(ModuleStorageCoordinatorException.DuplicateResourceException.class, () -> {
            moduleStorageCoordinatorDev.upload("INT", "12345", "20240101", getLocalFile("test-rf2-edition.zip"));
        });
    }

    @Test
    public void upload_ShouldThrowExpected_WhenRF2PackageAlreadyExists() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenArchive("dev/INT_12345/20240101", "test-rf2-edition.zip");

        // then
        assertThrows(ModuleStorageCoordinatorException.DuplicateResourceException.class, () -> {
            moduleStorageCoordinatorDev.upload("INT", "12345", "20240101", getLocalFile("test-rf2-edition.zip"));
        });
    }

    @Test
    public void upload_ShouldThrowExpected_WhenUploadedMetadataCannotBeFound() throws IOException {
        // given
        ResourceManager resourceManagerMock = mock(ResourceManager.class);
        ModuleStorageCoordinator msc = new ModuleStorageCoordinator(resourceManagerMock, rf2Service, "dev", List.of("dev", "uat", "prod"), true);
        givenDoesObjectExist(resourceManagerMock, false, false, false, false);

        // then
        assertThrows(ModuleStorageCoordinatorException.OperationFailedException.class, () -> {
            msc.upload("INT", "12345", "20240101", getLocalFile("test-rf2-edition.zip"));
        });
    }

    @Test
    public void upload_ShouldThrowExpected_WhenUploadedRF2CannotBeFound() throws IOException {
        // given
        ResourceManager resourceManagerMock = mock(ResourceManager.class);
        ModuleStorageCoordinator msc = new ModuleStorageCoordinator(resourceManagerMock, rf2Service, "dev", List.of("dev", "uat", "prod"), true);
        givenDoesObjectExist(resourceManagerMock, false, false, true, false);

        // then
        assertThrows(ModuleStorageCoordinatorException.OperationFailedException.class, () -> {
            msc.upload("INT", "12345", "20240101", getLocalFile("test-rf2-edition.zip"));
        });
    }

    @Test
    public void upload_ShouldDeleteMetadata_WhenUploadedRF2CannotBeFound() throws IOException {
        // given
        ResourceManager resourceManagerMock = mock(ResourceManager.class);
        ModuleStorageCoordinator msc = new ModuleStorageCoordinator(resourceManagerMock, rf2Service, "dev", List.of("dev", "uat", "prod"), true);
        givenDoesObjectExist(resourceManagerMock, false, false, true, false);

        // then
        assertThrows(ModuleStorageCoordinatorException.OperationFailedException.class, () -> {
            msc.upload("INT", "12345", "20240101", getLocalFile("test-rf2-edition.zip"));
        });
        verify(resourceManagerMock).doDeleteResource(anyString());
    }

    @Test
    public void upload_ShouldNotThrow_WhenWritingToDifferentDirectories() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenArchive("prod/INT_12345/20240101", "test-rf2-edition.zip");

        // when
        moduleStorageCoordinatorDev.upload("INT", "12345", "20240101", getLocalFile("test-rf2-edition.zip"));
    }

    @Test
    public void upload_ShouldThrowExpected_WhenWritingToSameDirectories() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenArchive("dev/INT_12345/20240101", "test-rf2-edition.zip");

        // then
        assertThrows(ModuleStorageCoordinatorException.DuplicateResourceException.class, () -> {
            moduleStorageCoordinatorDev.upload("INT", "12345", "20240101", getLocalFile("test-rf2-edition.zip"));
        });
    }

    @Test
    public void upload_ShouldDoExpected_WhenUsingProfileDev() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        ModuleStorageCoordinator msc = ModuleStorageCoordinator.initDev(resourceManager);

        // when
        msc.upload("INT", "12345", "20240101", getLocalFile("test-rf2-edition.zip"));
        boolean existsOnDev = doDoesObjectExist("dev/INT_12345/20240101/test-rf2-edition.zip");
        boolean existsOnUat = doDoesObjectExist("uat/INT_12345/20240101/test-rf2-edition.zip");
        boolean existsOnProd = doDoesObjectExist("prod/INT_12345/20240101/test-rf2-edition.zip");

        // then
        assertTrue(existsOnDev);
        assertFalse(existsOnUat);
        assertFalse(existsOnProd);
    }

    @Test
    public void upload_ShouldDoExpected_WhenUsingProfileUat() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        ModuleStorageCoordinator msc = ModuleStorageCoordinator.initUat(resourceManager);

        // when
        msc.upload("INT", "12345", "20240101", getLocalFile("test-rf2-edition.zip"));
        boolean existsOnDev = doDoesObjectExist("dev/INT_12345/20240101/test-rf2-edition.zip");
        boolean existsOnUat = doDoesObjectExist("uat/INT_12345/20240101/test-rf2-edition.zip");
        boolean existsOnProd = doDoesObjectExist("prod/INT_12345/20240101/test-rf2-edition.zip");

        // then
        assertFalse(existsOnDev);
        assertTrue(existsOnUat);
        assertFalse(existsOnProd);
    }

    @Test
    public void upload_ShouldDoExpected_WhenUsingProfileProd() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        ModuleStorageCoordinator msc = ModuleStorageCoordinator.initProd(resourceManager);

        // when
        msc.upload("INT", "12345", "20240101", getLocalFile("test-rf2-edition.zip"));
        boolean existsOnDev = doDoesObjectExist("dev/INT_12345/20240101/test-rf2-edition.zip");
        boolean existsOnUat = doDoesObjectExist("uat/INT_12345/20240101/test-rf2-edition.zip");
        boolean existsOnProd = doDoesObjectExist("prod/INT_12345/20240101/test-rf2-edition.zip");

        // then
        assertFalse(existsOnDev);
        assertFalse(existsOnUat);
        assertTrue(existsOnProd);
    }

    @Test
    public void generateMetadata_ShouldThrowExpected_WhenGivenInvalidCodeSystem() {
        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.generateMetadata(" ", "12345", "20240101", FileUtils.doCreateTempFile("tmp.zip"));
        });
    }

    @Test
    public void generateMetadata_ShouldThrowExpected_WhenGivenInvalidModuleId() {
        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.generateMetadata("INT", " ", "20240101", FileUtils.doCreateTempFile("tmp.zip"));
        });
    }

    @Test
    public void generateMetadata_ShouldThrowExpected_WhenGivenInvalidEffectiveTime() {
        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.generateMetadata("INT", "12345", "2024-01-01", FileUtils.doCreateTempFile("tmp.zip"));
        });
    }

    @Test
    public void generateMetadata_ShouldThrowExpected_WhenGivenInvalidFile() {
        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.generateMetadata(" ", "12345", "20240101", null);
        });
    }

    @Test
    public void generateMetadata_ShouldReturnExpectedFilename() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        File localFile = getLocalFile("test-rf2-edition.zip");

        // when
        ModuleMetadata result = moduleStorageCoordinatorDev.generateMetadata("INT", "12345", "20240101", localFile);

        // then
        assertEquals("test-rf2-edition.zip", result.getFilename());
    }

    @Test
    public void generateMetadata_ShouldReturnExpectedCodeSystemShortName() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        File localFile = getLocalFile("test-rf2-edition.zip");

        // when
        ModuleMetadata result = moduleStorageCoordinatorDev.generateMetadata("INT", "12345", "20240101", localFile);

        // then
        assertEquals("INT", result.getCodeSystemShortName());
    }

    @Test
    public void generateMetadata_ShouldReturnExpectedIdentifyingModuleId() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        File localFile = getLocalFile("test-rf2-edition.zip");

        // when
        ModuleMetadata result = moduleStorageCoordinatorDev.generateMetadata("INT", "12345", "20240101", localFile);

        // then
        assertEquals("12345", result.getIdentifyingModuleId());
    }

    @Test
    public void generateMetadata_ShouldReturnExpectedCompositionModuleIds() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        List<String> expectedCompositionModuleIds = List.of("900000000000012004", "900000000000207008");
        File localFile = getLocalFile("test-rf2-edition.zip");

        // when
        ModuleMetadata result = moduleStorageCoordinatorDev.generateMetadata("INT", "12345", "20240101", localFile);

        // then
        assertEquals(expectedCompositionModuleIds, result.getCompositionModuleIds());
    }

    @Test
    public void generateMetadata_ShouldReturnExpectedEffectiveTime() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        File localFile = getLocalFile("test-rf2-edition.zip");

        // when
        ModuleMetadata result = moduleStorageCoordinatorDev.generateMetadata("INT", "12345", "20240101", localFile);

        // then
        assertEquals(20240101, result.getEffectiveTime());
    }

    @Test
    public void generateMetadata_ShouldReturnExpectedFileTimestamp() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        File localFile = getLocalFile("test-rf2-edition.zip");

        // when
        ModuleMetadata result = moduleStorageCoordinatorDev.generateMetadata("INT", "12345", "20240101", localFile);

        // then
        assertNotNull(result.getFileTimeStamp());
        assertTrue(new Date().after(result.getFileTimeStamp()));
    }

    @Test
    public void generateMetadata_ShouldReturnExpectedMD5() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        File localFile = getLocalFile("test-rf2-edition.zip");

        // when
        ModuleMetadata result = moduleStorageCoordinatorDev.generateMetadata("INT", "12345", "20240101", localFile);

        // then
        // Expected value will change anytime test-rf2-edition.zip is modified.
        assertEquals("7082895c88eda5ad2985edd5054ad829", result.getFileMD5());
    }

    @Test
    public void generateMetadata_ShouldReturnExpectedPublished() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        File localFile = getLocalFile("test-rf2-edition.zip");

        // when
        ModuleMetadata result = moduleStorageCoordinatorDev.generateMetadata("INT", "12345", "20240101", localFile);

        // then
        assertFalse(result.getPublished());
    }

    @Test
    public void generateMetadata_ShouldReturnExpectedEdition() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        File localFile = getLocalFile("test-rf2-edition.zip");

        // when
        ModuleMetadata result = moduleStorageCoordinatorDev.generateMetadata("INT", "12345", "20240101", localFile);

        // then
        assertTrue(result.getEdition());
    }

    @Test
    public void generateMetadata_ShouldReturnExpectedDependencies_WhenGivenEdition() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        File localFile = getLocalFile("test-rf2-edition.zip");

        // when
        ModuleMetadata result = moduleStorageCoordinatorDev.generateMetadata("INT", "12345", "20240101", localFile);

        // then
        assertTrue(result.getDependencies().isEmpty());
    }

    @Test
    public void generateMetadata_ShouldThrowExpected_WhenGivenExtensionWithMissingDependency() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));

        // then
        assertThrows(ModuleStorageCoordinatorException.ResourceNotFoundException.class, () -> {
            moduleStorageCoordinatorDev.generateMetadata("XX", "3182250003", "20240105", getLocalFile("test-rf2-extension.zip"));
        });
    }

    @Test
    public void generateMetadata_ShouldReturnExpectedDependencies_WhenGivenExtensionWithCommon() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("YY", "3191250003", "20240103", getLocalFile("test-rf2-common.zip"));

        // when
        ModuleMetadata result = moduleStorageCoordinatorDev.generateMetadata("XX", "3182250003", "20240105", getLocalFile("test-rf2-extension.zip"));

        // then
        assertFalse(result.getDependencies().isEmpty());
        assertEquals(2, result.getDependencies().size());
        assertEquals("INT", result.getDependencies().get(0).getCodeSystemShortName());
        assertEquals("YY", result.getDependencies().get(1).getCodeSystemShortName());
    }

    @Test
    public void getMetadata_ShouldThrowExpected_WhenGivenNoCodeSystem() {
        // given
        String codeSystem = null;
        String moduleId = "12345";
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void getMetadata_ShouldThrowExpected_WhenGivenNoModuleId() {
        // given
        String codeSystem = "INT";
        String moduleId = null;
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void getMetadata_ShouldThrowExpected_WhenGivenNoEffectiveTime() {
        // given
        String codeSystem = "INT";
        String moduleId = "12345";
        String effectiveTime = null;

        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void getMetadata_ShouldThrowExpected_WhenNoReleaseFound() {
        // given
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.ResourceNotFoundException.class, () -> {
            moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void getMetadata_ShouldThrowExpected_WhenExistingMetadataMalformed() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenArchive("prod/INT_900000000000012004/20240101", "metadata.json"); // Malformed as blank file
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.OperationFailedException.class, () -> {
            moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void getMetadata_ShouldThrowExpected_WhenOnlyFoundMetadata() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenMetadata("prod/INT_900000000000012004/20240101", "test.zip");
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.ResourceNotFoundException.class, () -> {
            moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void getMetadata_ShouldThrowExpected_WhenUnrelatedPackageExists() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        String codeSystem = "YY";
        String moduleId = "3191250003";
        String effectiveTime = "20240103";

        // then
        assertThrows(ModuleStorageCoordinatorException.ResourceNotFoundException.class, () -> {
            moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void getMetadata_ShouldReturnExpected_WhenPackageExists() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // when
        ModuleMetadata result = moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime);

        // then
        assertEquals("test-rf2-edition.zip", result.getFilename());
    }

    @Test
    public void getMetadata_ShouldReturnExpected_WhenIncludingFile() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // when
        ModuleMetadata result = moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime, true);

        // then
        assertNotNull(result.getFile());
    }

    @Test
    public void getMetadata_ShouldReturnExpected_WhenNotIncludingFile() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // when
        ModuleMetadata result = moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime, false);

        // then
        assertNull(result.getFile());
    }

    @Test
    public void getMetadata_ShouldReturnExpected_WhenDevOverwrittenProd() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenMetadata("dev/INT_900000000000012004/20240101", "test-rf2.zip");
        givenArchive("dev/INT_900000000000012004/20240101", "test-rf2.zip");

        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // when
        ModuleMetadata result = moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime);

        // then
        assertEquals("test-rf2.zip", result.getFilename()); // Dev package is different (doesn't have suffix in this example)
    }

    @Test
    public void getMetadata_ShouldUseCache_WhenEnabled() throws ScriptException, IOException, ModuleStorageCoordinatorException, IOException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenMetadata("dev/INT_900000000000012004/20240101", "test-rf2.zip");
        ResourceManager resourceManagerCacheMock = mock(ResourceManager.class);
        ModuleStorageCoordinator msc = new ModuleStorageCoordinator(resourceManager, resourceManagerCacheMock, rf2Service, "dev", List.of("dev", "uat", "prod"), true);

        // when
        msc.getMetadata("INT", "900000000000012004", "20240101", true);

        // then
        verify(resourceManagerCacheMock).doReadResourceFile(anyString());
    }

    @Test
    public void archive_ShouldThrowExpected_WhenGivenNoCodeSystem() {
        // given
        String codeSystem = null;
        String moduleId = "12345";
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.archive(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void archive_ShouldThrowExpected_WhenGivenNoModuleId() {
        // given
        String codeSystem = "INT";
        String moduleId = null;
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.archive(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void archive_ShouldThrowExpected_WhenGivenNoEffectiveTime() {
        // given
        String codeSystem = "INT";
        String moduleId = "12345";
        String effectiveTime = null;

        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.archive(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void archive_ShouldThrowExpected_WhenArchivingProd() {
        // given
        String codeSystem = "INT";
        String moduleId = "12345";
        String effectiveTime = "20240101";
        ModuleStorageCoordinator msc = ModuleStorageCoordinator.initProd(resourceManager);

        // then
        assertThrows(ModuleStorageCoordinatorException.class, () -> {
            msc.archive(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void archive_ShouldThrowExpected_WhenMetadataDoesntExist() {
        // given
        String codeSystem = "INT";
        String moduleId = "12345";
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.ResourceNotFoundException.class, () -> {
            moduleStorageCoordinatorDev.archive(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void archive_ShouldThrowExpected_WhenExistingMetadataIsMalformed() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenArchive("dev/INT_900000000000012004/20240101", "metadata.json"); // Malformed as blank file
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.ResourceNotFoundException.class, () -> {
            moduleStorageCoordinatorDev.archive(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void archive_ShouldThrowExpected_WhenPackageDoesntExist() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenMetadata("dev/INT_900000000000012004/20240101", "test-rf2-edition.zip"); // Package not uploaded
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.ResourceNotFoundException.class, () -> {
            moduleStorageCoordinatorDev.archive(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void archive_ShouldThrowNothing_WhenFilesExist() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        moduleStorageCoordinatorDev.upload("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // when
        moduleStorageCoordinatorDev.archive(codeSystem, moduleId, effectiveTime);
    }

    @Test
    public void archive_ShouldDeleteOriginal_WhenArchivingSuccessful() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        moduleStorageCoordinatorDev.upload("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        moduleStorageCoordinatorDev.archive("INT", "900000000000012004", "20240101");

        // when
        Set<String> source = doListfilenames("dev/INT_900000000000012004/20240101");
        Set<String> destination = doListfilenames("dev/INT_900000000000012004/archive");

        // then
        assertEquals(0, source.size()); // Originals deleted
        assertEquals(2, destination.size()); // Originals copied here
    }

    @Test
    public void getRelease_ShouldThrowExpected_WhenGivenNoCodeSystem() {
        // given
        String codeSystem = null;
        String moduleId = "900000000000012004";
        String effectiveTime = "20240105";

        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.getRelease(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void getRelease_ShouldThrowExpected_WhenGivenNoModuleId() {
        // given
        String codeSystem = "INT";
        String moduleId = null;
        String effectiveTime = "20240105";

        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.getRelease(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void getRelease_ShouldThrowExpected_WhenGivenNoEffectiveTime() {
        // given
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = null;

        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.getRelease(codeSystem, moduleId, effectiveTime);
        });
    }

    @Test
    public void getRelease_ShouldThrowExpected_WhenQueryingNonExistentPackage() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));

        // then
        assertThrows(ModuleStorageCoordinatorException.ResourceNotFoundException.class, () -> {
            moduleStorageCoordinatorDev.getRelease("XX", "3182250003", "20240105");
        });
    }

    @Test
    public void getRelease_ShouldReturnExpected_WhenQueryingEdition() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("YY", "3191250003", "20240103", getLocalFile("test-rf2-common.zip"));
        givenProdReleasePackage("XX", "3182250003", "20240105", getLocalFile("test-rf2-extension.zip"));

        // when
        List<ModuleMetadata> moduleMetadatas = moduleStorageCoordinatorDev.getRelease("INT", "900000000000012004", "20240101");

        // then
        assertEquals(1, moduleMetadatas.size());
        assertEquals("INT", moduleMetadatas.get(0).getCodeSystemShortName());
        assertEquals(20240101, moduleMetadatas.get(0).getEffectiveTime());
    }

    @Test
    public void getRelease_ShouldReturnExpected_WhenQueryingCommon() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("YY", "3191250003", "20240103", getLocalFile("test-rf2-common.zip"));
        givenProdReleasePackage("XX", "3182250003", "20240105", getLocalFile("test-rf2-extension.zip"));

        // when
        List<ModuleMetadata> moduleMetadatas = moduleStorageCoordinatorDev.getRelease("YY", "3191250003", "20240103");

        // then
        assertEquals(2, moduleMetadatas.size());
        assertEquals("INT", moduleMetadatas.get(0).getCodeSystemShortName());
        assertEquals(20240101, moduleMetadatas.get(0).getEffectiveTime());
        assertEquals("YY", moduleMetadatas.get(1).getCodeSystemShortName());
        assertEquals(20240103, moduleMetadatas.get(1).getEffectiveTime());
    }

    @Test
    public void getRelease_ShouldReturnExpected_WhenQueryingExtension() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("YY", "3191250003", "20240103", getLocalFile("test-rf2-common.zip"));
        givenProdReleasePackage("XX", "3182250003", "20240105", getLocalFile("test-rf2-extension.zip"));

        // when
        List<ModuleMetadata> moduleMetadatas = moduleStorageCoordinatorDev.getRelease("XX", "3182250003", "20240105");

        // then
        assertEquals(3, moduleMetadatas.size());
        assertEquals("INT", moduleMetadatas.get(0).getCodeSystemShortName());
        assertEquals(20240101, moduleMetadatas.get(0).getEffectiveTime());
        assertEquals("YY", moduleMetadatas.get(1).getCodeSystemShortName());
        assertEquals(20240103, moduleMetadatas.get(1).getEffectiveTime());
        assertEquals("XX", moduleMetadatas.get(2).getCodeSystemShortName());
        assertEquals(20240105, moduleMetadatas.get(2).getEffectiveTime());
    }

    @Test
    public void setPublished_ShouldThrowExpected_WhenGivenNoCodeSystem() {
        // given
        String codeSystem = null;
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.setPublished(codeSystem, moduleId, effectiveTime, true);
        });
    }

    @Test
    public void setPublished_ShouldThrowExpected_WhenGivenNoModuleId() {
        // given
        String codeSystem = "INT";
        String moduleId = null;
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.setPublished(codeSystem, moduleId, effectiveTime, true);
        });
    }

    @Test
    public void setPublished_ShouldThrowExpected_WhenGivenNoEffectiveTime() {
        // given
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = null;

        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.setPublished(codeSystem, moduleId, effectiveTime, true);
        });
    }

    @Test
    public void setPublished_ShouldThrowExpected_WhenMetadataNonExistent() {
        // given
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.ResourceNotFoundException.class, () -> {
            moduleStorageCoordinatorDev.setPublished(codeSystem, moduleId, effectiveTime, true);
        });
    }

    @Test
    public void setPublished_ShouldDoExpected_WhenMetadataExistent() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenDevReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // then
        moduleStorageCoordinatorDev.setPublished(codeSystem, moduleId, effectiveTime, true);
    }

    @Test
    public void setPublished_ShouldUpdateProperty_WhenSuccessful() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";
        ModuleMetadata moduleMetadata;

        // Upload package
        givenDevReleasePackage(codeSystem, moduleId, effectiveTime, getLocalFile("test-rf2-edition.zip"));

        // Assert uploaded package is not published by default
        moduleMetadata = moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime, false);
        assertFalse(moduleMetadata.getPublished());

        // Update package
        moduleStorageCoordinatorDev.setPublished(codeSystem, moduleId, effectiveTime, true);

        // Assert uploaded package is published after update
        moduleMetadata = moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime, false);
        assertTrue(moduleMetadata.getPublished());
    }

    @Test
    public void setPublished_ShouldNotUpdateProperty_WhenNotSuccessful() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";
        ModuleMetadata moduleMetadata;

        // Upload package
        givenDevReleasePackage(codeSystem, moduleId, effectiveTime, getLocalFile("test-rf2-edition.zip"));

        // Assert uploaded package is not published by default
        moduleMetadata = moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime, false);
        assertFalse(moduleMetadata.getPublished());

        // Fail to update package (no effectiveTime)
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.setPublished(codeSystem, moduleId, null, true);
        });

        // Assert uploaded package is still not published after failed update
        moduleMetadata = moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime, false);
        assertFalse(moduleMetadata.getPublished());
    }

    @Test
    public void setPublished_ShouldThrowExpected_WhenDevTryingToUpdateProd() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.OperationFailedException.class, () -> {
            moduleStorageCoordinatorDev.setPublished(codeSystem, moduleId, effectiveTime, true);
        });
    }

    @Test
    public void setEdition_ShouldThrowExpected_WhenGivenNoCodeSystem() {
        // given
        String codeSystem = null;
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.setEdition(codeSystem, moduleId, effectiveTime, true);
        });
    }

    @Test
    public void setEdition_ShouldThrowExpected_WhenGivenNoModuleId() {
        // given
        String codeSystem = "INT";
        String moduleId = null;
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.setEdition(codeSystem, moduleId, effectiveTime, true);
        });
    }

    @Test
    public void setEdition_ShouldThrowExpected_WhenGivenNoEffectiveTime() {
        // given
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = null;

        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.setEdition(codeSystem, moduleId, effectiveTime, true);
        });
    }

    @Test
    public void setEdition_ShouldThrowExpected_WhenMetadataNonExistent() {
        // given
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.ResourceNotFoundException.class, () -> {
            moduleStorageCoordinatorDev.setEdition(codeSystem, moduleId, effectiveTime, true);
        });
    }

    @Test
    public void setEdition_ShouldThrowNothing_WhenMetadataExistent() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenDevReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // then
        moduleStorageCoordinatorDev.setEdition(codeSystem, moduleId, effectiveTime, true);
    }

    @Test
    public void setEdition_ShouldUpdateProperty_WhenSuccessful() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";
        ModuleMetadata moduleMetadata;

        // Upload package
        givenDevReleasePackage(codeSystem, moduleId, effectiveTime, getLocalFile("test-rf2-edition.zip"));

        // Assert uploaded package is edition by default
        moduleMetadata = moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime, false);
        assertTrue(moduleMetadata.getEdition());

        // Update package
        moduleStorageCoordinatorDev.setEdition(codeSystem, moduleId, effectiveTime, false);

        // Assert uploaded package is edition after update
        moduleMetadata = moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime, false);
        assertFalse(moduleMetadata.getEdition());
    }

    @Test
    public void setEdition_ShouldNotUpdateProperty_WhenNotSuccessful() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";
        ModuleMetadata moduleMetadata;

        // Upload package
        givenDevReleasePackage(codeSystem, moduleId, effectiveTime, getLocalFile("test-rf2-edition.zip"));

        // Assert uploaded package is edition by default
        moduleMetadata = moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime, false);
        assertTrue(moduleMetadata.getEdition());

        // Fail to update package (no effectiveTime)
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.setEdition(codeSystem, moduleId, null, false);
        });

        // Assert uploaded package is still edition after failed update
        moduleMetadata = moduleStorageCoordinatorDev.getMetadata(codeSystem, moduleId, effectiveTime, false);
        assertTrue(moduleMetadata.getEdition());
    }

    @Test
    public void setEdition_ShouldThrowExpected_WhenDevTryingToUpdateProd() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        String codeSystem = "INT";
        String moduleId = "900000000000012004";
        String effectiveTime = "20240101";

        // then
        assertThrows(ModuleStorageCoordinatorException.OperationFailedException.class, () -> {
            moduleStorageCoordinatorDev.setEdition(codeSystem, moduleId, effectiveTime, true);
        });
    }

    @Test
    public void getAllReleases_ShouldReturnExpected_WhenNoPackages() throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException {
        // when
        Map<String, List<ModuleMetadata>> result = moduleStorageCoordinatorDev.getAllReleases();

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    public void getAllReleases_ShouldReturnExpected_WhenEditionPackage() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));

        // when
        Map<String, List<ModuleMetadata>> releases = moduleStorageCoordinatorDev.getAllReleases();

        // then
        assertEquals(1, releases.size());
        assertNotNull(releases.get("INT"));
        assertEquals(1, releases.get("INT").size());
    }

    @Test
    public void getAllReleases_ShouldReturnExpected_WhenEditionPackageWithMultipleVersions() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240201", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240301", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240401", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240501", getLocalFile("test-rf2-edition.zip"));

        // when
        Map<String, List<ModuleMetadata>> releases = moduleStorageCoordinatorDev.getAllReleases();

        // then
        assertEquals(1, releases.size());
        assertNotNull(releases.get("INT"));
        assertEquals(5, releases.get("INT").size());
    }

    @Test
    public void getAllReleases_ShouldReturnExpected_WhenDevOverwritesProd() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenMetadata("dev/INT_900000000000012004/20240101", "test-rf2.zip");
        givenArchive("dev/INT_900000000000012004/20240101", "test-rf2.zip");
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));

        // when
        Map<String, List<ModuleMetadata>> releases = moduleStorageCoordinatorDev.getAllReleases();

        // then
        assertEquals(1, releases.size());
        assertNotNull(releases.get("INT"));
        assertEquals(1, releases.get("INT").size());
        assertEquals("test-rf2.zip", releases.get("INT").get(0).getFilename()); // In this example, Dev doesn't have -edition suffix
    }

    @Test
    public void getAllReleases_ShouldReturnExpected_WhenMultipleCodeSystems() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("YY", "3191250003", "20240103", getLocalFile("test-rf2-common.zip"));
        givenProdReleasePackage("XX", "3182250003", "20240105", getLocalFile("test-rf2-extension.zip"));

        // when
        Map<String, List<ModuleMetadata>> releases = moduleStorageCoordinatorDev.getAllReleases();

        // then
        assertEquals(3, releases.size());
        assertNotNull(releases.get("INT"));
        assertNotNull(releases.get("YY"));
        assertNotNull(releases.get("XX"));
        assertEquals(1, releases.get("INT").size());
        assertEquals(1, releases.get("YY").size());
        assertEquals(1, releases.get("XX").size());
    }

    @Test
    public void getAllReleases_ShouldReturnExpected_WhenRogueJsonFile() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenArchive("dev", "cache.json"); // Fictional later requirement adds new file in hierarchy

        // when
        Map<String, List<ModuleMetadata>> releases = moduleStorageCoordinatorDev.getAllReleases();

        // then
        assertFalse(releases.isEmpty());
        assertEquals(1, releases.size());
        assertEquals(1, releases.get("INT").size());
    }

    @Test
    public void getAllReleases_ShouldReturnExpected_WhenRogueTestPackage() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenMetadata("dev/INT_900000000000012004/20240201", "test-rf2.zip");
        givenArchive("dev/INT_900000000000012004/20240201", "test-rf2.zip");

        // Path ignored
        givenMetadata("dev/wip/INT_900000000000012004/20240201", "test-rf2.zip");
        givenArchive("dev/wip/INT_900000000000012004/20240201", "test-rf2.zip");

        givenMetadata("prod/INT_900000000000012004/20240101", "test-rf2.zip");
        givenArchive("prod/INT_900000000000012004/20240101", "test-rf2.zip");

        // when
        Map<String, List<ModuleMetadata>> releases = moduleStorageCoordinatorDev.getAllReleases();

        // then
        assertFalse(releases.isEmpty());
        assertEquals(1, releases.size());
        assertEquals(2, releases.get("INT").size());
    }

    @Test
    public void getAllReleases_ShouldThrowExpected_WhenGivenInvalidCodeSystem(){
        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.getAllReleases(null);
        });
    }

    @Test
    public void getAllReleases_ShouldThrowExpected_WhenNoData(){
        // then
        assertThrows(ModuleStorageCoordinatorException.ResourceNotFoundException.class, () -> {
            moduleStorageCoordinatorDev.getAllReleases("INT");
        });
    }

    @Test
    public void getAllReleases_ShouldThrowExpected_WhenNoMatch() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));

        // then
        assertThrows(ModuleStorageCoordinatorException.ResourceNotFoundException.class, () -> {
            moduleStorageCoordinatorDev.getAllReleases("XX");
        });
    }

    @Test
    public void getAllReleases_ShouldReturnExpected_WhenCodeSystemHasMatch() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));

        // when
        List<ModuleMetadata> releases = moduleStorageCoordinatorDev.getAllReleases("INT");

        // then
        assertEquals(1, releases.size());
        assertEquals("INT", releases.get(0).getCodeSystemShortName());
    }

    @Test
    public void getAllReleases_ShouldReturnExpected_WhenCodeSystemHasMultipleMatch() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240201", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240301", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240401", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240501", getLocalFile("test-rf2-edition.zip"));

        // when
        List<ModuleMetadata> releases = moduleStorageCoordinatorDev.getAllReleases("INT");

        // then
        assertEquals(5, releases.size());
        for (ModuleMetadata release : releases) {
            assertEquals("INT", release.getCodeSystemShortName());
        }
    }

    @Test
    public void getAllReleases_ShouldReturnExpected_WhenCodeSystemMatch() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));

        // when
        List<ModuleMetadata> releases = moduleStorageCoordinatorDev.getAllReleases("INT");

        // then
        assertEquals(1, releases.size());
        assertEquals("INT", releases.get(0).getCodeSystemShortName());
        assertNull(releases.get(0).getFile());
    }

    @Test
    public void getCodeSystems_ShouldReturnExpected_WhenNoData() throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException {
        // when
        List<String> codeSystems = moduleStorageCoordinatorDev.getCodeSystems();

        // then
        assertTrue(codeSystems.isEmpty());
    }

    @Test
    public void getCodeSystems_ShouldReturnExpected_WhenCodeSystemExists() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));

        // when
        List<String> codeSystems = moduleStorageCoordinatorDev.getCodeSystems();

        // then
        assertEquals(1, codeSystems.size());
        assertEquals("INT", codeSystems.get(0));
    }

    @Test
    public void getCodeSystems_ShouldReturnExpected_WhenCodeSystemExistsWithMultipleVersions() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240201", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240301", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240401", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240501", getLocalFile("test-rf2-edition.zip"));

        // when
        List<String> codeSystems = moduleStorageCoordinatorDev.getCodeSystems();

        // then
        assertEquals(1, codeSystems.size());
        assertEquals("INT", codeSystems.get(0));
    }

    @Test
    public void getCodeSystems_ShouldReturnExpected_WhenMultipleCodeSystemWithMultipleVersions() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240201", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240301", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240401", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240501", getLocalFile("test-rf2-edition.zip"));

        givenProdReleasePackage("YY", "3191250003", "20240103", getLocalFile("test-rf2-common.zip"));
        givenProdReleasePackage("YY", "3191250003", "20240203", getLocalFile("test-rf2-common.zip"));
        givenProdReleasePackage("YY", "3191250003", "20240303", getLocalFile("test-rf2-common.zip"));
        givenProdReleasePackage("YY", "3191250003", "20240403", getLocalFile("test-rf2-common.zip"));
        givenProdReleasePackage("YY", "3191250003", "20240503", getLocalFile("test-rf2-common.zip"));

        // when
        List<String> codeSystems = moduleStorageCoordinatorDev.getCodeSystems();

        // then
        assertEquals(2, codeSystems.size());
        for (String codeSystem : codeSystems) {
            boolean matchInt = Objects.equals("INT", codeSystem);
            boolean matchYY = Objects.equals("YY", codeSystem);
            assertTrue(matchInt || matchYY);
        }
    }

    @Test
    public void getCodeSystems_ShouldReturnExpectedOrdered_WhenMultipleCodeSystems() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("YY", "3191250003", "20240103", getLocalFile("test-rf2-common.zip"));

        // when
        List<String> codeSystems = moduleStorageCoordinatorDev.getCodeSystems();

        // then
        assertEquals(2, codeSystems.size());
        assertEquals("INT", codeSystems.get(0));
        assertEquals("YY", codeSystems.get(1));
    }

    @Test
    public void getCodeSystems_ShouldReturnExpected_WhenMultipleCodeSystemsAcrossEnvironments() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("A", "12345", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenDevReleasePackage("B", "678910", "20240101", getLocalFile("test-rf2-edition.zip"));

        // when
        List<String> codeSystems = moduleStorageCoordinatorDev.getCodeSystems();

        // then
        assertEquals(2, codeSystems.size());
        assertEquals("A", codeSystems.get(0));
        assertEquals("B", codeSystems.get(1));
    }

    @Test
    public void getReleaseDates_ShouldThrowExpected_WhenGivenNoCodeSystem() {
        // then
        assertThrows(ModuleStorageCoordinatorException.InvalidArgumentsException.class, () -> {
            moduleStorageCoordinatorDev.getReleaseDates(null);
        });
    }

    @Test
    public void getReleaseDates_ShouldThrowExpected_WhenCodeSystemNotFound() {
        // then
        assertThrows(ModuleStorageCoordinatorException.ResourceNotFoundException.class, () -> {
            moduleStorageCoordinatorDev.getReleaseDates("INT");
        });
    }

    @Test
    public void getReleaseDates_ShouldThrowExpected_WhenCodeSystemNotFoundWhenDataExists() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("A", "12345", "20240101", getLocalFile("test-rf2-edition.zip"));

        // then
        assertThrows(ModuleStorageCoordinatorException.ResourceNotFoundException.class, () -> {
            moduleStorageCoordinatorDev.getReleaseDates("XX");
        });
    }

    @Test
    public void getReleaseDates_ShouldReturnExpected_WhenCodeSystemFound() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));

        // when
        List<Integer> releaseDates = moduleStorageCoordinatorDev.getReleaseDates("INT");

        // then
        assertEquals(1, releaseDates.size());
        assertEquals(20240101, releaseDates.get(0));
    }

    @Test
    public void getReleaseDates_ShouldReturnExpected_WhenCodeSystemHasMultipleVersions() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240201", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240301", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240401", getLocalFile("test-rf2-edition.zip"));
        givenProdReleasePackage("INT", "900000000000012004", "20240501", getLocalFile("test-rf2-edition.zip"));

        // when
        List<Integer> releaseDates = moduleStorageCoordinatorDev.getReleaseDates("INT");

        // then
        assertEquals(5, releaseDates.size());
        assertEquals(20240101, releaseDates.get(4));
        assertEquals(20240201, releaseDates.get(3));
        assertEquals(20240301, releaseDates.get(2));
        assertEquals(20240401, releaseDates.get(1));
        assertEquals(20240501, releaseDates.get(0));
    }

    @Test
    public void getReleaseDates_ShouldReturnExpected_WhenCodeSystemHasMultipleVersionsAcrossEnvironments() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenDevReleasePackage("INT", "900000000000012004", "20240201", getLocalFile("test-rf2-edition.zip"));

        // when
        List<Integer> releaseDates = moduleStorageCoordinatorDev.getReleaseDates("INT");

        // then
        assertEquals(2, releaseDates.size());
        assertEquals(20240101, releaseDates.get(1));
        assertEquals(20240201, releaseDates.get(0));
    }

    @Test
    public void getReleaseDates_ShouldReturnExpected_WhenDuplicateCodeSystem() throws ScriptException, IOException, ModuleStorageCoordinatorException {
        // given
        givenProdReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));
        givenDevReleasePackage("INT", "900000000000012004", "20240101", getLocalFile("test-rf2-edition.zip"));

        // when
        List<Integer> releaseDates = moduleStorageCoordinatorDev.getReleaseDates("INT");

        // then
        assertEquals(1, releaseDates.size());
        assertEquals(20240101, releaseDates.get(0));
    }

    private Set<String> doListfilenames(String prefix) {
        try {
            return resourceManager.listFilenames(prefix);
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    private void givenProdReleasePackage(String codeSystem, String moduleId, String effectiveTime, File rf2Package) throws ScriptException, IOException, ModuleStorageCoordinatorException {
        moduleStorageCoordinatorProd.upload(codeSystem, moduleId, effectiveTime, rf2Package);
    }

    private void givenDevReleasePackage(String codeSystem, String moduleId, String effectiveTime, File rf2Package) throws ScriptException, IOException, ModuleStorageCoordinatorException {
        moduleStorageCoordinatorDev.upload(codeSystem, moduleId, effectiveTime, rf2Package);
    }

    private void givenArchive(String folderName, String fileName) throws ScriptException, IOException, ModuleStorageCoordinatorException, IOException {
        String resourcePath = folderName + "/" + fileName;
        File tempFile = FileUtils.doCreateTempFile(fileName);
        FileInputStream fileInputStream = asFileInputStream(tempFile);

        // Upload to S3
        doWriteResource(resourcePath, fileInputStream);

        // Download from S3
        InputStream inputStream = doReadResource(resourcePath);
        assertNotNull(inputStream);
    }

    private void givenMetadata(String resourcePath, String rf2PackageName) throws ScriptException, IOException, ModuleStorageCoordinatorException {
        ModuleMetadata moduleMetadata = getMetadataFromPath(resourcePath);
        moduleMetadata.setFilename(rf2PackageName);

        File file = FileUtils.doCreateTempFile("metadata.json");
        FileUtils.writeToFile(file, moduleMetadata);
        doWriteResource(resourcePath + "/metadata.json", asFileInputStream(file));
    }

    private ModuleMetadata getMetadataFromPath(String resourcePath) {
        String[] parts = resourcePath.split("[/_]");

        ModuleMetadata mm = new ModuleMetadata()
                .withCodeSystemShortName(parts[1])
                .withIdentifyingModuleId(parts[2]);
        //There is a test with a rogue path, so don't add the effective time if it has not parsed corrected
        if (parts[3].length() == 8) {
            mm.setEffectiveTime(Integer.parseInt(parts[3]));
        }
        return mm;
    }

    private FileInputStream asFileInputStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Cannot create file input stream.", e);
        }
    }

    private void doWriteResource(String resourcePath, InputStream resourceInputStream) throws ModuleStorageCoordinatorException {
        try {
            resourceManager.writeResource(resourcePath, resourceInputStream);
        } catch (IOException e) {
            throw new ModuleStorageCoordinatorException("Cannot write resource: " + resourcePath, e);
        }
    }

    private InputStream doReadResource(String resourcePath) throws ModuleStorageCoordinatorException {
        try {
            return resourceManager.readResourceStream(resourcePath);
        } catch (IOException e) {
            throw new ModuleStorageCoordinatorException("Cannot read resource: " + resourcePath, e);
        }
    }

    private File getLocalFile(String fileName) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:" + fileName);

            if (resources.length > 0) {
                return resources[0].getFile();
            } else {
                throw new RuntimeException("Multiple files with name found: " + fileName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot load file: " + fileName, e);
        }
    }

    private boolean doDoesObjectExist(String resourcePath) {
        return resourceManager.doesObjectExist(resourcePath);
    }

    private void givenDoesObjectExist(ResourceManager resourceManager, Boolean bool1, Boolean bool2, Boolean bool3, Boolean bool4) throws IOException {
        when(resourceManager.doesObjectExist(anyString())).thenReturn(bool1).thenReturn(bool2).thenReturn(bool3).thenReturn(bool4);
    }
}