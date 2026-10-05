package com.droneops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Konum noktası ekleme isteği")
public record WaypointRequest(
        @Schema(description = "Enlem (derece)", example = "41.0256", minimum = "-90", maximum = "90")
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @Schema(description = "Boylam (derece)", example = "28.9744", minimum = "-180", maximum = "180")
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Schema(description = "Rakım (metre)", example = "120", minimum = "0", maximum = "10000")
        @NotNull @DecimalMin("0.0") @DecimalMax("10000.0") Double altitude) {
}
