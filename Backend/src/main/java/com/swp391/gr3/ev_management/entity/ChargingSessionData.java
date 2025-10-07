package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table (name = "ChargingSessionData")
@Data @NoArgsConstructor @AllArgsConstructor
@Builder
public class ChargingSessionData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dataId;


    @Column(name = "time_stamp")
    private double timestamp;


    @Column(name = "current_SOC")
    private double currentSOC;


    @Column(name = "power_KW")
    private double powerKW;


    @Column(name = "voltage")
    private double voltage;


    @Column(name = "current")
    private double current;


    @Column(name = "temperature")
    private double temperature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SessionID")
    private ChargingSession session;
}
