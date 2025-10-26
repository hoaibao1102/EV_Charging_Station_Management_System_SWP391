package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "SlotTemplate")
@Data
@NoArgsConstructor  @AllArgsConstructor @Builder
public class SlotTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TemplateID")
    private Long templateId;

    @Column(name = "Slot_index", nullable = false)
    private int slotIndex;

    @Column(name = "Start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "End_time" , nullable = false)
    private LocalDateTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ConfigID", nullable = false)
    private SlotConfig config;

    @OneToMany(mappedBy = "template", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SlotAvailability> slotAvailabilities;
}
