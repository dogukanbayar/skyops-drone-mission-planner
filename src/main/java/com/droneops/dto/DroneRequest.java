package com.droneops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Drone oluşturma isteği")
public record DroneRequest(
        @Schema(description = "Drone çağrı adı", example = "Falcon-1")
        @NotBlank @Size(max = 100) String name,
        @Schema(description = "Drone modeli", example = "DJI Matrice 350 RTK")
        @NotBlank @Size(max = 100) String model,
        @Schema(description = "Benzersiz seri numarası", example = "SN-FAL-0001")
        @NotBlank @Size(max = 60) String serialNumber,
        @Schema(description = "Batarya seviyesi (%)", example = "92", minimum = "0", maximum = "100")
        @NotNull @Min(0) @Max(100) Integer batteryLevel) {
}
