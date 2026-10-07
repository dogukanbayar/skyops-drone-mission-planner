package com.droneops.service;

import com.droneops.domain.Drone;
import com.droneops.domain.DroneStatus;
import com.droneops.domain.Mission;
import com.droneops.domain.MissionPriority;
import com.droneops.domain.MissionStatus;
import com.droneops.domain.Operator;
import com.droneops.dto.MissionRequest;
import com.droneops.dto.MissionResponse;
import com.droneops.dto.PageResponse;
import com.droneops.dto.WaypointRequest;
import com.droneops.exception.BusinessRuleException;
import com.droneops.exception.ResourceNotFoundException;
import com.droneops.mapper.MissionMapper;
import com.droneops.repository.DroneRepository;
import com.droneops.repository.MissionRepository;
import com.droneops.repository.MissionSpecifications;
import com.droneops.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Görev kullanım senaryoları. Durum geçiş kuralları {@link Mission} içindedir; drone ile ilgili
 * kurallar (bakım, tek ACTIVE görev) drone satırı kilitlenerek burada uygulanır.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionService {

    static final int MAX_PAGE_SIZE = 100;

    private final MissionRepository missionRepository;
    private final OperatorRepository operatorRepository;
    private final DroneRepository droneRepository;
    private final MissionMapper mapper;

    @Transactional
    public MissionResponse create(MissionRequest request) {
        log.info("Creating mission: operatorId={}, droneId={}, priority={}", request.operatorId(), request.droneId(), request.priority());
        Operator operator = operatorRepository.findById(request.operatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Operatör", request.operatorId()));
        Drone drone = lockDrone(request.droneId());
        ensureDroneAssignable(drone);

        Mission mission = mapper.toEntity(request, operator, drone);
        if (request.waypoints() != null) {
            request.waypoints().forEach(w -> mission.addWaypoint(w.latitude(), w.longitude(), w.altitude()));
        }
        Mission saved = missionRepository.save(mission);
        log.info("Mission created: id={}, status={}, waypoints={}", saved.getId(), saved.getStatus(), saved.getWaypoints().size());
        return mapper.toResponse(saved);
    }

    public PageResponse<MissionResponse> findAll(MissionStatus status, MissionPriority priority, Long droneId,
                                                 int page, int size) {
        log.debug("Listing missions: status={}, priority={}, droneId={}, page={}, size={}", status, priority, droneId, page, size);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);
        return PageResponse.from(missionRepository
                .findAll(MissionSpecifications.filter(status, priority, droneId), pageable)
                .map(mapper::toResponse));
    }

    public MissionResponse findById(Long id) {
        return mapper.toResponse(getMission(id));
    }

    @Transactional
    public MissionResponse addWaypoint(Long id, WaypointRequest request) {
        Mission mission = getMission(id);
        log.debug("Adding waypoint: missionId={}, status={}, currentWaypoints={}", id, mission.getStatus(), mission.getWaypoints().size());
        mission.addWaypoint(request.latitude(), request.longitude(), request.altitude());
        log.info("Waypoint added: missionId={}, totalWaypoints={}", id, mission.getWaypoints().size());
        missionRepository.flush();
        return mapper.toResponse(mission);
    }

    @Transactional
    public MissionResponse start(Long id) {
        log.info("Starting mission: id={}", id);
        Mission mission = getMission(id);
        mission.ensureStartable();
        log.debug("Mission passed start checks: id={}, waypoints={}, droneId={}", id, mission.getWaypoints().size(), mission.getDrone().getId());
        Drone drone = lockDrone(mission.getDrone().getId());
        ensureDroneAssignable(drone);
        mission.start();
        log.info("Mission started: id={}, status={}", id, mission.getStatus());
        return mapper.toResponse(mission);
    }

    @Transactional
    public MissionResponse complete(Long id) {
        Mission mission = getMission(id);
        mission.complete();
        log.info("Mission completed: id={}, status={}", id, mission.getStatus());
        return mapper.toResponse(mission);
    }

    @Transactional
    public MissionResponse cancel(Long id) {
        Mission mission = getMission(id);
        MissionStatus previous = mission.getStatus();
        mission.cancel();
        log.info("Mission cancelled: id={}, previousStatus={}", id, previous);
        return mapper.toResponse(mission);
    }

    private Mission getMission(Long id) {
        return missionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Görev", id));
    }

    private Drone lockDrone(Long id) {
        log.debug("Acquiring write lock on drone: id={}", id);
        return droneRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Drone", id));
    }

    /** Bakımdaki drone'a veya zaten ACTIVE görevi olan drone'a görev atanamaz. */
    private void ensureDroneAssignable(Drone drone) {
        log.debug("Checking drone assignability: id={}, status={}", drone.getId(), drone.getStatus());
        if (drone.getStatus() == DroneStatus.MAINTENANCE) {
            throw new BusinessRuleException(drone.getName() + " şu an bakımda; göreve atanamaz.");
        }
        if (missionRepository.existsByDroneIdAndStatus(drone.getId(), MissionStatus.ACTIVE)) {
            throw new BusinessRuleException(drone.getName() + " zaten ACTIVE bir görevde; ikinci görev atanamaz.");
        }
        log.debug("Drone is assignable: id={} (no ACTIVE mission)", drone.getId());
    }
}
