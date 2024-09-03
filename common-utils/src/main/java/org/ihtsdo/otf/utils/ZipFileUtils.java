package org.ihtsdo.otf.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipFileUtils {

	private static final int THRESHOLD_ENTRIES = 10000; // Maximum number of entries
	private static final long THRESHOLD_SIZE = 10000000000L; // 10 GB maximum uncompressed size

	private ZipFileUtils() {
		// private constructor to prevent instantiation
	}
	/**
	 * Utility method for extracting files only from a zip file to a given folder not including the folder structure
	 * @param file the zip file to be extracted
	 * @param outputDir the output folder to extract the zip to.
	 * @throws IOException IOException
	 */
	public static void extractFilesFromZipToOneFolder(final File file, final String outputDir) throws IOException {
		extractDataFromZipFile(file, outputDir, true);
	}

	/**
	 * Extracts a zip file to a given folder including the folder structure
	 * @param file the zip file to be extracted
	 * @param outputDir the output folder to extract the zip to.
	 * @throws IOException IOException
	 */
	public static void extractZipFile(final File file, final String outputDir) throws IOException {
		extractDataFromZipFile(file, outputDir, false);
	}

	private static void extractDataFromZipFile(File file, String outputDir, boolean fileOnly) throws IOException {
		if (file == null || !file.exists()) {
			throw new FileNotFoundException("Zip file does not exist " + file);
		}
		if (outputDir == null) {
			throw new IllegalArgumentException("Output directory must not be null");
		}
		File outputDirFile = new File(outputDir);
		if (!outputDirFile.exists()) {
			throw new FileNotFoundException("Output dir does not exist " + outputDir);
		}
		if (!outputDirFile.isDirectory()) {
			throw new IllegalArgumentException("Output dir must be a directory " + outputDir);
		}

		try (ZipFile zipFile = new ZipFile(file)) {
			checkZipBombsVulnerability(zipFile);
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			Stream<? extends ZipEntry> stream = Collections.list(entries).stream();
			stream.parallel().forEach(entry -> {
				try {
					extractEntry(zipFile, entry, outputDir, fileOnly);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		}
	}

	private static void checkZipBombsVulnerability(ZipFile zipFile) throws IOException {
		long totalSizeArchive = 0;
		// Check for Zip Bombs before processing
		final Enumeration<? extends ZipEntry> entries = zipFile.entries();
		List<? extends ZipEntry> entryList = Collections.list(entries);
		if (entryList.size() > THRESHOLD_ENTRIES) {
			throw new IOException("Too many entries in the zip file. Found " + entryList.size() + " entries but the maximum allowed is " + THRESHOLD_ENTRIES);
		}
		// Calculate total size and validate entries
		for (ZipEntry entry : entryList) {
			totalSizeArchive += entry.getSize();
			if (totalSizeArchive > THRESHOLD_SIZE) {
				throw new IOException("Total uncompressed size exceeds limit. Found " + totalSizeArchive + " bytes but the maximum allowed is " + THRESHOLD_SIZE + " bytes");
			}
		}
	}

	private static void extractEntry(ZipFile zipFile, ZipEntry entry, String outputDir, boolean extractFileOnly) throws IOException {
		Path outputPath = Paths.get(outputDir, entry.getName());
		if (extractFileOnly) {
			// Extract only the file name
			outputPath = Paths.get(outputDir, Paths.get(entry.getName()).getFileName().toString());
		}
		String canonicalFilePath = outputPath.toFile().getCanonicalPath();
		// Check for Zip Slip vulnerability
		String canonicalOutputDir = Paths.get(outputDir).toFile().getCanonicalPath();
		if (!canonicalFilePath.startsWith(canonicalOutputDir + File.separator)) {
			throw new IOException("Entry is outside of the target directory: " + entry.getName());
		}
		if (extractFileOnly && entry.isDirectory()) {
			return;
		}
		// Create parent directories if they don't exist
		if (entry.isDirectory()) {
			Files.createDirectories(outputPath);
		} else {
			// Use try-with-resources and buffered streams
			if (!extractFileOnly) {
				// Ensure parent directories are created
				Files.createDirectories(outputPath.getParent());
			}
			try (InputStream in = new BufferedInputStream(zipFile.getInputStream(entry))) {
				// Use Files.copy for better performance
				Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}



	/**
     * Zip it
     * @param zipFile output ZIP file location
	 * @param sourceFileDir source file directory
	 * @throws IOException IOException
	 * @throws FileNotFoundException file not found exception
     */
	public static void zip(final String sourceFileDir, final String zipFile) throws IOException {
		Path sourceDirPath = Paths.get(sourceFileDir);
		if (!Files.exists(sourceDirPath)) {
			throw new FileNotFoundException("Source directory does not exist: " + sourceFileDir);
		}
		String rootFolderName = sourceDirPath.getFileName().toString();
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));
			 Stream<Path> walk = Files.walk(sourceDirPath)) {
				walk.forEach(path -> {
				String zipEntryName = rootFolderName + "/" + sourceDirPath.relativize(path);
				try {
					if (Files.isDirectory(path)) {
						if (!zipEntryName.endsWith("/")) {
							zipEntryName += "/";
						}
						ZipEntry dirEntry = new ZipEntry(zipEntryName);
						zipOutputStream.putNextEntry(dirEntry);
					} else {
						ZipEntry fileEntry = new ZipEntry(zipEntryName);
						zipOutputStream.putNextEntry(fileEntry);
						Files.copy(path, zipOutputStream);
					}
					zipOutputStream.closeEntry();
				} catch (IOException e) {
					throw new UncheckedIOException("Error while zipping: " + path + " - " + e.getMessage(), e);
				}
			});
		}
	}

	public static List<String> listFiles(final File zipFile) throws IOException {
		List<String> fileList;
		try (ZipFile zipFileToOpen = new ZipFile(zipFile)) {
			fileList = zipFileToOpen.stream().filter(file -> !file.isDirectory()).map(ZipEntry::getName).toList();
		}
		return fileList;
	}
}
