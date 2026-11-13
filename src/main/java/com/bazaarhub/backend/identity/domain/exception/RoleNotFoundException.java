package com.bazaarhub.backend.identity.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

/**
 * Thrown when an assigned role cannot be found in the system.
 */
public class RoleNotFoundException extends ResourceNotFoundException {

    public RoleNotFoundException(String roleName) {
        super("Role", roleName);
    }
}
