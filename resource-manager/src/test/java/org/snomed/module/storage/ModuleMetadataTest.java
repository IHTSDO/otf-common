package org.snomed.module.storage;

import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModuleMetadataTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void moduleMetadata_ShouldSerializeExpectedProperties() throws IOException {
        // given
        ModuleMetadata moduleMetadata = moduleMetadata(
                "filename",
                "TEST",
                "45678910",
                "45678910",
                20240102,
                new Date(),
                "md5",
                true,
                true
        );

        moduleMetadata.setDependencies(List.of(moduleMetadata(
                "filename",
                "TEST",
                "12345",
                "12345",
                20240101,
                new Date(),
                "md5",
                true,
                true
        )));

        // when
        String jsonString = OBJECT_MAPPER.writeValueAsString(moduleMetadata);
        JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonString);

        // then
        // Update assertExpectedFieldName or exclude new field names from de/serialisation
        jsonNode.fieldNames().forEachRemaining(this::assertExpectedFieldName);
    }

    @Test
    void moduleMetadata_ShouldDeserialize() throws IOException {
        // given
        ModuleMetadata input = moduleMetadata(
                "filename",
                "TEST",
                "45678910",
                "45678910",
                20240102,
                new Date(),
                "md5",
                true,
                true
        );

        input.setDependencies(List.of(moduleMetadata(
                "filename",
                "TEST",
                "12345",
                "12345",
                20240101,
                new Date(),
                "md5",
                true,
                true
        )));

        // when
        String jsonString = OBJECT_MAPPER.writeValueAsString(input);
        ModuleMetadata output = OBJECT_MAPPER.readValue(jsonString, ModuleMetadata.class);

        // then
        assertEquals(input, output);
    }

    private void assertExpectedFieldName(String fieldName) {
        List<String> expected = List.of("filename", "codeSystemShortName", "identifyingModuleId", "compositionModuleIds", "effectiveTime", "fileTimeStamp", "fileMD5", "published", "edition", "dependencies");
        boolean contains = expected.contains(fieldName);
        if (!contains) {
            fail(fieldName + " is not recognised.");
        } else {
            assertTrue(true);
        }
    }

    private ModuleMetadata moduleMetadata(
            String filename, String codeSystemShortName,
            String identifyingModuleId, String compositionModuleId,
            Integer effectiveTime, Date fileTimeStamp,
            String fileMD5, boolean published, boolean edition
    ) {
        ModuleMetadata moduleMetadata = new ModuleMetadata();
        moduleMetadata.setFilename(filename);
        moduleMetadata.setCodeSystemShortName(codeSystemShortName);
        moduleMetadata.setIdentifyingModuleId(identifyingModuleId);
        moduleMetadata.setCompositionModuleIds(List.of(compositionModuleId));
        moduleMetadata.setEffectiveTime(effectiveTime);
        moduleMetadata.setFileTimeStamp(fileTimeStamp);
        moduleMetadata.setFileMD5(fileMD5);
        moduleMetadata.setPublished(published);
        moduleMetadata.setEdition(edition);

        return moduleMetadata;
    }
}