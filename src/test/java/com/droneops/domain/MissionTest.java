package com.droneops.domain;

import com.droneops.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MissionTest {

    private Mission planned(int waypoints) {
        Mission m = new Mission();
        for (int i = 0; i < waypoints; i++) {
            m.addWaypoint(41.0 + i, 29.0, 100);
        }
        return m;
    }

    @Test
    void newMission_isPlanned_andWaypointsAreNumberedInOrder() {
        Mission m = planned(3);
        assertEquals(MissionStatus.PLANNED, m.getStatus());
        assertEquals(3, m.getWaypoints().get(2).getSequenceNo());
        assertSame(m, m.getWaypoints().get(0).getMission());
    }

    @Test
    void start_requiresTwoWaypoints() {
        Mission oneWaypoint = planned(1);
        assertThrows(BusinessRuleException.class, oneWaypoint::start);
        Mission ok = planned(2);
        ok.start();
        assertEquals(MissionStatus.ACTIVE, ok.getStatus());
        assertNotNull(ok.getStartedAt());
    }

    @Test
    void start_isOnlyAllowedFromPlanned() {
        Mission active = planned(2);
        active.start();
        assertThrows(BusinessRuleException.class, active::start);
    }

    @Test
    void cancelledMission_neverBecomesActiveAgain() {
        Mission m = planned(2);
        m.cancel();
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, m::start);
        assertTrue(ex.getMessage().contains("CANCELLED"));
        assertThrows(BusinessRuleException.class, m::cancel);
    }

    @Test
    void complete_onlyFromActive_thenFinishedBlocksWaypointsAndCancel() {
        Mission m = planned(2);
        assertThrows(BusinessRuleException.class, m::complete);
        m.start();
        m.complete();
        assertEquals(MissionStatus.FINISHED, m.getStatus());
        assertNotNull(m.getFinishedAt());
        assertThrows(BusinessRuleException.class, () -> m.addWaypoint(1, 1, 1));
        assertThrows(BusinessRuleException.class, m::cancel);
    }

    @Test
    void cancelledMission_rejectsWaypoints() {
        Mission m = planned(0);
        m.cancel();
        assertThrows(BusinessRuleException.class, () -> m.addWaypoint(1, 1, 1));
    }
}
