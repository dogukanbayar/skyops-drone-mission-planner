package com.droneops.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Operatör bilgisi")
public record OperatorResponse(
        @Schema(description = "Operatör kimliği", example = "1") Long id,
        @Schema(description = "Adı soyadı", example = "Elif Kaya") String fullName,
        @Schema(description = "E-posta", example = "elif.kaya@skyops.io") String email) {
}
