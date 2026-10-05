package com.droneops.mapper;

import com.droneops.domain.Drone;
import com.droneops.dto.DroneRequest;
import com.droneops.dto.DroneResponse;
import org.springframework.stereotype.Component;

@Component
public class DroneMapper {

    public Drone toEntity(DroneRequest request) {
        Drone drone = new Drone();
        drone.setName(request.name().trim());
        drone.setModel(request.model().trim());
        drone.setSerialNumber(request.serialNumber().trim());
        drone.setBatteryLevel(request.batteryLevel());
        return drone;
    }

    public DroneResponse toResponse(Drone drone, boolean onActiveMission) {
        return new DroneResponse(drone.getId(), drone.getName(), drone.getModel(), drone.getSerialNumber(),
                drone.getBatteryLevel(), drone.getStatus(), onActiveMission);
    }
}
