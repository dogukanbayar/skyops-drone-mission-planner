package com.droneops.controller;

import com.droneops.domain.MissionPriority;
import com.droneops.domain.MissionStatus;
import com.droneops.dto.ErrorResponse;
import com.droneops.dto.MissionRequest;
import com.droneops.dto.MissionResponse;
import com.droneops.dto.PageResponse;
import com.droneops.dto.WaypointRequest;
import com.droneops.service.MissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
@Tag(name = "Görevler", description = "Görev planlama, konum noktaları ve yaşam döngüsü")
public class MissionController {

    private static final String CONFLICT_DESC = "İş kuralı ihlali";

    private final MissionService missionService;

    @Operation(summary = "Görev oluştur",
            description = "Görev PLANNED olarak başlar. Drone bakımdaysa veya ACTIVE görevdeyse atanamaz.")
    @ApiResponse(responseCode = "201", description = "Görev oluşturuldu")
    @ApiResponse(responseCode = "400", description = "Doğrulama hatası",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = CONFLICT_DESC,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<MissionResponse> create(@Valid @RequestBody MissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(missionService.create(request));
    }

    @Operation(summary = "Görevleri listele",
            description = "Duruma, önceliğe ve drone'a göre opsiyonel filtreleme; sayfalıdır (varsayılan 20, en fazla 100).")
    @GetMapping
    public PageResponse<MissionResponse> list(
            @Parameter(description = "Görev durumu") @RequestParam(required = false) MissionStatus status,
            @Parameter(description = "Öncelik") @RequestParam(required = false) MissionPriority priority,
            @Parameter(description = "Drone kimliği") @RequestParam(required = false) Long droneId,
            @Parameter(description = "Sayfa numarası (0'dan başlar)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Sayfa boyutu (1-100)") @RequestParam(defaultValue = "20") int size) {
        return missionService.findAll(status, priority, droneId, page, size);
    }

    @Operation(summary = "Görev detayı getir")
    @ApiResponse(responseCode = "404", description = "Görev bulunamadı",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public MissionResponse get(@PathVariable Long id) {
        return missionService.findById(id);
    }

    @Operation(summary = "Konum noktası ekle", description = "FINISHED (ve CANCELLED) göreve nokta eklenemez.")
    @ApiResponse(responseCode = "201", description = "Nokta eklendi, güncel görev döner")
    @ApiResponse(responseCode = "409", description = CONFLICT_DESC,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/waypoints")
    public ResponseEntity<MissionResponse> addWaypoint(@PathVariable Long id, @Valid @RequestBody WaypointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(missionService.addWaypoint(id, request));
    }

    @Operation(summary = "Görevi başlat",
            description = "En az 2 konum noktası gerekir. CANCELLED görev tekrar ACTIVE olamaz.")
    @ApiResponse(responseCode = "409", description = CONFLICT_DESC,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/start")
    public MissionResponse start(@PathVariable Long id) {
        return missionService.start(id);
    }

    @Operation(summary = "Görevi tamamla", description = "Yalnızca ACTIVE görev tamamlanabilir.")
    @ApiResponse(responseCode = "409", description = CONFLICT_DESC,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/complete")
    public MissionResponse complete(@PathVariable Long id) {
        return missionService.complete(id);
    }

    @Operation(summary = "Görevi iptal et", description = "PLANNED veya ACTIVE görev iptal edilebilir.")
    @ApiResponse(responseCode = "409", description = CONFLICT_DESC,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/cancel")
    public MissionResponse cancel(@PathVariable Long id) {
        return missionService.cancel(id);
    }
}
