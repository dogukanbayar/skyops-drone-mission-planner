package com.droneops.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drones")
@Getter
@Setter
@NoArgsConstructor
public class Drone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false, unique = true, length = 60)
    private String serialNumber;

    @Column(nullable = false)
    private int batteryLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DroneStatus status = DroneStatus.AVAILABLE;

    @OneToMany(mappedBy = "drone", fetch = FetchType.LAZY)
    private List<Mission> missions = new ArrayList<>();
}
