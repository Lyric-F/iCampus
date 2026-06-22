package com.example.demo.controller;

import com.example.demo.entity.Address;
import com.example.demo.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/address")
@CrossOrigin(origins = "*")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @GetMapping("/list")
    public List<Address> getAddresses(@RequestParam String studentId) {
        return addressService.getAddresses(studentId);
    }

    @PostMapping("/add")
    public Map<String, Object> addAddress(@RequestParam String studentId,
                                          @RequestParam String name,
                                          @RequestParam String phone,
                                          @RequestParam String detail) {
        Map<String, Object> result = new HashMap<>();
        try {
            Address address = addressService.addAddress(studentId, name, phone, detail);
            result.put("success", true);
            result.put("address", address);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/set-default")
    public Map<String, Object> setDefault(@RequestParam Long addressId,
                                          @RequestParam String studentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            addressService.setDefaultAddress(addressId, studentId);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}