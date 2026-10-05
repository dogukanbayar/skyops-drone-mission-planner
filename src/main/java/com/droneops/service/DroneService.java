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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        Drone drone = droneRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Drone", id));
        if (request.status() == DroneStatus.MAINTENANCE && isOnActiveMission(drone)) {
            throw new BusinessRuleException("ACTIVE görevdeki drone bakıma alınamaz. Önce görevi tamamlayın veya iptal edin.");
        }
        drone.setStatus(request.status());
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
