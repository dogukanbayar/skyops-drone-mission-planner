package com.droneops.repository;

import com.droneops.domain.Operator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatorRepository extends JpaRepository<Operator, Long> {
    boolean existsByEmailIgnoreCase(String email);
}
