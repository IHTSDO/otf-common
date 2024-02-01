package org.snomed.module.storage;

import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.script.dao.SimpleStorageResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class IntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);
    private static final String BUCKET_NAME = "otf-common";
    private static final String BUCKET_PATH = "files";

    @Container
    public static LocalStackContainer localStackContainer = new TestLocalStackContainer();

    protected final S3Client s3Client;
    protected final ResourceLoader resourceLoader;
    protected final ResourceConfiguration resourceConfiguration;
    protected final ResourceManager resourceManager;
    protected final S3ClientWrapper s3ClientWrapper;
    protected final ModuleStorageCoordinator moduleStorageCoordinatorDev;
    protected final ModuleStorageCoordinator moduleStorageCoordinatorProd;
    protected final RF2Service rf2Service;

    public IntegrationTest() {
        this.rf2Service = new RF2Service();
        this.s3Client = s3Client();
        this.resourceLoader = resourceLoader();
        this.resourceConfiguration = resourceConfiguration();
        this.resourceManager = resourceManager();
        this.s3ClientWrapper = new S3ClientWrapper(s3Client);
        this.moduleStorageCoordinatorDev = ModuleStorageCoordinator.initDev(resourceManager);
        this.moduleStorageCoordinatorProd = ModuleStorageCoordinator.initProd(resourceManager);
    }

    @BeforeEach
    public void setUp() {
        LOGGER.info("Setting up before test...");

        LOGGER.info("Creating bucket {}", BUCKET_NAME);
        boolean success = s3ClientWrapper.createBucket(BUCKET_NAME);
        assertTrue(success);

        List<Bucket> buckets = s3ClientWrapper.readBuckets();
        assertEquals(1, buckets.size());
    }

    @AfterEach
    public void tearDown() {
        LOGGER.info("Tearing down after test...");
        // Delete all data
        List<Bucket> buckets = s3ClientWrapper.readBuckets();
        for (Bucket bucket : buckets) {
            String bucketName = bucket.name();
            List<String> objectKeys = s3ClientWrapper.readObjectKeys(bucketName);
            for (String objectKey : objectKeys) {
                LOGGER.info("Deleting object {}", objectKey);
                s3ClientWrapper.deleteObject(bucketName, objectKey);
            }

            objectKeys = s3ClientWrapper.readObjectKeys(bucketName);
            assertEquals(0, objectKeys.size());

            LOGGER.info("Deleting bucket {}", bucketName);
            s3ClientWrapper.deleteBucket(bucketName);
        }

        buckets = s3ClientWrapper.readBuckets();
        assertEquals(0, buckets.size());
    }

    private S3Client s3Client() {
        try {
            return S3Client
                    .builder()
                    .endpointOverride(localStackContainer.getEndpointOverride(LocalStackContainer.Service.S3))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate S3Client.", e);
        }
    }

    private ResourceLoader resourceLoader() {
        return new SimpleStorageResourceLoader(this.s3Client);
    }

    private ResourceConfiguration resourceConfiguration() {
        ResourceConfiguration.Cloud cloud = new ResourceConfiguration.Cloud(BUCKET_NAME, BUCKET_PATH);

        return new ManualResourceConfiguration(false, true, null, cloud);
    }

    private ResourceManager resourceManager() {
        try {
            return new ResourceManager(resourceConfiguration, resourceLoader, s3Client);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate ResourceManager.", e);
        }
    }
}
