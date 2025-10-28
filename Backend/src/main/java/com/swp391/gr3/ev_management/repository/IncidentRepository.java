package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

}
