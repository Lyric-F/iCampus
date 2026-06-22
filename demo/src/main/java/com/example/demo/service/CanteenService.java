package com.example.demo.service;

import com.example.demo.entity.Canteen;
import com.example.demo.repository.CanteenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CanteenService {

    @Autowired
    private CanteenRepository canteenRepository;

    public List<Canteen> getAllCanteens() {
        return canteenRepository.findAll();
    }
}