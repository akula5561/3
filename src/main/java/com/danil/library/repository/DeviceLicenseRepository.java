package com.danil.library.repository;

import com.danil.library.model.Device;
import com.danil.library.model.DeviceLicense;
import com.danil.library.model.License;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, UUID> {

    long countByLicense(License license);

    List<DeviceLicense> findByDeviceAndLicense(Device device, License license);
}

