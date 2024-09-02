package org.ihtsdo.otf.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipFileUtils {
	
	/**
	 * Utility method for extracting files only from a zip file to a given folder not including the folder structure
	 * @param file the zip file to be extracted
	 * @param outputDir the output folder to extract the zip to.
	 * @throws IOException IOException
	 */
	public static void extractFilesFromZipToOneFolder(final File file, final String outputDir) throws IOException {
		if (file == null || outputDir == null) {
			throw new IllegalArgumentException("File and output directory must not be null");
		}
		if (!file.exists()) {
			throw new FileNotFoundException("File does not exist: " + file);
		}
		File outputDirFile = new File(outputDir);
		if (outputDirFile.exists() && !outputDirFile.isDirectory()) {
			throw new IllegalArgumentException("Output directory must be a directory: " + outputDir);
		}
		if (!outputDirFile.exists() && !outputDirFile.mkdirs()) {
			throw new IOException("Failed to create output directory: " + outputDir);
		}
		try (ZipFile zipFile = new ZipFile(file)) {
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			Stream<? extends ZipEntry> stream = Collections.list(entries).stream();
			stream.parallel().forEach(entry -> {
				if (!entry.isDirectory()) {
					Path outputPath = Paths.get(outputDir, Paths.get(entry.getName()).getFileName().toString());
					try (InputStream in = zipFile.getInputStream(entry)) {
						Files.copy(in, outputPath);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			});
		}
	}

	/**
	 * Extracts a zip file to a given folder including the folder structure
	 * @param file the zip file to be extracted
	 * @param outputDir the output folder to extract the zip to.
	 * @throws IOException IOException
	 */
	public static void extractZipFile(final File file, final String outputDir) throws IOException {
		Path outputPath = Paths.get(outputDir);
		// Use try-with-resources to ensure resources are closed automatically
		try (ZipFile zipFile = new ZipFile(file)) {
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();

			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				Path entryPath = outputPath.resolve(entry.getName());
				// Create parent directories if they don't exist
				if (entry.isDirectory()) {
					Files.createDirectories(entryPath);
				} else {
					// Ensure parent directories are created
					Files.createDirectories(entryPath.getParent());
					// Use try-with-resources and buffered streams
					try (InputStream in = new BufferedInputStream(zipFile.getInputStream(entry))) {
						// Use Files.copy for better performance
						Files.copy(in, entryPath, StandardCopyOption.REPLACE_EXISTING);
					}
				}
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
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
			Files.walk(sourceDirPath)
					.forEach(path -> {
						String zipEntryName = rootFolderName + "/" + sourceDirPath.relativize(path);
						try {
							if (Files.isDirectory(path)) {
								if (!zipEntryName.endsWith("/")) {
									zipEntryName += "/";
								}
								ZipEntry dirEntry = new ZipEntry(zipEntryName);
								zipOutputStream.putNextEntry(dirEntry);
								zipOutputStream.closeEntry();
							} else {
								ZipEntry fileEntry = new ZipEntry(zipEntryName);
								zipOutputStream.putNextEntry(fileEntry);
								Files.copy(path, zipOutputStream);
								zipOutputStream.closeEntry();
							}
						} catch (IOException e) {
							throw new UncheckedIOException("Error while zipping: " + path + " - " + e.getMessage(), e);
						}
					});
		}
	}
}
