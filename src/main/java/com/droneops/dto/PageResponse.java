package com.droneops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Sayfalı liste yanıtı")
public record PageResponse<T>(
        @Schema(description = "Bu sayfadaki kayıtlar") List<T> content,
        @Schema(description = "Sayfa numarası (0'dan başlar)", example = "0") int page,
        @Schema(description = "Sayfa boyutu", example = "20") int size,
        @Schema(description = "Toplam kayıt sayısı", example = "42") long totalElements,
        @Schema(description = "Toplam sayfa sayısı", example = "3") int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
