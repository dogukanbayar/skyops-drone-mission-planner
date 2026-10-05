package com.droneops.repository;

import com.droneops.domain.Mission;
import com.droneops.domain.MissionPriority;
import com.droneops.domain.MissionStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Opsiyonel filtreler için JPA Criteria tabanlı (native SQL içermeyen) sorgu üreticisi. */
public final class MissionSpecifications {

    private MissionSpecifications() {
    }

    public static Specification<Mission> filter(MissionStatus status, MissionPriority priority, Long droneId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (droneId != null) {
                predicates.add(cb.equal(root.get("drone").get("id"), droneId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
