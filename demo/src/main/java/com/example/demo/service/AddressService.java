package com.example.demo.service;

import com.example.demo.entity.Address;
import com.example.demo.repository.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    public List<Address> getAddresses(String studentId) {
        return addressRepository.findByStudentIdOrderByCreateTimeDesc(studentId);
    }

    @Transactional
    public Address addAddress(String studentId, String name, String phone, String detail) {
        Address address = new Address();
        address.setStudentId(studentId);
        address.setName(name);
        address.setPhone(phone);
        address.setDetail(detail);
        address.setCreateTime(LocalDateTime.now());
        if (addressRepository.findByStudentIdOrderByCreateTimeDesc(studentId).isEmpty()) {
            address.setIsDefault(true);
        }
        return addressRepository.save(address);
    }

    @Transactional
    public void setDefaultAddress(Long addressId, String studentId) {
        List<Address> addresses = addressRepository.findByStudentIdOrderByCreateTimeDesc(studentId);
        for (Address addr : addresses) {
            addr.setIsDefault(false);
            addressRepository.save(addr);
        }
        Address defaultAddr = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("地址不存在"));
        if (!defaultAddr.getStudentId().equals(studentId)) {
            throw new RuntimeException("无权操作");
        }
        defaultAddr.setIsDefault(true);
        addressRepository.save(defaultAddr);
    }
}