package com.droneops.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Tüm hatalar için standart yanıt gövdesi")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(description = "Hatanın oluştuğu an (UTC)") Instant timestamp,
        @Schema(description = "HTTP durum kodu", example = "409") int status,
        @Schema(description = "HTTP durum açıklaması", example = "Conflict") String error,
        @Schema(description = "Kullanıcıya gösterilebilir hata mesajı") String message,
        @Schema(description = "İstek yolu", example = "/api/missions/1/start") String path,
        @Schema(description = "Alan bazlı doğrulama hataları") Map<String, String> validationErrors) {
}
