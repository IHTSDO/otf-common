package org.snomed.otf.script.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.Optional;

public class FileUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static File doCreateTempFile(String fileName) {
        try {
            int lastDotIndex = fileName.lastIndexOf('.');
            String prefix = fileName.substring(0, lastDotIndex);
            String suffix = fileName.substring(lastDotIndex);

            File file = Files.createTempFile(prefix, suffix).toFile();
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Cannot create temporary file.", e);
        }
    }

    public static void copyInputStreamToFile(InputStream input, File file) {
        try (OutputStream output = new FileOutputStream(file)) {
            input.transferTo(output);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public static <T> T convertToObject(File file, Class<T> t) {
        try {
            return OBJECT_MAPPER.readValue(file, t);
        } catch (IOException e) {
            return null;
        }
    }

    public static <T> boolean writeToFile(File file, Object t) {
        try {
            OBJECT_MAPPER.writeValue(file, t);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static Optional<String> getMD5(File file) {
        try {
            return Optional.of(getMD5OrThrow(file));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static String getMD5OrThrow(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return DigestUtils.md5DigestAsHex(fis);
        }
    }
}
