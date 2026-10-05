package com.droneops.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Uçtan uca: Controller -> Service -> Repository -> (H2 PostgreSQL modu) akışı. */
@SpringBootTest
@AutoConfigureMockMvc
class MissionApiIntegrationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired MockMvc mvc;

    private long create(String url, String json) throws Exception {
        String body = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long operator() throws Exception {
        int n = SEQ.incrementAndGet();
        return create("/api/operators", "{\"fullName\":\"Op " + n + "\",\"email\":\"op" + n + "@skyops.io\"}");
    }

    private long drone() throws Exception {
        int n = SEQ.incrementAndGet();
        return create("/api/drones", "{\"name\":\"D" + n + "\",\"model\":\"M\",\"serialNumber\":\"SN-IT-" + n + "\",\"batteryLevel\":90}");
    }

    private long mission(long operatorId, long droneId) throws Exception {
        return create("/api/missions", "{\"name\":\"Görev\",\"priority\":\"HIGH\",\"operatorId\":" + operatorId
                + ",\"droneId\":" + droneId + "}");
    }

    private ResultActions addWaypoint(long missionId, double lat) throws Exception {
        return mvc.perform(post("/api/missions/" + missionId + "/waypoints").contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":" + lat + ",\"longitude\":29.0,\"altitude\":100}"));
    }

    @Test
    void fullLifecycle_createStartCompleteAndFinishedBlocksWaypoints() throws Exception {
        long op = operator();
        long dr = drone();
        long m = mission(op, dr);

        addWaypoint(m, 41.0).andExpect(status().isCreated());
        mvc.perform(post("/api/missions/" + m + "/start")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("en az 2")));

        addWaypoint(m, 41.1).andExpect(status().isCreated()).andExpect(jsonPath("$.waypoints", hasSize(2)));
        mvc.perform(post("/api/missions/" + m + "/start")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));
        mvc.perform(get("/api/drones/" + dr)).andExpect(jsonPath("$.onActiveMission", is(true)));

        mvc.perform(post("/api/missions/" + m + "/complete")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FINISHED")));
        addWaypoint(m, 41.2).andExpect(status().isConflict());
        mvc.perform(get("/api/missions/" + m)).andExpect(status().isOk()).andExpect(jsonPath("$.waypoints", hasSize(2)));
    }

    @Test
    void secondMissionOnActiveDrone_isRejected_andMaintenanceBlocked() throws Exception {
        long op = operator();
        long dr = drone();
        long first = mission(op, dr);
        addWaypoint(first, 41.0).andExpect(status().isCreated());
        addWaypoint(first, 41.1).andExpect(status().isCreated());
        mvc.perform(post("/api/missions/" + first + "/start")).andExpect(status().isOk());

        mvc.perform(post("/api/missions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"İkinci\",\"priority\":\"LOW\",\"operatorId\":" + op + ",\"droneId\":" + dr + "}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/drones/" + dr + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"MAINTENANCE\"}")).andExpect(status().isConflict());
    }

    @Test
    void maintenanceDrone_cannotBeAssigned() throws Exception {
        long op = operator();
        long dr = drone();
        mvc.perform(patch("/api/drones/" + dr + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"MAINTENANCE\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("MAINTENANCE")));

        mvc.perform(post("/api/missions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"priority\":\"LOW\",\"operatorId\":" + op + ",\"droneId\":" + dr + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelledMission_cannotBeStartedAgain() throws Exception {
        long m = mission(operator(), drone());
        addWaypoint(m, 41.0).andExpect(status().isCreated());
        addWaypoint(m, 41.1).andExpect(status().isCreated());

        mvc.perform(post("/api/missions/" + m + "/cancel")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
        mvc.perform(post("/api/missions/" + m + "/start")).andExpect(status().isConflict());
    }

    @Test
    void validationAndErrorFormat() throws Exception {
        mvc.perform(post("/api/missions").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.name").exists());

        long m = mission(operator(), drone());
        addWaypoint(m, 95.0).andExpect(status().isBadRequest());
        mvc.perform(post("/api/missions").contentType(MediaType.APPLICATION_JSON).content("{bozuk json"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/missions/abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/missions/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path", is("/api/missions/999999")));
        mvc.perform(get("/api/bilinmeyen")).andExpect(status().isNotFound());
    }

    @Test
    void duplicatesAreRejected_andListsWork() throws Exception {
        int n = SEQ.incrementAndGet();
        String opJson = "{\"fullName\":\"Dup\",\"email\":\"dup" + n + "@skyops.io\"}";
        long opId = create("/api/operators", opJson);
        mvc.perform(post("/api/operators").contentType(MediaType.APPLICATION_JSON).content(opJson))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/operators/" + opId)).andExpect(status().isOk());
        mvc.perform(get("/api/operators")).andExpect(status().isOk());

        String droneJson = "{\"name\":\"Dup\",\"model\":\"M\",\"serialNumber\":\"SN-DUP-" + n + "\",\"batteryLevel\":50}";
        create("/api/drones", droneJson);
        mvc.perform(post("/api/drones").contentType(MediaType.APPLICATION_JSON).content(droneJson))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/drones")).andExpect(status().isOk());
    }

    @Test
    void filtersAndDashboard() throws Exception {
        long op = operator();
        long dr = drone();
        long m = mission(op, dr);

        mvc.perform(get("/api/missions").param("status", "PLANNED").param("priority", "HIGH").param("droneId", String.valueOf(dr)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is((int) m)));
        mvc.perform(get("/api/missions").param("status", "FINISHED").param("droneId", String.valueOf(dr)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(get("/api/dashboard/stats")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMissions").isNumber());
    }

    @Test
    void pagination_returnsRequestedPage() throws Exception {
        long op = operator();
        long dr = drone();
        for (int i = 0; i < 3; i++) {
            mission(op, dr);
        }
        mvc.perform(get("/api/missions").param("droneId", String.valueOf(dr)).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)));
        mvc.perform(get("/api/missions").param("droneId", String.valueOf(dr)).param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void concurrentStarts_onSameDrone_onlyOneSucceeds() throws Exception {
        long op = operator();
        long dr = drone();
        long[] missions = {mission(op, dr), mission(op, dr)};
        for (long m : missions) {
            addWaypoint(m, 41.0).andExpect(status().isCreated());
            addWaypoint(m, 41.1).andExpect(status().isCreated());
        }

        CountDownLatch gate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (long m : missions) {
                results.add(pool.submit(() -> {
                    gate.await();
                    return mvc.perform(post("/api/missions/" + m + "/start")).andReturn().getResponse().getStatus();
                }));
            }
            gate.countDown();
            List<Integer> codes = new ArrayList<>();
            for (Future<Integer> result : results) {
                codes.add(result.get(20, TimeUnit.SECONDS));
            }
            assertEquals(1, codes.stream().filter(c -> c == 200).count(), "Tam olarak bir başlatma başarılı olmalı: " + codes);
            assertEquals(1, codes.stream().filter(c -> c == 409).count(), "Diğeri iş kuralı ile reddedilmeli: " + codes);
        } finally {
            pool.shutdownNow();
        }
    }
}
