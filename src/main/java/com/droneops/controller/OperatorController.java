package com.droneops.controller;

import com.droneops.dto.ErrorResponse;
import com.droneops.dto.OperatorRequest;
import com.droneops.dto.OperatorResponse;
import com.droneops.service.OperatorService;
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
@RequestMapping("/api/operators")
@RequiredArgsConstructor
@Tag(name = "Operatörler", description = "Görevleri planlayan operatör yönetimi")
public class OperatorController {

    private final OperatorService operatorService;

    @Operation(summary = "Operatör oluştur", description = "E-posta adresi benzersiz olmalıdır.")
    @ApiResponse(responseCode = "201", description = "Operatör oluşturuldu")
    @ApiResponse(responseCode = "409", description = "E-posta zaten kayıtlı",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<OperatorResponse> create(@Valid @RequestBody OperatorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(operatorService.create(request));
    }

    @Operation(summary = "Operatörleri listele")
    @GetMapping
    public List<OperatorResponse> list() {
        return operatorService.findAll();
    }

    @Operation(summary = "Operatör getir")
    @ApiResponse(responseCode = "404", description = "Operatör bulunamadı",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public OperatorResponse get(@PathVariable Long id) {
        return operatorService.findById(id);
    }
}
