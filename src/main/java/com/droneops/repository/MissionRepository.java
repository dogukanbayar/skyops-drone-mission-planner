package com.droneops.repository;

import com.droneops.domain.Mission;
import com.droneops.domain.MissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MissionRepository extends JpaRepository<Mission, Long>, JpaSpecificationExecutor<Mission> {

    boolean existsByDroneIdAndStatus(Long droneId, MissionStatus status);

    long countByStatus(MissionStatus status);

    @Override
    @EntityGraph(attributePaths = {"operator", "drone"})
    Page<Mission> findAll(Specification<Mission> spec, Pageable pageable);
}
