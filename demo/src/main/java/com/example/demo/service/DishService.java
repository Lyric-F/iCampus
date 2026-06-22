package com.example.demo.service;

import com.example.demo.entity.Dish;
import com.example.demo.repository.DishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DishService {

    @Autowired
    private DishRepository dishRepository;

    public List<Dish> getMenuByCanteen(Long canteenId) {
        return dishRepository.findByCanteenId(canteenId);
    }
}