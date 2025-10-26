package com.swp391.gr3.ev_management.entity;

import com.swp391.gr3.ev_management.enums.SlotStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "SlotAvailability")
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class SlotAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SlotID")
    private Long slotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TemplateID")
    private SlotTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PointID")
    private ChargingPoint chargingPoint;

    @Column(name = "Status", columnDefinition = "NVARCHAR(20)")
    @Enumerated(EnumType.STRING)
    private SlotStatus status;

    @OneToOne(mappedBy = "slot", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private BookingSlot bookingSlot;

    @Column(name = "Date")
    private LocalDateTime date;

}