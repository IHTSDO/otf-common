package org.ihtsdo.otf.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {

	private static final String MD5_EXTENSION = ".md5";

	private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

	private static final String ZIP_EXTENSION = ".zip";

	private static final int KB = 1024;

	private static final String MD5 = "MD5";


	private static final int THRESHOLD_ENTRIES = 10000;


	private FileUtils() {
		// private constructor to prevent instantiation
	}

	public static Map<String, String> examineZipContents(final String filename, final InputStream is) {
		Map<String, String> contents = new HashMap<>();
		try (ZipInputStream zis = new ZipInputStream(is)) {
			ZipEntry entry;
			int idx = 0;
			while ((entry = zis.getNextEntry()) != null) {
				contents.put("zip_content_" + idx, entry.getName());
				LOGGER.debug("{}[{}]: {}", filename, idx, entry.getName());
				idx++;
				// Check for zip bombs
				if (idx > THRESHOLD_ENTRIES) {
					LOGGER.error("Zip bomb detected in file: {}", filename);
					break;
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to enumerate zip file contents", e);
		}
		return contents;
	}

	/*
	 *@author http://www.mkyong.com/java/how-to-generate-a-file-checksum-value-in-java/
	 */
	public static String calculateMD5(final File file) throws NoSuchAlgorithmException, IOException {
		StringBuilder sb = new StringBuilder();
		MessageDigest md = MessageDigest.getInstance(MD5);
		try (FileInputStream fis = new FileInputStream(file)) {
			byte[] dataBytes = new byte[KB];
			int bytesRead;
			while ((bytesRead = fis.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, bytesRead);
			}
			//convert the byte to hex format
			byte[] md5Bytes = md.digest();
			for (byte md5Byte : md5Bytes) {
				sb.append(Integer.toString((md5Byte & 0xff) + 0x100, 16).substring(1));
			}
		}
		return sb.toString();
	}

	/**
	 * Creates a file in the same directory as hashMe, using the same name with .md5 appended to it.
	 *
	 * @param hashMe   the file to be hashed
	 * @return the created MD5 file
	 * @throws IOException if the file cannot be created
	 */
	public static File createMD5File(final File hashMe, String md5String) throws IOException {
		String resultFilePath = hashMe.getAbsolutePath() + MD5_EXTENSION;
		File resultFile = new File(resultFilePath);
		try (FileOutputStream fop = new FileOutputStream(resultFile)) {
			byte[] contentInBytes = md5String.getBytes();
			fop.write(contentInBytes);
		}
		return resultFile;
	}

	public static boolean hasExtension(final String fileName, final String extension) {
    if (fileName == null) { return false; }
		return fileName.endsWith(extension);
	}

	public static boolean isZip(final String fileName) {
		return hasExtension(fileName, ZIP_EXTENSION);
	}

	public static String getFilenameFromPath(final String filePath) {
		return filePath.substring(filePath.lastIndexOf("/") + 1);
	}
	
	public static boolean isMD5(final String fileName) {
	    return hasExtension(fileName, MD5_EXTENSION);
	}

}
