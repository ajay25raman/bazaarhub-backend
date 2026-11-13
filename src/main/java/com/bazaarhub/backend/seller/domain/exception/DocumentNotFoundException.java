package com.bazaarhub.backend.seller.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class DocumentNotFoundException extends ResourceNotFoundException {

    public DocumentNotFoundException(String identifier) {
        super("Document", identifier);
    }

    public DocumentNotFoundException(Long id) {
        super("Document", id != null ? id.toString() : "null");
    }
}
