package org.snomed.otf.script.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.SystemUtils;
import org.ihtsdo.otf.exception.ScriptException;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static <T> T convertToObject(InputStream inputStream, Class<T> t) throws ScriptException {
        try {
            return OBJECT_MAPPER.readValue(inputStream, t);
        } catch (IOException e) {
            throw new ScriptException("Failed to deserialize InputStream into a " + t.getSimpleName() , e);
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

	public static String getMD5Nullable(File file) {
		try {
			return getMD5OrThrow(file);
		} catch (IOException e) {
			return null;
		}
	}

    public static String getMD5OrThrow(File file) throws IOException {
        //Reading the entire file into memory is slow, so try the native tool first
        String nativeMd5 = getMD5Native(file);
        if (nativeMd5 != null) {
            return nativeMd5;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            return DigestUtils.md5DigestAsHex(fis);
        }
    }

    private static final Pattern MD5_HEX_PATTERN = Pattern.compile("\\b[0-9a-fA-F]{32}\\b");
    private static final Duration NATIVE_MD5_TIMEOUT = Duration.ofMinutes(30);

    /**
     * Shells out to the platform's native MD5 tool, since it's typically far faster on large files
     * than reading through {@code MessageDigest} in the JVM. Returns null on any failure (tool missing,
     * unrecognised OS, non-zero exit, timeout) so the caller can fall back to the JVM implementation.
     */
    private static String getMD5Native(File file) {
        List<String> command = nativeMd5Command(file);
        if (command == null) {
            return null;
        }
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(NATIVE_MD5_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            if (process.exitValue() != 0) {
                return null;
            }
            Matcher matcher = MD5_HEX_PATTERN.matcher(output);
            return matcher.find() ? matcher.group().toLowerCase(Locale.ROOT) : null;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static List<String> nativeMd5Command(File file) {
        String path = file.getAbsolutePath();
        if (SystemUtils.IS_OS_WINDOWS) {
            return List.of("certutil", "-hashfile", path, "MD5");
        } else if (SystemUtils.IS_OS_MAC) {
            return List.of("md5", "-q", path);
        } else if (SystemUtils.IS_OS_LINUX) {
            return List.of("md5sum", path);
        }
        return null;
    }
}
