package org.snomed.module.storage;

import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    @Container
    public static LocalStackContainer localStackContainer = new TestLocalStackContainer();

    protected final S3Client s3Client;
    protected final ResourceLoader resourceLoader;
    protected final ResourceConfiguration resourceConfiguration;
    protected final ResourceManager resourceManager;
    protected final S3ClientWrapper s3ClientWrapper;

    public IntegrationTest() {
        this.s3Client = s3Client();
        this.resourceLoader = resourceLoader();
        this.resourceConfiguration = resourceConfiguration();
        this.resourceManager = resourceManager();
        this.s3ClientWrapper = new S3ClientWrapper(s3Client);
    }

    @BeforeEach
    public void setUp() {

    }

    @AfterEach
    public void tearDown() {
        // Delete all data
        List<Bucket> buckets = s3ClientWrapper.readBuckets();
        for (Bucket bucket : buckets) {
            String bucketName = bucket.name();
            List<String> objectKeys = s3ClientWrapper.readObjectKeys(bucketName);
            for (String objectKey : objectKeys) {
                s3ClientWrapper.deleteObject(bucketName, objectKey);
            }

            objectKeys = s3ClientWrapper.readObjectKeys(bucketName);
            assertEquals(0, objectKeys.size());

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
        ResourceConfiguration.Cloud cloud = new ResourceConfiguration.Cloud("otf-common", "files");

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
