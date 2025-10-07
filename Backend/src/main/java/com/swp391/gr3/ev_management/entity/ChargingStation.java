package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ChargingStations")
@Data @NoArgsConstructor
@AllArgsConstructor @Builder
public class ChargingStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StationID")
    private Long stationId;

    @Column(name = "StationName", length = 255)
    private String stationName;

    @Column(name = "Address", length = 500)
    private String address;

    @Column(name = "Latitude")
    private double latitude;

    @Column(name = "Longitude")
    private double longitude;

    @Column(name = "OperatingHours", length = 50)
    private String operatingHours;

    @Column(name = "Status", length = 20)
    private String status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "station", fetch = FetchType.LAZY)
    private List<ChargingPoint> points;

    @OneToMany(mappedBy = "station", fetch = FetchType.LAZY)
    private List<SlotConfig> slotConfigs;

    @OneToMany(mappedBy = "station", fetch = FetchType.LAZY)
    private List<StationStaff> stationStaffs;

    @OneToMany(mappedBy = "station", fetch = FetchType.LAZY)
    private List<Incident> incidents;

    @OneToMany(mappedBy = "station", fetch = FetchType.LAZY)
    private List<Booking> bookings;
}
