package com.example.demo.service;

import com.example.demo.entity.Order;
import com.example.demo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public Order save(Order order) {
        return orderRepository.save(order);
    }

    public Order findById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    public List<Order> getOrdersByStudent(String studentId) {
        return orderRepository.findByStudentIdOrderByCreateTimeDesc(studentId);
    }

    public void deleteById(Long id) {
        orderRepository.deleteById(id);
    }

    // 新增：获取待接单的跑腿任务
    public List<Order> getPendingRunOrders() {
        return orderRepository.findByOrderTypeAndStatus("RUN", "PENDING");
    }
    // 在 OrderService.java 中添加
    public List<Order> getMyOrders(String studentId) {
        return orderRepository.findByStudentIdOrAcceptorIdOrderByCreateTimeDesc(studentId, studentId);
    }
    // OrderService.java
    public int countMyOrders(String studentId) {
        return orderRepository.countByStudentIdOrAcceptorId(studentId, studentId);
    }
}