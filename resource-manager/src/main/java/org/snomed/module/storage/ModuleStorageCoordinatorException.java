package org.snomed.module.storage;

public class ModuleStorageCoordinatorException extends Exception {
    public static class InvalidArgumentsException extends ModuleStorageCoordinatorException {
        public InvalidArgumentsException(String message) {
            super(message);
        }
    }

    public static class ResourceNotFoundException extends ModuleStorageCoordinatorException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    public static class DuplicateResourceException extends ModuleStorageCoordinatorException {
        public DuplicateResourceException(String message) {
            super(message);
        }
    }

    public static class OperationFailedException extends ModuleStorageCoordinatorException {
        public OperationFailedException(String message) {
            super(message);
        }
    }

    public ModuleStorageCoordinatorException(String message) {
        super(message);
    }
}
