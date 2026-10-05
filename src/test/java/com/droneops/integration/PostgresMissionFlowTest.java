package com.droneops.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gerçek PostgreSQL üzerinde yaşam döngüsü ve kilit davranışı. Docker gerektirdiği için varsayılan
 * build'e dahil değildir: {@code mvn test -Dexcluded.groups=none -Dtest=PostgresMissionFlowTest}
 */
@Tag("postgres")
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class PostgresMissionFlowTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    MockMvc mvc;

    private long createEntity(String url, String json) throws Exception {
        String body = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void lifecycleAndRulesWorkOnRealPostgres() throws Exception {
        long op = createEntity("/api/operators", "{\"fullName\":\"PG Op\",\"email\":\"pg@skyops.io\"}");
        long dr = createEntity("/api/drones", "{\"name\":\"PG-1\",\"model\":\"M\",\"serialNumber\":\"SN-PG-1\",\"batteryLevel\":80}");
        String body = "{\"name\":\"PG görevi\",\"priority\":\"HIGH\",\"operatorId\":" + op + ",\"droneId\":" + dr
                + ",\"waypoints\":[{\"latitude\":41.0,\"longitude\":29.0,\"altitude\":100},"
                + "{\"latitude\":41.1,\"longitude\":29.1,\"altitude\":100}]}";
        long mission = createEntity("/api/missions", body);

        mvc.perform(post("/api/missions/" + mission + "/start")).andExpect(status().isOk());
        mvc.perform(post("/api/missions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/missions").param("status", "ACTIVE").param("droneId", String.valueOf(dr)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isNotEmpty());
        mvc.perform(post("/api/missions/" + mission + "/complete")).andExpect(status().isOk());
    }
}
