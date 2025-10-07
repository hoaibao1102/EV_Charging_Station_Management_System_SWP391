package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "SlotConfig")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SlotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ConfigID")
    private Long configId;

    @Column(name = "Slot_duration_min")
    private int slotDurationMin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StationID")
    private ChargingStation station;

    @Column(name = "Active_from")
    private LocalDateTime activeFrom;

    @Column(name = "Active_expire")
    private LocalDateTime activeExpire;

    @Column(name = "Is_Active", length = 20)
    private String isActive;

    @OneToMany(mappedBy = "config", fetch = FetchType.LAZY)
    private List<SlotTemplate> slotTemplates;
}
