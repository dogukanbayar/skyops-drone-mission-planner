package com.droneops.dto;

import com.droneops.domain.DroneStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Drone bilgisi")
public record DroneResponse(
        @Schema(description = "Drone kimliği", example = "1") Long id,
        @Schema(description = "Çağrı adı", example = "Falcon-1") String name,
        @Schema(description = "Model", example = "DJI Matrice 350 RTK") String model,
        @Schema(description = "Seri numarası", example = "SN-FAL-0001") String serialNumber,
        @Schema(description = "Batarya (%)", example = "92") int batteryLevel,
        @Schema(description = "Filo durumu") DroneStatus status,
        @Schema(description = "Şu anda ACTIVE bir görevde mi?", example = "false") boolean onActiveMission) {
}
