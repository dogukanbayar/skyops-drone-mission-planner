package com.droneops.dto;

import com.droneops.domain.MissionPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Görev oluşturma isteği")
public record MissionRequest(
        @Schema(description = "Görev adı", example = "Boğaz köprüleri keşfi")
        @NotBlank @Size(max = 120) String name,
        @Schema(description = "Görev açıklaması", example = "Köprü ayakları ve kablo gerginliği görsel denetimi")
        @Size(max = 1000) String description,
        @Schema(description = "Öncelik seviyesi", example = "HIGH") @NotNull MissionPriority priority,
        @Schema(description = "Görevi oluşturan operatörün kimliği", example = "1") @NotNull Long operatorId,
        @Schema(description = "Görevi gerçekleştirecek drone kimliği", example = "1") @NotNull Long droneId,
        @Schema(description = "Opsiyonel: oluşturma anında eklenecek konum noktaları")
        @Valid List<WaypointRequest> waypoints) {
}
