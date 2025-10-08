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

    @Column(name = "StationName", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String stationName;

    @Column(name = "Address", columnDefinition = "NVARCHAR(500)", nullable = false)
    private String address;

    @Column(name = "Latitude", nullable = false)
    private double latitude;

    @Column(name = "Longitude", nullable = false)
    private double longitude;

    @Column(name = "OperatingHours", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String operatingHours;

    @Column(name = "Status", columnDefinition = "NVARCHAR(20)", nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt", nullable = false)
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
