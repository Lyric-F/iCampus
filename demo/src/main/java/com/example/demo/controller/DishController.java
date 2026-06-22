package com.example.demo.controller;

import com.example.demo.entity.Dish;
import com.example.demo.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/canteen")
@CrossOrigin(origins = "*")
public class DishController {

    @Autowired
    private DishService dishService;

    @GetMapping("/{canteenId}/menu")
    public List<Dish> getMenu(@PathVariable Long canteenId) {
        return dishService.getMenuByCanteen(canteenId);
    }
}