package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    public User findUsersByPhoneNumberAndPasswordHash(String phoneNumber , String password);
    public User findUsersByPhoneNumber(String phoneNumber);
    public User findUserByUserId(Long userId);
    public boolean existsByPhoneNumber(String phoneNumber);
    public boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "role")
    public User findByEmail(String email);

    @EntityGraph(attributePaths = "role")
    User findByPhoneNumber(String phone);

    @Query("select u from User u join fetch u.role where u.email = :email")
    User findByEmailWithRole(@Param("email") String email);

    @Query("select u from User u join fetch u.role where u.phoneNumber = :phone")
    User findByPhoneWithRole(@Param("phone") String phone);

    @Query("""
       select u from User u
       left join fetch u.role r
       left join fetch u.driver d
       left join fetch u.staffs s
       left join fetch u.admin a
       """)
    List<User> findAllWithJoins();
}
