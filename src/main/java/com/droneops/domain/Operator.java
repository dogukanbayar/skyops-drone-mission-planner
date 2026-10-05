package com.droneops.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "operators")
@Getter
@Setter
@NoArgsConstructor
public class Operator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @OneToMany(mappedBy = "operator", fetch = FetchType.LAZY)
    private List<Mission> missions = new ArrayList<>();
}
