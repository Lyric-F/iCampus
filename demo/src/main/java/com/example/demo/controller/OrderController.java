package com.example.demo.controller;

import com.example.demo.entity.Order;
import com.example.demo.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建订单（跑腿或食堂）
     */
    @PostMapping("/create")
    public Map<String, Object> createOrder(@RequestBody Order order) {
        Map<String, Object> result = new HashMap<>();
        try {
            order.setCreateTime(LocalDateTime.now());
            // 如果是跑腿订单，初始状态为 PENDING（待接单）
            if ("RUN".equals(order.getOrderType())) {
                order.setStatus("PENDING");
            } else {
                order.setStatus("PAID"); // 食堂订单直接支付
            }
            Order saved = orderService.save(order);
            result.put("success", true);
            result.put("orderId", saved.getId());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取用户订单列表
     */
    @GetMapping("/list")
    public List<Order> getUserOrders(@RequestParam String studentId) {
        return orderService.getOrdersByStudent(studentId);
    }

    /**
     * 删除订单
     */
    @PostMapping("/delete")
    public Map<String, Object> deleteOrder(@RequestParam Long orderId, @RequestParam String studentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Order order = orderService.findById(orderId);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }
            if (!order.getStudentId().equals(studentId)) {
                result.put("success", false);
                result.put("message", "无权操作");
                return result;
            }
            orderService.deleteById(orderId);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取待接单的跑腿任务列表（状态为 PENDING）
     */
    @GetMapping("/run/tasks")
    public List<Order> getPendingRunTasks() {
        return orderService.getPendingRunOrders();
    }

    /**
     * 接单：更新订单状态为 ACCEPTED，并记录接单人信息
     */
    @PostMapping("/run/accept")
    public Map<String, Object> acceptRunTask(@RequestParam Long orderId, @RequestParam String runnerId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Order order = orderService.findById(orderId);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }
            if (!"PENDING".equals(order.getStatus())) {
                result.put("success", false);
                result.put("message", "该任务已被接走");
                return result;
            }

            // 更新订单状态和接单人
            order.setStatus("ACCEPTED");
            order.setAcceptorId(runnerId);

            // 可选：在 details 中记录接单信息（不是必须，但可保留）
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> details = mapper.readValue(order.getDetails(), Map.class);
                details.put("runnerId", runnerId);
                details.put("acceptTime", LocalDateTime.now().toString());
                order.setDetails(mapper.writeValueAsString(details));
            } catch (Exception e) {
                // 如果解析失败，忽略详情更新，不影响主流程
                System.err.println("解析 details 失败: " + e.getMessage());
            }

            orderService.save(order);  // 一次保存
            result.put("success", true);
            System.out.println("接单成功，订单 ID: " + orderId + ", 接单人: " + runnerId); // 控制台输出
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
    // 将原来的 getMyOrders 方法改为：
    @GetMapping("/my")
    public List<Order> getMyOrders(@RequestParam String studentId) {
        return orderService.getMyOrders(studentId);
    }
    @PostMapping("/complete")
    public Map<String, Object> completeOrder(@RequestParam Long orderId, @RequestParam String studentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Order order = orderService.findById(orderId);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }
            if (!order.getStudentId().equals(studentId)) {
                result.put("success", false);
                result.put("message", "无权操作");
                return result;
            }
            if (!"ACCEPTED".equals(order.getStatus())) {
                result.put("success", false);
                result.put("message", "只能将进行中的订单标记为完成");
                return result;
            }
            order.setStatus("COMPLETED");
            orderService.save(order);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}