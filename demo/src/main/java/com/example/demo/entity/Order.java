package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String studentId;
    private String orderType;   // 打印/跑腿/干洗
    private BigDecimal amount;
    private String status;      // PAID, UNPAID
    private LocalDateTime createTime;
    // getters & setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    @Column(columnDefinition = "TEXT")
    private String details;

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    // 在 Order.java 中添加
    private String acceptorId;  // 接单人学号

    // 添加 getter/setter
    public String getAcceptorId() { return acceptorId; }
    public void setAcceptorId(String acceptorId) { this.acceptorId = acceptorId; }
}