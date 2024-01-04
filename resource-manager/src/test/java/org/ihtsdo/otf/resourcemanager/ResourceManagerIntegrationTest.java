package org.ihtsdo.otf.resourcemanager;

import org.junit.jupiter.api.Test;
import org.snomed.module.storage.IntegrationTest;

import java.io.*;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResourceManagerIntegrationTest extends IntegrationTest {
    @Test
    public void testCanWriteAndReadFile() {
        // File metadata
        String fileName = UUID.randomUUID().toString();
        String fileExtension = ".txt";
        String fileResourcePath = fileName + fileExtension;

        // Create bucket
        boolean success = s3ClientWrapper.createBucket("otf-common");
        assertTrue(success);

        // Create temporary file
        File tempFile = doCreateTempFile(fileName, fileExtension);
        FileInputStream fileInputStream = asFileInputStream(tempFile);

        // Upload to S3
        success = doWriteResource(fileResourcePath, fileInputStream);
        assertTrue(success);

        // Download from S3
        InputStream inputStream = doReadResource(fileResourcePath);

        // Assert
        assertNotNull(inputStream);
    }

    private File doCreateTempFile(String prefix, String suffix) {
        try {
            return Files.createTempFile(prefix, suffix).toFile();
        } catch (IOException e) {
            throw new RuntimeException("Cannot create temporary file.", e);
        }
    }

    private FileInputStream asFileInputStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Cannot create file input stream.", e);
        }
    }

    private boolean doWriteResource(String resourcePath, InputStream resourceInputStream) {
        try {
            resourceManager.writeResource(resourcePath, resourceInputStream);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private InputStream doReadResource(String resourcePath) {
        try {
            return resourceManager.readResourceStream(resourcePath);
        } catch (IOException e) {
            return null;
        }
    }
}