package org.ihtsdo.otf.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ZipFileUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void extractFilesFromZipToOneFolder_extractsFilesCorrectly() throws IOException {
        Path zipFilePath = tempDir.resolve("test.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            ZipEntry entry1 = new ZipEntry("file1.txt");
            zos.putNextEntry(entry1);
            zos.write("content1".getBytes());
            zos.closeEntry();

            ZipEntry entry2 = new ZipEntry("file2.txt");
            zos.putNextEntry(entry2);
            zos.write("content2".getBytes());
            zos.closeEntry();
        }

        File outputDir = tempDir.resolve("output").toFile();
        ZipFileUtils.extractFilesFromZipToOneFolder(zipFilePath.toFile(), outputDir.getAbsolutePath());

        assertTrue(new File(outputDir, "file1.txt").exists());
        assertTrue(new File(outputDir, "file2.txt").exists());
    }

    @Test
    void extractZipFile_extractsFilesAndDirectoriesCorrectly() throws IOException {
        Path zipFilePath = tempDir.resolve("test.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            ZipEntry dirEntry = new ZipEntry("dir/");
            zos.putNextEntry(dirEntry);
            zos.closeEntry();

            ZipEntry fileEntry = new ZipEntry("dir/file.txt");
            zos.putNextEntry(fileEntry);
            zos.write("content".getBytes());
            zos.closeEntry();
        }

        File outputDir = tempDir.resolve("output").toFile();
        ZipFileUtils.extractZipFile(zipFilePath.toFile(), outputDir.getAbsolutePath());

        assertTrue(new File(outputDir, "dir").exists());
        assertTrue(new File(outputDir, "dir/file.txt").exists());
    }

    @Test
    void zip_createsZipFileCorrectly() throws IOException {
        Path sourceDir = tempDir.resolve("source/");
        Files.createDirectories(sourceDir);
        // Create sub directories
        Files.createDirectories(sourceDir.resolve("subdirA"));
        Files.createDirectories(sourceDir.resolve("subdirB"));

        Files.write(sourceDir.resolve("subdirA/file1.txt"), "content1".getBytes());
        Files.write(sourceDir.resolve("subdirB/file2.txt"), "content2".getBytes());

        Path zipFilePath = tempDir.resolve("test.zip");
        ZipFileUtils.zip(sourceDir.toString(), zipFilePath.toString());

        assertTrue(Files.exists(zipFilePath));
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(zipFilePath.toFile())) {
            assertNotNull(zipFile.getEntry("source/"));
            assertNotNull(zipFile.getEntry("source/subdirA/"));
            assertNotNull(zipFile.getEntry("source/subdirB/"));
            assertNotNull(zipFile.getEntry("source/subdirA/file1.txt"));
            assertNotNull(zipFile.getEntry("source/subdirB/file2.txt"));
        }
    }

    @Test
    void extractFilesFromZipToOneFolder_throwsExceptionForNullFile() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ZipFileUtils.extractFilesFromZipToOneFolder(null, "outputDir"));
        assertEquals("File and output directory must not be null", exception.getMessage());
    }

    @Test
    void extractFilesFromZipToOneFolder_throwsExceptionForNonExistentFile() {
        File nonExistentFile = new File("nonExistent.zip");
        File outputDir = tempDir.resolve("output").toFile();
        FileNotFoundException exception = assertThrows(FileNotFoundException.class, () ->
                ZipFileUtils.extractFilesFromZipToOneFolder(nonExistentFile, outputDir.getAbsolutePath()));
        assertEquals("File does not exist: nonExistent.zip", exception.getMessage());
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

        File invalidOutputDir = tempDir.resolve("output.txt").toFile();
        Files.createFile(invalidOutputDir.toPath());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ZipFileUtils.extractFilesFromZipToOneFolder(zipFilePath.toFile(), invalidOutputDir.getAbsolutePath()));
        assertEquals("Output directory must be a directory: " + invalidOutputDir.getAbsolutePath(), exception.getMessage());
    }

    @Test
    void zip_throwsExceptionForNonExistentSourceDir() {
        String nonExistentDir = "nonExistentDir";
        String zipFile = tempDir.resolve("test.zip").toString();
        FileNotFoundException exception = assertThrows(FileNotFoundException.class, () ->
                ZipFileUtils.zip(nonExistentDir, zipFile));
        assertEquals("Source directory does not exist: " + nonExistentDir, exception.getMessage());
    }
}