package com.droneops.service;

import com.droneops.domain.*;
import com.droneops.dto.MissionRequest;
import com.droneops.dto.MissionResponse;
import com.droneops.dto.PageResponse;
import com.droneops.dto.WaypointRequest;
import com.droneops.exception.BusinessRuleException;
import com.droneops.exception.ResourceNotFoundException;
import com.droneops.mapper.MissionMapper;
import com.droneops.repository.DroneRepository;
import com.droneops.repository.MissionRepository;
import com.droneops.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @Mock MissionRepository missionRepository;
    @Mock OperatorRepository operatorRepository;
    @Mock DroneRepository droneRepository;

    MissionService service;
    Operator operator;
    Drone drone;

    @BeforeEach
    void setUp() {
        service = new MissionService(missionRepository, operatorRepository, droneRepository, new MissionMapper());
        operator = new Operator();
        operator.setId(1L);
        operator.setFullName("Elif Kaya");
        drone = new Drone();
        drone.setId(10L);
        drone.setName("Falcon-1");
    }

    /** Önce noktalar eklenir, sonra durum atanır (sonlanmış göreve nokta eklenemez). */
    private Mission mission(MissionStatus status, int waypointCount) {
        Mission m = new Mission();
        m.setId(5L);
        m.setName("Test");
        m.setPriority(MissionPriority.HIGH);
        m.setOperator(operator);
        m.setDrone(drone);
        for (int i = 0; i < waypointCount; i++) {
            m.addWaypoint(41.0 + i, 29.0, 100);
        }
        m.setStatus(status);
        return m;
    }

    private MissionRequest request(List<WaypointRequest> waypoints) {
        return new MissionRequest("Keşif", "açıklama", MissionPriority.HIGH, 1L, 10L, waypoints);
    }

    private void givenOperatorAndLockedDrone() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(droneRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(drone));
    }

    @Test
    void create_withWaypoints_savesPlannedMission() {
        givenOperatorAndLockedDrone();
        when(missionRepository.existsByDroneIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(false);
        when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));

        MissionResponse response = service.create(request(List.of(new WaypointRequest(41.0, 29.0, 100.0))));

        assertEquals(MissionStatus.PLANNED, response.status());
        assertEquals(1, response.waypoints().size());
        assertEquals("Falcon-1", response.droneName());
    }

    @Test
    void create_withoutWaypoints_isAllowed() {
        givenOperatorAndLockedDrone();
        when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(service.create(request(null)).waypoints().isEmpty());
    }

    @Test
    void create_droneInMaintenance_isRejected() {
        drone.setStatus(DroneStatus.MAINTENANCE);
        givenOperatorAndLockedDrone();

        MissionRequest noWaypoints = request(null);
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.create(noWaypoints));
        assertTrue(ex.getMessage().contains("bakımda"));
        verify(missionRepository, never()).save(any());
    }

    @Test
    void create_droneAlreadyActive_isRejected() {
        givenOperatorAndLockedDrone();
        when(missionRepository.existsByDroneIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(true);

        MissionRequest noWaypoints = request(null);
        assertThrows(BusinessRuleException.class, () -> service.create(noWaypoints));
        verify(missionRepository, never()).save(any());
    }

    @Test
    void create_unknownOperator_throwsNotFound() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.empty());
        MissionRequest noWaypoints = request(null);
        assertThrows(ResourceNotFoundException.class, () -> service.create(noWaypoints));
    }

    @Test
    void create_unknownDrone_throwsNotFound() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(droneRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());
        MissionRequest noWaypoints = request(null);
        assertThrows(ResourceNotFoundException.class, () -> service.create(noWaypoints));
    }

    @Test
    void addWaypoint_onFinishedOrCancelled_isRejected() {
        WaypointRequest wp = new WaypointRequest(41.0, 29.0, 50.0);
        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission(MissionStatus.FINISHED, 2)));
        assertThrows(BusinessRuleException.class, () -> service.addWaypoint(5L, wp));

        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission(MissionStatus.CANCELLED, 2)));
        assertThrows(BusinessRuleException.class, () -> service.addWaypoint(5L, wp));
    }

    @Test
    void addWaypoint_onPlanned_appendsWithNextSequence() {
        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission(MissionStatus.PLANNED, 1)));

        MissionResponse response = service.addWaypoint(5L, new WaypointRequest(41.5, 29.5, 70.0));

        assertEquals(2, response.waypoints().size());
        assertEquals(2, response.waypoints().get(1).sequenceNo());
    }

    @Test
    void start_withLessThanTwoWaypoints_isRejected_beforeTouchingDrone() {
        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission(MissionStatus.PLANNED, 1)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.start(5L));
        assertTrue(ex.getMessage().contains("en az 2"));
        verifyNoInteractions(droneRepository);
    }

    @Test
    void start_cancelledMission_isRejected() {
        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission(MissionStatus.CANCELLED, 3)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.start(5L));
        assertTrue(ex.getMessage().contains("CANCELLED"));
    }

    @Test
    void start_nonPlannedMission_isRejected() {
        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission(MissionStatus.ACTIVE, 3)));
        assertThrows(BusinessRuleException.class, () -> service.start(5L));
    }

    @Test
    void start_droneBusyWithAnotherActiveMission_isRejected() {
        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission(MissionStatus.PLANNED, 2)));
        when(droneRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(drone));
        when(missionRepository.existsByDroneIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.start(5L));
    }

    @Test
    void start_droneInMaintenance_isRejected() {
        drone.setStatus(DroneStatus.MAINTENANCE);
        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission(MissionStatus.PLANNED, 2)));
        when(droneRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(drone));

        assertThrows(BusinessRuleException.class, () -> service.start(5L));
    }

    @Test
    void start_validMission_becomesActive() {
        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission(MissionStatus.PLANNED, 2)));
        when(droneRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(drone));
        when(missionRepository.existsByDroneIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(false);

        MissionResponse response = service.start(5L);

        assertEquals(MissionStatus.ACTIVE, response.status());
        assertNotNull(response.startedAt());
    }

    @Test
    void complete_onlyActiveMissionCanFinish() {
        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission(MissionStatus.PLANNED, 2)));
        assertThrows(BusinessRuleException.class, () -> service.complete(5L));

        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission(MissionStatus.ACTIVE, 2)));
        MissionResponse response = service.complete(5L);
        assertEquals(MissionStatus.FINISHED, response.status());
        assertNotNull(response.finishedAt());
    }

    @Test
    void cancel_plannedOrActive_succeeds_finishedOrCancelledFails() {
        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission(MissionStatus.PLANNED, 0)));
        assertEquals(MissionStatus.CANCELLED, service.cancel(5L).status());

        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission(MissionStatus.FINISHED, 2)));
        assertThrows(BusinessRuleException.class, () -> service.cancel(5L));

        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission(MissionStatus.CANCELLED, 2)));
        assertThrows(BusinessRuleException.class, () -> service.cancel(5L));
    }

    @Test
    void findById_unknown_throwsNotFound() {
        when(missionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void findById_existing_isMapped() {
        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission(MissionStatus.PLANNED, 2)));
        assertEquals(2, service.findById(5L).waypoints().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_mapsResults_andClampsPaging() {
        when(missionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mission(MissionStatus.PLANNED, 2))));

        PageResponse<MissionResponse> result = service.findAll(MissionStatus.PLANNED, MissionPriority.HIGH, 10L, -3, 500);

        assertEquals(1, result.content().size());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(missionRepository).findAll(any(Specification.class), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(MissionService.MAX_PAGE_SIZE, captor.getValue().getPageSize());
    }
}
