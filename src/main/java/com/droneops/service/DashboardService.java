package com.droneops.service;

import com.droneops.domain.DroneStatus;
import com.droneops.domain.MissionStatus;
import com.droneops.dto.DashboardStats;
import com.droneops.repository.DroneRepository;
import com.droneops.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final MissionRepository missionRepository;
    private final DroneRepository droneRepository;

    public DashboardStats stats() {
        return new DashboardStats(
                missionRepository.count(),
                missionRepository.countByStatus(MissionStatus.PLANNED),
                missionRepository.countByStatus(MissionStatus.ACTIVE),
                missionRepository.countByStatus(MissionStatus.FINISHED),
                missionRepository.countByStatus(MissionStatus.CANCELLED),
                droneRepository.count(),
                droneRepository.countByStatus(DroneStatus.MAINTENANCE));
    }
}
