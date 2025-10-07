package com.swp391.gr3.ev_management.entity;

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
    @JoinColumn(name = "ConnectorTypeID")
    private ConnectorType connectorType;

    @Column(name = "Status", length = 20)
    private String status;

    @Column(name = "Date")
    private LocalDateTime date;

    @OneToOne(mappedBy = "slot", fetch = FetchType.LAZY)
    private Booking booking;
}
