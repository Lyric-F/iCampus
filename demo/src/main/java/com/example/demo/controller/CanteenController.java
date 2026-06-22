package com.example.demo.controller;

import com.example.demo.entity.Canteen;
import com.example.demo.service.CanteenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/canteen")
@CrossOrigin(origins = "*")
public class CanteenController {

    @Autowired
    private CanteenService canteenService;

    @GetMapping("/list")
    public List<Canteen> list() {
        return canteenService.getAllCanteens();
    }
}