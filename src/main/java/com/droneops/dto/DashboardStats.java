package com.droneops.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Komuta merkezi özet sayıları")
public record DashboardStats(
        @Schema(description = "Toplam görev") long totalMissions,
        @Schema(description = "Planlanan görev") long planned,
        @Schema(description = "Aktif görev") long active,
        @Schema(description = "Tamamlanan görev") long finished,
        @Schema(description = "İptal edilen görev") long cancelled,
        @Schema(description = "Toplam drone") long totalDrones,
        @Schema(description = "Bakımdaki drone") long inMaintenance) {
}
