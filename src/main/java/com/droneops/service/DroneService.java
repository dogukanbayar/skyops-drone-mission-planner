package com.droneops.service;

import com.droneops.domain.Drone;
import com.droneops.domain.DroneStatus;
import com.droneops.domain.MissionStatus;
import com.droneops.dto.DroneRequest;
import com.droneops.dto.DroneResponse;
import com.droneops.dto.DroneStatusRequest;
import com.droneops.exception.BusinessRuleException;
import com.droneops.exception.ResourceNotFoundException;
import com.droneops.mapper.DroneMapper;
import com.droneops.repository.DroneRepository;
import com.droneops.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DroneService {

    private final DroneRepository droneRepository;
    private final MissionRepository missionRepository;
    private final DroneMapper mapper;

    @Transactional
    public DroneResponse create(DroneRequest request) {
        if (droneRepository.existsBySerialNumberIgnoreCase(request.serialNumber().trim())) {
            throw new BusinessRuleException("Bu seri numarasıyla kayıtlı bir drone zaten var.");
        }
        Drone saved = droneRepository.save(mapper.toEntity(request));
        log.info("Drone created: id={}, status={}", saved.getId(), saved.getStatus());
        return mapper.toResponse(saved, false);
    }

    public List<DroneResponse> findAll() {
        return droneRepository.findAll().stream().map(this::toResponse).toList();
    }

    public DroneResponse findById(Long id) {
        return toResponse(getDrone(id));
    }

    @Transactional
    public DroneResponse updateStatus(Long id, DroneStatusRequest request) {
        log.debug("Updating drone status: id={}, requested={}", id, request.status());
        Drone drone = droneRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Drone", id));
        if (request.status() == DroneStatus.MAINTENANCE && isOnActiveMission(drone)) {
            throw new BusinessRuleException("ACTIVE görevdeki drone bakıma alınamaz. Önce görevi tamamlayın veya iptal edin.");
        }
        DroneStatus previous = drone.getStatus();
        drone.setStatus(request.status());
        log.info("Drone status changed: id={}, {} -> {}", drone.getId(), previous, drone.getStatus());
        return toResponse(drone);
    }

    private Drone getDrone(Long id) {
        return droneRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Drone", id));
    }

    private boolean isOnActiveMission(Drone drone) {
        return missionRepository.existsByDroneIdAndStatus(drone.getId(), MissionStatus.ACTIVE);
    }

    private DroneResponse toResponse(Drone drone) {
        return mapper.toResponse(drone, isOnActiveMission(drone));
    }
}
