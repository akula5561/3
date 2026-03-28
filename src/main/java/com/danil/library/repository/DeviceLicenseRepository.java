package com.danil.library.repository;

import com.danil.library.model.Device;
import com.danil.library.model.DeviceLicense;
import com.danil.library.model.License;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, UUID> {

    long countByLicense(License license);

    List<DeviceLicense> findByDeviceAndLicense(Device device, License license);

    /**
     * Активная лицензия: привязана к пользователю и устройству, не заблокирована, срок не истёк.
     */
    @Query("""
            select dl.license from DeviceLicense dl
            where dl.device.id = :deviceId
            and dl.license.user.id = :userId
            and dl.license.product.id = :productId
            and dl.license.blocked = false
            and dl.license.endingDate is not null
            and dl.license.endingDate >= :today
            """)
    Optional<License> findActiveByDeviceUserAndProduct(
            @Param("deviceId") UUID deviceId,
            @Param("userId") Long userId,
            @Param("productId") UUID productId,
            @Param("today") LocalDate today
    );
}

