package org.ihtsdo.otf.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ZipFileUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void extractFilesFromZipToOneFolder_extractsFilesCorrectly() throws IOException {
        File testZipFile = createTestZipFile();
        File outputDir = tempDir.resolve("output").toFile();
        if (!outputDir.mkdir()) {
            fail("Failed to create output directory");
        }
        ZipFileUtils.extractFilesFromZipToOneFolder(testZipFile, outputDir.getAbsolutePath());

        assertTrue(new File(outputDir, "file1.txt").exists());
        assertTrue(new File(outputDir, "file2.txt").exists());
    }

    @Test
    void extractZipFile_extractsFilesAndDirectoriesCorrectly() throws IOException {
        File testZipFile = createTestZipFile();
        File outputDir = tempDir.resolve("output").toFile();
        if (!outputDir.mkdir()) {
            fail("Failed to create output directory");
        }
        ZipFileUtils.extractZipFile(testZipFile, outputDir.getAbsolutePath());

        assertTrue(new File(outputDir, "source/subdirA/").exists());
        assertTrue(new File(outputDir, "source/subdirA/file1.txt").exists());
        assertTrue(new File(outputDir, "source/subdirB").exists());
        assertTrue(new File(outputDir, "source/subdirB/file2.txt").exists());
    }

    @Test
    void zip_createsZipFileCorrectly() throws IOException {
        File testZipFile = createTestZipFile();
        assertTrue(testZipFile.exists());
        try (ZipFile zipFile = new ZipFile(testZipFile)) {
            assertNotNull(zipFile.getEntry("source/"));
            assertNotNull(zipFile.getEntry("source/subdirA/"));
            assertNotNull(zipFile.getEntry("source/subdirB/"));
            assertNotNull(zipFile.getEntry("source/subdirA/file1.txt"));
            assertNotNull(zipFile.getEntry("source/subdirB/file2.txt"));
        }
    }



    private File createTestZipFile() throws IOException {
        Path sourceDir = tempDir.resolve("source/");
        Files.createDirectories(sourceDir);
        // Create sub directories
        Files.createDirectories(sourceDir.resolve("subdirA"));
        Files.createDirectories(sourceDir.resolve("subdirB"));

        Files.write(sourceDir.resolve("subdirA/file1.txt"), "content1".getBytes());
        Files.write(sourceDir.resolve("subdirB/file2.txt"), "content2".getBytes());

        Path zipFilePath = tempDir.resolve("test.zip");
        ZipFileUtils.zip(sourceDir.toString(), zipFilePath.toString());
        return zipFilePath.toFile();
    }

    @Test
    void extractFilesFromZipToOneFolder_throwsExceptionForNullFile() {
        FileNotFoundException exception = assertThrowsExactly(FileNotFoundException.class, () ->
                ZipFileUtils.extractFilesFromZipToOneFolder(new File("test.zip"), "outputDir"));
        assertEquals("Zip file does not exist test.zip", exception.getMessage());
    }

    @Test
    void extractFilesFromZipToOneFolder_throwsExceptionForNonExistentFile() {
        File nonExistentFile = new File("nonExistent.zip");
        File outputDir = tempDir.resolve("output").toFile();
        FileNotFoundException exception = assertThrows(FileNotFoundException.class, () ->
                ZipFileUtils.extractFilesFromZipToOneFolder(nonExistentFile, outputDir.getAbsolutePath()));
        assertEquals("Zip file does not exist nonExistent.zip", exception.getMessage());
    }

    @Test
    void extractFilesFromZipToOneFolder_throwsExceptionForInvalidOutputDir() throws IOException {
        Path zipFilePath = tempDir.resolve("test.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            ZipEntry entry = new ZipEntry("file.txt");
            zos.putNextEntry(entry);
            zos.write("content".getBytes());
            zos.closeEntry();
        }

        File invalidOutput = tempDir.resolve("output.txt").toFile();
        if (!invalidOutput.createNewFile()) {
            fail("Failed to create output file");
        }

        IllegalArgumentException exception = assertThrowsExactly(IllegalArgumentException.class, () ->
                ZipFileUtils.extractFilesFromZipToOneFolder(zipFilePath.toFile(), invalidOutput.getAbsolutePath()));
        assertEquals("Output dir must be a directory " + invalidOutput.getAbsolutePath(), exception.getMessage());
    }

    @Test
    void zip_throwsExceptionForNonExistentSourceDir() {
        String nonExistentDir = "nonExistentDir";
        String zipFile = tempDir.resolve("test.zip").toString();
        FileNotFoundException exception = assertThrows(FileNotFoundException.class, () ->
                ZipFileUtils.zip(nonExistentDir, zipFile));
        assertEquals("Source directory does not exist: " + nonExistentDir, exception.getMessage());
    }

    @Test
    void listFiles() throws IOException{
        File testZipFile = createTestZipFile();
        // Check file names in the zip file
        List<String> filenames = ZipFileUtils.listFiles(testZipFile);
        assertEquals(2, filenames.size());
        assertTrue(filenames.contains("source/subdirA/file1.txt"));
        assertTrue(filenames.contains("source/subdirB/file2.txt"));
    }
}