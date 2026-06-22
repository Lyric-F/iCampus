package com.example.demo.repository;

import com.example.demo.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findByCanteenId(Long canteenId);
}