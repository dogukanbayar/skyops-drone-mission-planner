package com.droneops.repository;

import com.droneops.domain.Drone;
import com.droneops.domain.DroneStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DroneRepository extends JpaRepository<Drone, Long> {
    boolean existsBySerialNumberIgnoreCase(String serialNumber);

    long countByStatus(DroneStatus status);

    /**
     * Drone satırını yazma kilidiyle okur. "Bir drone aynı anda tek ACTIVE görevde" kuralının
     * eşzamanlı isteklerde de bozulmaması için görev atama/başlatma bu kilit altında yapılır.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Drone d where d.id = :id")
    Optional<Drone> findByIdForUpdate(@Param("id") Long id);
}
