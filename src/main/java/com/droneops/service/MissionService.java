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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Görev kullanım senaryoları. Durum geçiş kuralları {@link Mission} içindedir; drone ile ilgili
 * kurallar (bakım, tek ACTIVE görev) drone satırı kilitlenerek burada uygulanır.
 */
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
        Operator operator = operatorRepository.findById(request.operatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Operatör", request.operatorId()));
        Drone drone = lockDrone(request.droneId());
        ensureDroneAssignable(drone);

        Mission mission = mapper.toEntity(request, operator, drone);
        if (request.waypoints() != null) {
            request.waypoints().forEach(w -> mission.addWaypoint(w.latitude(), w.longitude(), w.altitude()));
        }
        return mapper.toResponse(missionRepository.save(mission));
    }

    public PageResponse<MissionResponse> findAll(MissionStatus status, MissionPriority priority, Long droneId,
                                                 int page, int size) {
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
        mission.addWaypoint(request.latitude(), request.longitude(), request.altitude());
        missionRepository.flush();
        return mapper.toResponse(mission);
    }

    @Transactional
    public MissionResponse start(Long id) {
        Mission mission = getMission(id);
        mission.ensureStartable();
        Drone drone = lockDrone(mission.getDrone().getId());
        ensureDroneAssignable(drone);
        mission.start();
        return mapper.toResponse(mission);
    }

    @Transactional
    public MissionResponse complete(Long id) {
        Mission mission = getMission(id);
        mission.complete();
        return mapper.toResponse(mission);
    }

    @Transactional
    public MissionResponse cancel(Long id) {
        Mission mission = getMission(id);
        mission.cancel();
        return mapper.toResponse(mission);
    }

    private Mission getMission(Long id) {
        return missionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Görev", id));
    }

    private Drone lockDrone(Long id) {
        return droneRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Drone", id));
    }

    /** Bakımdaki drone'a veya zaten ACTIVE görevi olan drone'a görev atanamaz. */
    private void ensureDroneAssignable(Drone drone) {
        if (drone.getStatus() == DroneStatus.MAINTENANCE) {
            throw new BusinessRuleException(drone.getName() + " şu an bakımda; göreve atanamaz.");
        }
        if (missionRepository.existsByDroneIdAndStatus(drone.getId(), MissionStatus.ACTIVE)) {
            throw new BusinessRuleException(drone.getName() + " zaten ACTIVE bir görevde; ikinci görev atanamaz.");
        }
    }
}
