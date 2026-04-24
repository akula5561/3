package com.danil.library.repository;

import com.danil.library.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByMacAddress(String macAddress);

    Optional<Device> findByMacAddressAndUser_Id(String macAddress, Long userId);

    /** : {@code findByMac}. */
    default Optional<Device> findByMac(String macAddress) {
        return findByMacAddress(macAddress);
    }
}

