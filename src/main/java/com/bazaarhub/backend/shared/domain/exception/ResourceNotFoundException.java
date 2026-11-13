package com.bazaarhub.backend.shared.domain.exception;

/**
 * Thrown when a requested domain entity or aggregate cannot be located.
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(String.format("%s with identifier '%s' was not found", resourceName, identifier), "RESOURCE_NOT_FOUND");
    }
}
