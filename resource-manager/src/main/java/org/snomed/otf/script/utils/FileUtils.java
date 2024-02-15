package org.snomed.otf.script.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.otf.exception.ScriptException;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.Optional;

public class FileUtils {
    private static ObjectMapper OBJECT_MAPPER;
    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static File doCreateTempFile(String fileName) throws IOException {
        try {
            int lastDotIndex = fileName.lastIndexOf('.');
            String prefix = fileName.substring(0, lastDotIndex);
            String suffix = fileName.substring(lastDotIndex);

            File file = Files.createTempFile(prefix, suffix).toFile();
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new IOException("Cannot create temporary file.", e);
        }
    }

    public static void copyInputStreamToFile(InputStream input, File file) throws IOException {
        try (OutputStream output = new FileOutputStream(file)) {
            input.transferTo(output);
        }  catch (IOException e) {
            throw new IOException("Failed to copy input stream to file: " + file, e);
        }
    }

    public static <T> T convertToObject(File file, Class<T> t) throws ScriptException {
        try {
            return OBJECT_MAPPER.readValue(file, t);
        } catch (IOException e) {
            throw new ScriptException("Failed to deserialize file: " + file + " into a " + t.getSimpleName() , e);
        }
    }

    public static <T> void writeToFile(File file, Object t) throws ScriptException {
        try {
            OBJECT_MAPPER.writeValue(file, t);
        } catch (IOException e) {
            throw new ScriptException("Failed to write " + t + " to file: " + file, e);
        }
    }

    public static Optional<String> getMD5(File file) throws ScriptException {
        try {
            return Optional.of(getMD5OrThrow(file));
        } catch (IOException e) {
            throw new ScriptException("Failed to MD5 file: " + file, e);
        }
    }

    public static String getMD5OrThrow(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return DigestUtils.md5DigestAsHex(fis);
        }
    }
}
