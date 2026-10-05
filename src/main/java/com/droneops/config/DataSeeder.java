package com.droneops.config;

import com.droneops.domain.DroneStatus;
import com.droneops.domain.MissionPriority;
import com.droneops.dto.*;
import com.droneops.repository.OperatorRepository;
import com.droneops.service.DroneService;
import com.droneops.service.MissionService;
import com.droneops.service.OperatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/** Boş veritabanına İstanbul üzerinde örnek filo ve görevler ekler (droneops.seed.enabled=false ile kapatılır). */
@Component
@Order(1)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "droneops.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final OperatorRepository operatorRepository;
    private final OperatorService operatorService;
    private final DroneService droneService;
    private final MissionService missionService;

    @Override
    public void run(String... args) {
        if (operatorRepository.count() > 0) {
            return;
        }
        OperatorResponse elif = operatorService.create(new OperatorRequest("Elif Kaya", "elif.kaya@skyops.io"));
        operatorService.create(new OperatorRequest("Mert Demir", "mert.demir@skyops.io"));

        DroneResponse falcon = droneService.create(new DroneRequest("Falcon-1", "DJI Matrice 350 RTK", "SN-FAL-0001", 92));
        DroneResponse kartal = droneService.create(new DroneRequest("Kartal-2", "Autel EVO Max 4T", "SN-KAR-0002", 78));
        DroneResponse albatros = droneService.create(new DroneRequest("Albatros-3", "DJI Mavic 3 Enterprise", "SN-ALB-0003", 55));
        DroneResponse sahin = droneService.create(new DroneRequest("Sahin-4", "Skydio X10", "SN-SAH-0004", 100));
        droneService.updateStatus(albatros.id(), new DroneStatusRequest(DroneStatus.MAINTENANCE));

        MissionResponse done = missionService.create(new MissionRequest("Sahil şeridi taraması",
                "Caddebostan - Moda kıyı çizgisi fotogrametri uçuşu", MissionPriority.LOW, elif.id(), kartal.id(),
                List.of(wp(40.9650, 29.0600, 90), wp(40.9780, 29.0250, 90))));
        missionService.start(done.id());
        missionService.complete(done.id());

        MissionResponse bosphorus = missionService.create(new MissionRequest("Boğaz köprüleri keşfi",
                "Galata'dan 15 Temmuz Şehitler Köprüsü ve FSM'ye görsel denetim hattı", MissionPriority.HIGH,
                elif.id(), falcon.id(),
                List.of(wp(41.0256, 28.9744, 120), wp(41.0451, 29.0340, 150), wp(41.0916, 29.0562, 150))));
        missionService.start(bosphorus.id());

        missionService.create(new MissionRequest("Haliç altyapı denetimi",
                "Haliç kıyı yapıları için termal tarama", MissionPriority.MEDIUM, elif.id(), kartal.id(),
                List.of(wp(41.0412, 28.9496, 80), wp(41.0486, 28.9421, 80), wp(41.0540, 28.9330, 80))));

        missionService.create(new MissionRequest("Orman yangını gözlem uçuşu",
                "Acil: Belgrad Ormanı duman tespiti. Rota henüz tamamlanmadı.", MissionPriority.CRITICAL,
                elif.id(), sahin.id(), List.of(wp(41.1830, 28.9880, 200))));
    }

    private static WaypointRequest wp(double lat, double lon, double alt) {
        return new WaypointRequest(lat, lon, alt);
    }
}
