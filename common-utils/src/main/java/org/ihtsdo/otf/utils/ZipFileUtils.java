package org.ihtsdo.otf.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

public class ZipFileUtils {
	
	/**
	 * Utility method for extracting a zip file to a given folder
	 * @param file the zip file to be extracted
	 * @param outputDir the output folder to extract the zip to.
	 * @throws IOException 
	 */
	public static void extractFilesFromZipToOneFolder(final File file, final String outputDir) throws IOException {
		try (ZipFile zipFile = new ZipFile(file)){
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				if (!entry.isDirectory()) {
					InputStream in = null;
					OutputStream out = null;
					try {
							in = zipFile.getInputStream(entry);
							String fileName = Paths.get(entry.getName()).getFileName().toString();
							final File entryDestination = new File(outputDir,fileName);
							out = new FileOutputStream(entryDestination);
							IOUtils.copy(in, out);
						} finally {
							IOUtils.closeQuietly(in);
							IOUtils.closeQuietly(out);
						}
				}
			}
		} 
	}
	
	
	
	/**
	 * Utility method for extracting a zip file to a given folder
	 * @param file the zip file to be extracted
	 * @param outputDir the output folder to extract the zip to.
	 * @throws IOException 
	 */
	public static void extractZipFile(final File file, final String outputDir) throws IOException {
		
		try (ZipFile zipFile = new ZipFile(file)) {
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				final File entryDestination = new File(outputDir,  entry.getName());
				entryDestination.getParentFile().mkdirs();
				if (entry.isDirectory()) {
					entryDestination.mkdirs();
				} else {
					InputStream in = null;
					OutputStream out = null;
					try {
						in = zipFile.getInputStream(entry);
						out = new FileOutputStream(entryDestination);
						IOUtils.copy(in, out);
						} finally {
							IOUtils.closeQuietly(in);
							IOUtils.closeQuietly(out);
						}
				}
			}
		}
	}
	
	
	/**
     * Zip it
     * @param zipFile output ZIP file location
	 * @param sourceFileDir 
	 * @throws IOException 
	 * @throws FileNotFoundException 
     */
    public static void zip(final String sourceFileDir, final String zipFile) throws FileNotFoundException, IOException {
     final byte[] buffer = new byte[1024];
     try ( final FileOutputStream fos = new FileOutputStream(zipFile);
    	 final ZipOutputStream zos = new ZipOutputStream(fos) ) {
    	final List<File> fileList = new ArrayList<>();
    	generateFileList(new File(sourceFileDir), fileList);
		for (final File file : fileList) {
    		final ZipEntry ze= new ZipEntry(file.getName());
        	zos.putNextEntry(ze);
        	try (final FileInputStream in = new FileInputStream(file) ) {
        		int len;
            	while ((len = in.read(buffer)) > 0) {
            		zos.write(buffer, 0, len);
            	}
        	}
    	}
     } 
   }
 
    /**
     * Traverse a directory and get all files,
     * and add the file into fileList  
     * @param node file or directory
     */
    private static void generateFileList(final File node, final List<File> fileList){
    	//add file only
    	if(node.isFile()){
    		fileList.add(node);
    	}
    	if(node.isDirectory()){
    		final String[] subNote = node.list();
            assert subNote != null;
            for(final String filename : subNote){
    			generateFileList(new File(node, filename),fileList);
    		}
    	}
    }
}
