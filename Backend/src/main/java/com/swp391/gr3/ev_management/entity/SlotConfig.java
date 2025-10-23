package com.swp391.gr3.ev_management.entity;

import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
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

    @Column(name = "Slot_duration_min", nullable = false)
    private int slotDurationMin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StationID", nullable = false)
    private ChargingStation station;

    @Column(name = "Active_from", nullable = false)
    private LocalDateTime activeFrom;

    @Column(name = "Active_expire", nullable = false)
    private LocalDateTime activeExpire;

    @Enumerated(EnumType.STRING)
    @Column(name = "Is_Active", columnDefinition = "NVARCHAR(20)", nullable = false)
    private SlotConfigStatus isActive;

    @OneToMany(mappedBy = "config", fetch = FetchType.LAZY)
    private List<SlotTemplate> slotTemplates;
}
