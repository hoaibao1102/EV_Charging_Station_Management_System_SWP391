package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.DriverViolationTripletResponse;
import com.swp391.gr3.ev_management.entity.DriverViolationTriplet;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public interface DriverViolationTripletService {

    List<DriverViolationTripletResponse> getAllTriplets();

    List<DriverViolationTripletResponse> getTripletsByUserPhone(String phoneNumber);

    DriverViolationTripletResponse updateTripletStatusToPaid(Long tripletId);

    DriverViolationTripletResponse updateTripletStatusToCanceled(Long tripletId);

    boolean existsByViolation(Long violationId);

    Collection<DriverViolationTriplet> findOpenByDriver(Long driverId);

    DriverViolationTriplet save(DriverViolationTriplet triplet);

    void addDriverViolationTriplet(DriverViolationTriplet triplet);
}
