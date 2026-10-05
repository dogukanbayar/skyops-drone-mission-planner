package com.droneops.domain;

import com.droneops.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Görev varlığı; durum geçiş kuralları (yaşam döngüsü) burada tek yerde toplanır. */
@Entity
@Table(name = "missions")
@Getter
@Setter
@NoArgsConstructor
public class Mission {

    public static final int MIN_WAYPOINTS_TO_START = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MissionPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MissionStatus status = MissionStatus.PLANNED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id")
    private Operator operator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drone_id")
    private Drone drone;

    @OneToMany(mappedBy = "mission", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNo ASC")
    @BatchSize(size = 50)
    private List<Waypoint> waypoints = new ArrayList<>();

    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    /** FINISHED (ve CANCELLED) göreve nokta eklenemez; aksi halde sıradaki sıra numarasıyla bağlanır. */
    public Waypoint addWaypoint(double latitude, double longitude, double altitude) {
        if (status == MissionStatus.FINISHED) {
            throw new BusinessRuleException("Tamamlanmış (FINISHED) göreve konum noktası eklenemez.");
        }
        if (status == MissionStatus.CANCELLED) {
            throw new BusinessRuleException("İptal edilmiş (CANCELLED) göreve konum noktası eklenemez.");
        }
        Waypoint waypoint = new Waypoint();
        waypoint.setMission(this);
        waypoint.setSequenceNo(waypoints.size() + 1);
        waypoint.setLatitude(latitude);
        waypoint.setLongitude(longitude);
        waypoint.setAltitude(altitude);
        waypoints.add(waypoint);
        return waypoint;
    }

    /** Başlatma ön koşullarını doğrular (drone kontrolleri serviste yapılır). */
    public void ensureStartable() {
        if (status == MissionStatus.CANCELLED) {
            throw new BusinessRuleException("İptal edilmiş (CANCELLED) görev tekrar ACTIVE yapılamaz.");
        }
        if (status != MissionStatus.PLANNED) {
            throw new BusinessRuleException("Yalnızca PLANNED görev başlatılabilir. Mevcut durum: " + status);
        }
        if (waypoints.size() < MIN_WAYPOINTS_TO_START) {
            throw new BusinessRuleException("Görevi başlatmak için en az " + MIN_WAYPOINTS_TO_START
                    + " konum noktası gerekir. Mevcut: " + waypoints.size());
        }
    }

    public void start() {
        ensureStartable();
        this.status = MissionStatus.ACTIVE;
        this.startedAt = Instant.now();
    }

    public void complete() {
        if (status != MissionStatus.ACTIVE) {
            throw new BusinessRuleException("Yalnızca ACTIVE görev tamamlanabilir. Mevcut durum: " + status);
        }
        this.status = MissionStatus.FINISHED;
        this.finishedAt = Instant.now();
    }

    public void cancel() {
        if (status == MissionStatus.FINISHED || status == MissionStatus.CANCELLED) {
            throw new BusinessRuleException("Görev zaten sonlanmış. Mevcut durum: " + status);
        }
        this.status = MissionStatus.CANCELLED;
        this.finishedAt = Instant.now();
    }
}
