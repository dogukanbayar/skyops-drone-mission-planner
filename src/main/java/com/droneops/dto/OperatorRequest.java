package com.droneops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Operatör oluşturma isteği")
public record OperatorRequest(
        @Schema(description = "Operatörün adı soyadı", example = "Elif Kaya")
        @NotBlank @Size(max = 100) String fullName,
        @Schema(description = "Benzersiz e-posta adresi", example = "elif.kaya@skyops.io")
        @NotBlank @Email @Size(max = 150) String email) {
}
