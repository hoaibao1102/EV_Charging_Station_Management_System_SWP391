package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ChargingStations")
@Data
public class ChargingStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stationId;

    @Column(name = "station_name", columnDefinition = "NVARCHAR(200)", nullable = true)
    private String stationName;

    @Column(name = "address", columnDefinition = "NVARCHAR(400)", nullable = true)
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "status", columnDefinition = "NVARCHAR(10)")
    private String status;

    @CreationTimestamp
    private LocalDateTime createAt;

    @UpdateTimestamp
    private LocalDateTime updateAt;

    @OneToMany(mappedBy = "chargingstation")
    private List<Booking> booking;

    @OneToMany(mappedBy = "chargingstation")
    private List<ChargingPoint> chargingpoint;
}
