package com.drivio.repository;

import com.drivio.entity.User;
import com.drivio.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByUser(User user);
    List<Vehicle> findByUserId(Long userId);
}

