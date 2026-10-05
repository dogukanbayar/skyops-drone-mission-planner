package com.droneops.dto;

import com.droneops.domain.MissionPriority;
import com.droneops.domain.MissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Görev detayı")
public record MissionResponse(
        @Schema(description = "Görev kimliği", example = "1") Long id,
        @Schema(description = "Görev adı") String name,
        @Schema(description = "Açıklama") String description,
        @Schema(description = "Öncelik") MissionPriority priority,
        @Schema(description = "Görev durumu") MissionStatus status,
        @Schema(description = "Operatör kimliği") Long operatorId,
        @Schema(description = "Operatör adı") String operatorName,
        @Schema(description = "Drone kimliği") Long droneId,
        @Schema(description = "Drone adı") String droneName,
        @Schema(description = "Sıralı konum noktaları") List<WaypointResponse> waypoints,
        @Schema(description = "Oluşturulma zamanı (UTC)") Instant createdAt,
        @Schema(description = "Başlangıç zamanı (UTC)") Instant startedAt,
        @Schema(description = "Bitiş/iptal zamanı (UTC)") Instant finishedAt) {
}
