package com.danil.library.service;

import com.danil.library.model.LicenseType;
import com.danil.library.repository.LicenseTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class LicenseTypeService {

    private final LicenseTypeRepository types;

    public LicenseTypeService(LicenseTypeRepository types) {
        this.types = types;
    }

    /** Методичка: {@code LicenseTypeService.getTypeOrFail(typeId)}. */
    public LicenseType getTypeOrFail(UUID typeId) {
        return types.findById(typeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "License type not found"));
    }
}
