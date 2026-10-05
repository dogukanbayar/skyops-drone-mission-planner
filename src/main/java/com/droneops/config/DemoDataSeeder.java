package com.droneops.config;

import com.droneops.domain.DroneStatus;
import com.droneops.domain.MissionPriority;
import com.droneops.dto.*;
import com.droneops.exception.BusinessRuleException;
import com.droneops.repository.OperatorRepository;
import com.droneops.service.DroneService;
import com.droneops.service.MissionService;
import com.droneops.service.OperatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * "demo-data" profilinde İstanbul üzerinde rastgele operatör, drone ve görev üretir.
 * Tüm kayıtlar servisler üzerinden oluşturulduğu için iş kuralları üretimde de geçerlidir.
 */
@Slf4j
@Component
@Order(2)
@Profile("demo-data")
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private static final int BASE_OPERATORS = 2;
    private static final int OPERATOR_COUNT = 8;
    private static final int DRONE_COUNT = 12;
    private static final int MISSION_COUNT = 36;
    private static final int MAX_ACTIVE = 4;

    private static final String[] BIRDS = {"Atmaca", "Doğan", "Kerkenez", "Çaylak", "Laçin", "Albatros",
            "Leylek", "Martı", "Kuzgun", "Baykuş", "Turna", "Pelikan"};
    private static final String[] MODELS = {"DJI Matrice 350 RTK", "Autel EVO Max 4T", "Skydio X10",
            "DJI Mavic 3 Enterprise", "Parrot Anafi USA"};
    private static final String[] TASKS = {"Köprü denetimi", "Yangın gözlemi", "Sahil taraması", "Trafik izleme",
            "Altyapı kontrolü", "Arama kurtarma", "Termal tarama", "Şantiye haritalama"};
    private static final String[] DISTRICTS = {"Kadıköy", "Beşiktaş", "Üsküdar", "Sarıyer", "Fatih", "Beylikdüzü",
            "Maltepe", "Bakırköy", "Tuzla", "Eyüpsultan"};

    private final OperatorRepository operatorRepository;
    private final OperatorService operatorService;
    private final DroneService droneService;
    private final MissionService missionService;

    @Override
    public void run(String... args) {
        if (operatorRepository.count() > BASE_OPERATORS) {
            log.info("Demo verisi zaten yüklü, atlanıyor.");
            return;
        }
        Faker faker = new Faker(Locale.forLanguageTag("tr"));
        Random rnd = new Random(42);

        List<Long> operatorIds = new ArrayList<>();
        for (int i = 1; i <= OPERATOR_COUNT; i++) {
            operatorIds.add(operatorService.create(
                    new OperatorRequest(faker.name().fullName(), "demo.operator" + i + "@skyops.io")).id());
        }

        Set<Long> maintenance = new HashSet<>();
        List<Long> droneIds = new ArrayList<>();
        for (int i = 1; i <= DRONE_COUNT; i++) {
            DroneResponse drone = droneService.create(new DroneRequest(BIRDS[i - 1] + "-" + (10 + i),
                    MODELS[rnd.nextInt(MODELS.length)], String.format("SN-DEMO-%04d", i), 15 + rnd.nextInt(86)));
            droneIds.add(drone.id());
            if (i % 6 == 0) {
                droneService.updateStatus(drone.id(), new DroneStatusRequest(DroneStatus.MAINTENANCE));
                maintenance.add(drone.id());
            }
        }

        createMissions(faker, rnd, operatorIds, droneIds, maintenance);
        log.info("Demo verisi yüklendi: {} operatör, {} drone, en fazla {} görev.", OPERATOR_COUNT, DRONE_COUNT, MISSION_COUNT);
    }

    private void createMissions(Faker faker, Random rnd, List<Long> operatorIds, List<Long> droneIds,
                                Set<Long> maintenance) {
        Set<Long> active = new HashSet<>();
        for (int i = 0; i < MISSION_COUNT; i++) {
            List<Long> candidates = droneIds.stream()
                    .filter(id -> !maintenance.contains(id) && !active.contains(id)).toList();
            if (candidates.isEmpty()) {
                break;
            }
            Long droneId = candidates.get(rnd.nextInt(candidates.size()));
            String district = DISTRICTS[rnd.nextInt(DISTRICTS.length)];
            MissionRequest request = new MissionRequest(
                    TASKS[rnd.nextInt(TASKS.length)] + " – " + district,
                    "Bölge: " + district + ", " + faker.address().streetName(),
                    MissionPriority.values()[rnd.nextInt(MissionPriority.values().length)],
                    operatorIds.get(rnd.nextInt(operatorIds.size())), droneId, route(rnd));
            try {
                MissionResponse mission = missionService.create(request);
                advance(mission, droneId, rnd, active);
            } catch (BusinessRuleException e) {
                log.warn("Demo görevi atlandı: {}", e.getMessage());
            }
        }
    }

    /** Görevi rastgele bir yaşam döngüsü noktasına taşır (planlı / aktif / tamamlanmış / iptal). */
    private void advance(MissionResponse mission, Long droneId, Random rnd, Set<Long> active) {
        double roll = rnd.nextDouble();
        boolean routable = mission.waypoints().size() >= 2;
        if (roll < 0.25 || (roll < 0.80 && !routable)) {
            return;
        }
        if (roll < 0.55) {
            if (active.size() < MAX_ACTIVE) {
                missionService.start(mission.id());
                active.add(droneId);
            }
        } else if (roll < 0.80) {
            missionService.start(mission.id());
            missionService.complete(mission.id());
        } else {
            missionService.cancel(mission.id());
        }
    }

    /** İstanbul sınırları içinde, birbirine yakın 0-5 noktalık rastgele rota. */
    private List<WaypointRequest> route(Random rnd) {
        double lat = 40.92 + rnd.nextDouble() * 0.25;
        double lon = 28.65 + rnd.nextDouble() * 0.70;
        List<WaypointRequest> points = new ArrayList<>();
        for (int i = rnd.nextInt(6); i > 0; i--) {
            points.add(new WaypointRequest(round(lat), round(lon), (double) (60 + rnd.nextInt(190))));
            lat += (rnd.nextDouble() - 0.5) * 0.04;
            lon += (rnd.nextDouble() - 0.5) * 0.05;
        }
        return points;
    }

    private static double round(double value) {
        return Math.round(value * 1_000_000d) / 1_000_000d;
    }
}
