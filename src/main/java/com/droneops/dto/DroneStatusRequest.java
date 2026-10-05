package com.droneops.dto;

import com.droneops.domain.DroneStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Drone durum güncelleme isteği")
public record DroneStatusRequest(
        @Schema(description = "Yeni durum", example = "MAINTENANCE") @NotNull DroneStatus status) {
}
