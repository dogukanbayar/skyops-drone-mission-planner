package com.droneops.service;

import com.droneops.domain.Drone;
import com.droneops.domain.DroneStatus;
import com.droneops.domain.MissionStatus;
import com.droneops.domain.Operator;
import com.droneops.dto.DroneRequest;
import com.droneops.dto.DroneResponse;
import com.droneops.dto.DroneStatusRequest;
import com.droneops.dto.OperatorRequest;
import com.droneops.exception.BusinessRuleException;
import com.droneops.exception.ResourceNotFoundException;
import com.droneops.mapper.DroneMapper;
import com.droneops.mapper.OperatorMapper;
import com.droneops.repository.DroneRepository;
import com.droneops.repository.MissionRepository;
import com.droneops.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DroneAndOperatorServiceTest {

    @Mock DroneRepository droneRepository;
    @Mock MissionRepository missionRepository;
    @Mock OperatorRepository operatorRepository;

    DroneService droneService;
    OperatorService operatorService;

    @BeforeEach
    void setUp() {
        droneService = new DroneService(droneRepository, missionRepository, new DroneMapper());
        operatorService = new OperatorService(operatorRepository, new OperatorMapper());
    }

    private Drone drone() {
        Drone d = new Drone();
        d.setId(1L);
        d.setName("Falcon-1");
        d.setStatus(DroneStatus.AVAILABLE);
        return d;
    }

    @Test
    void createDrone_duplicateSerial_isRejected() {
        when(droneRepository.existsBySerialNumberIgnoreCase("SN-1")).thenReturn(true);
        DroneRequest duplicate = new DroneRequest("A", "M", "SN-1", 80);
        assertThrows(BusinessRuleException.class, () -> droneService.create(duplicate));
    }

    @Test
    void createDrone_savesAndMaps() {
        when(droneRepository.existsBySerialNumberIgnoreCase("SN-2")).thenReturn(false);
        when(droneRepository.save(any(Drone.class))).thenAnswer(inv -> inv.getArgument(0));

        DroneResponse response = droneService.create(new DroneRequest(" A ", "M", "SN-2", 80));

        assertEquals("A", response.name());
        assertFalse(response.onActiveMission());
    }

    @Test
    void maintenance_whileOnActiveMission_isRejected() {
        when(droneRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(drone()));
        when(missionRepository.existsByDroneIdAndStatus(1L, MissionStatus.ACTIVE)).thenReturn(true);

        DroneStatusRequest toMaintenance = new DroneStatusRequest(DroneStatus.MAINTENANCE);
        assertThrows(BusinessRuleException.class, () -> droneService.updateStatus(1L, toMaintenance));
    }

    @Test
    void maintenance_whenIdle_updatesStatus() {
        when(droneRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(drone()));
        when(missionRepository.existsByDroneIdAndStatus(1L, MissionStatus.ACTIVE)).thenReturn(false);

        assertEquals(DroneStatus.MAINTENANCE,
                droneService.updateStatus(1L, new DroneStatusRequest(DroneStatus.MAINTENANCE)).status());
    }

    @Test
    void droneLookup_listAndNotFound() {
        when(droneRepository.findAll()).thenReturn(List.of(drone()));
        when(missionRepository.existsByDroneIdAndStatus(1L, MissionStatus.ACTIVE)).thenReturn(true);
        assertTrue(droneService.findAll().get(0).onActiveMission());

        when(droneRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> droneService.findById(9L));
    }

    @Test
    void operator_createDuplicateAndNotFound() {
        when(operatorRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(true);
        OperatorRequest duplicate = new OperatorRequest("A", "a@b.com");
        assertThrows(BusinessRuleException.class, () -> operatorService.create(duplicate));

        when(operatorRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> operatorService.findById(5L));
    }

    @Test
    void operator_createListAndGet() {
        when(operatorRepository.existsByEmailIgnoreCase("x@y.com")).thenReturn(false);
        when(operatorRepository.save(any(Operator.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("x@y.com", operatorService.create(new OperatorRequest("X", "x@y.com")).email());

        Operator op = new Operator();
        op.setId(2L);
        op.setFullName("Z");
        when(operatorRepository.findAll()).thenReturn(List.of(op));
        when(operatorRepository.findById(2L)).thenReturn(Optional.of(op));
        assertEquals(1, operatorService.findAll().size());
        assertEquals("Z", operatorService.findById(2L).fullName());
    }
}
