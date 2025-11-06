package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    // Đủ “đường đi” như bạn yêu cầu + fetch để tránh N+1
    // (vì Transaction.invoice là NOT NULL nên join fetch invoice là an toàn)
    @Query("""
           select distinct t
           from Transaction t
             join fetch t.invoice i
             join fetch i.session s
             join fetch s.booking b
             join fetch b.vehicle v
             join fetch v.driver d
             join fetch d.user u
           where u.userId = :userId
           order by t.createdAt desc
           """)
    List<Transaction> findAllDeepGraphByDriverUserId(@Param("userId") Long userId);
}