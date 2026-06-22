// OrderRepository.java
package com.example.demo.repository;

import com.example.demo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    int countByStudentId(String studentId);
    List<Order> findByStudentIdOrderByCreateTimeDesc(String studentId);

    // 新增：根据订单类型和状态查询
    List<Order> findByOrderTypeAndStatus(String orderType, String status);
    // 在 OrderRepository.java 中添加
    List<Order> findByStudentIdOrAcceptorIdOrderByCreateTimeDesc(String studentId, String acceptorId);
    // OrderRepository.java
// 统计用户发布或接单的订单总数
    int countByStudentIdOrAcceptorId(String studentId, String acceptorId);
}