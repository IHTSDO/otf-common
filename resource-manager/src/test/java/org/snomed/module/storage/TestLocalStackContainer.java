package org.snomed.module.storage;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

public class TestLocalStackContainer extends LocalStackContainer {
    private static final String FULL_IMAGE_NAME = "localstack/localstack:3.0.2";

    public TestLocalStackContainer() {
        super(DockerImageName.parse(FULL_IMAGE_NAME));
        super.withServices(Service.S3);
    }
}
