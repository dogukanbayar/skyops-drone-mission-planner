package com.droneops.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Görev rotasındaki konum noktası")
public record WaypointResponse(
        @Schema(description = "Nokta kimliği", example = "10") Long id,
        @Schema(description = "Rota üzerindeki sıra", example = "1") int sequenceNo,
        @Schema(description = "Enlem", example = "41.0256") double latitude,
        @Schema(description = "Boylam", example = "28.9744") double longitude,
        @Schema(description = "Rakım (m)", example = "120") double altitude) {
}
