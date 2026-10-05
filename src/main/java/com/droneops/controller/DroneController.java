package com.droneops.controller;

import com.droneops.dto.DroneRequest;
import com.droneops.dto.DroneResponse;
import com.droneops.dto.DroneStatusRequest;
import com.droneops.dto.ErrorResponse;
import com.droneops.service.DroneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drones")
@RequiredArgsConstructor
@Tag(name = "Dronelar", description = "Filo yönetimi ve bakım durumu")
public class DroneController {

    private final DroneService droneService;

    @Operation(summary = "Drone ekle", description = "Seri numarası benzersiz olmalıdır.")
    @ApiResponse(responseCode = "201", description = "Drone eklendi")
    @ApiResponse(responseCode = "409", description = "Seri numarası zaten kayıtlı",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<DroneResponse> create(@Valid @RequestBody DroneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(droneService.create(request));
    }

    @Operation(summary = "Filoyu listele", description = "Her drone için ACTIVE görevde olup olmadığı da döner.")
    @GetMapping
    public List<DroneResponse> list() {
        return droneService.findAll();
    }

    @Operation(summary = "Drone getir")
    @ApiResponse(responseCode = "404", description = "Drone bulunamadı",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public DroneResponse get(@PathVariable Long id) {
        return droneService.findById(id);
    }

    @Operation(summary = "Drone durumunu güncelle",
            description = "Bakıma alma/servise döndürme. ACTIVE görevdeki drone bakıma alınamaz.")
    @ApiResponse(responseCode = "409", description = "Drone ACTIVE görevde",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id}/status")
    public DroneResponse updateStatus(@PathVariable Long id, @Valid @RequestBody DroneStatusRequest request) {
        return droneService.updateStatus(id, request);
    }
}
