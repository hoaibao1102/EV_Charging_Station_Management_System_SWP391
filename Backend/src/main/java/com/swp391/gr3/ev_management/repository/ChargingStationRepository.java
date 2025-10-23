    package com.swp391.gr3.ev_management.repository;

    import com.swp391.gr3.ev_management.entity.ChargingStation;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.stereotype.Repository;

    @Repository
    public interface ChargingStationRepository extends JpaRepository<ChargingStation, Long> {
        ChargingStation findByStationId(long id);
    }
