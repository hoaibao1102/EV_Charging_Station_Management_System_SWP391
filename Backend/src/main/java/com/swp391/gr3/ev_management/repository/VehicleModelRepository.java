package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.VehicleModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {

    boolean existsByBrandIgnoreCaseAndModelIgnoreCaseAndYear(String brand, String model, int year);

    boolean existsByBrandIgnoreCaseAndModelIgnoreCaseAndYearAndModelIdNot(String brand, String model, int year, Long modelId);

    @Query("SELECT vm FROM VehicleModel vm " +
            "WHERE (:brand IS NULL OR LOWER(vm.brand) LIKE LOWER(CONCAT('%', :brand, '%'))) " +
            "AND (:model IS NULL OR LOWER(vm.model) LIKE LOWER(CONCAT('%', :model, '%'))) " +
            "AND (:year IS NULL OR vm.year = :year) " +
            "AND (:connectorTypeId IS NULL OR vm.connectorType.connectorTypeId = :connectorTypeId)")
    List<VehicleModel> search(String brand, String model, Integer year, Integer connectorTypeId);

//    @Query("SELECT vm FROM VehicleModel vm " +
//        "WHERE (:brand IS NULL OR LOWER(vm.brand) LIKE LOWER(CONCAT('%', :brand, '%'))) " +
//        "AND (:model IS NULL OR LOWER(vm.model) LIKE LOWER(CONCAT('%', :model, '%'))) " +
//        "AND (:year IS NULL OR vm.year = :year) " +
//        "AND (:connectorTypeId IS NULL OR vm.connectorType.connectorTypeId = :connectorTypeId)")
//    Page<VehicleModel> searchPage(String brand, String model, Integer year, Integer connectorTypeId, Pageable pageable);
}
