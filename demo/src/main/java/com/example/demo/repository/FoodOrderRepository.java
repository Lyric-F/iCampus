package com.example.demo.repository;

import com.example.demo.entity.FoodOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {
    List<FoodOrder> findByStudentIdOrderByCreateTimeDesc(String studentId);
}