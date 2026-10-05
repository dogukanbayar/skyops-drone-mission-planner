package com.droneops.mapper;

import com.droneops.domain.Drone;
import com.droneops.domain.Mission;
import com.droneops.domain.Operator;
import com.droneops.domain.Waypoint;
import com.droneops.dto.MissionRequest;
import com.droneops.dto.MissionResponse;
import com.droneops.dto.WaypointResponse;
import org.springframework.stereotype.Component;

@Component
public class MissionMapper {

    public Mission toEntity(MissionRequest request, Operator operator, Drone drone) {
        Mission mission = new Mission();
        mission.setName(request.name().trim());
        mission.setDescription(request.description());
        mission.setPriority(request.priority());
        mission.setOperator(operator);
        mission.setDrone(drone);
        return mission;
    }

    public MissionResponse toResponse(Mission mission) {
        return new MissionResponse(
                mission.getId(), mission.getName(), mission.getDescription(),
                mission.getPriority(), mission.getStatus(),
                mission.getOperator().getId(), mission.getOperator().getFullName(),
                mission.getDrone().getId(), mission.getDrone().getName(),
                mission.getWaypoints().stream().map(this::toWaypointResponse).toList(),
                mission.getCreatedAt(), mission.getStartedAt(), mission.getFinishedAt());
    }

    public WaypointResponse toWaypointResponse(Waypoint w) {
        return new WaypointResponse(w.getId(), w.getSequenceNo(), w.getLatitude(), w.getLongitude(), w.getAltitude());
    }
}
