package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.DriverViolationTripletResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface DriverViolationTripletService {

    List<DriverViolationTripletResponse> getAllTriplets();
    List<DriverViolationTripletResponse> getTripletsByUserPhone(String phoneNumber);
    DriverViolationTripletResponse updateTripletStatusToPaid(Long tripletId);
    DriverViolationTripletResponse updateTripletStatusToCanceled(Long tripletId);

}
