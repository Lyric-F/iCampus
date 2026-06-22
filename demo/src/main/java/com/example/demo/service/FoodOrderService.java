package com.example.demo.service;

import com.example.demo.entity.FoodOrder;
import com.example.demo.entity.FoodOrderItem;
import com.example.demo.repository.FoodOrderRepository;
import com.example.demo.repository.FoodOrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class FoodOrderService {

    @Autowired
    private FoodOrderRepository foodOrderRepository;

    @Autowired
    private FoodOrderItemRepository foodOrderItemRepository;

    @Transactional
    public FoodOrder createOrder(String studentId, Long canteenId, List<Map<String, Object>> items,
                                 BigDecimal totalAmount, Long addressId, String deliveryTime, String paymentMethod) {
        FoodOrder order = new FoodOrder();
        order.setStudentId(studentId);
        order.setCanteenId(canteenId);
        order.setTotalAmount(totalAmount);
        order.setStatus("UNPAID");
        order.setDeliveryTime(deliveryTime);
        order.setPaymentMethod(paymentMethod);
        order.setCreateTime(LocalDateTime.now());
        order = foodOrderRepository.save(order);

        for (Map<String, Object> item : items) {
            Long dishId = ((Number) item.get("dishId")).longValue();
            Integer quantity = (Integer) item.get("quantity");
            BigDecimal price = new BigDecimal(item.get("price").toString());

            FoodOrderItem orderItem = new FoodOrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setDishId(dishId);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(price);
            foodOrderItemRepository.save(orderItem);
        }
        return order;
    }

    public List<FoodOrder> getOrdersByStudent(String studentId) {
        return foodOrderRepository.findByStudentIdOrderByCreateTimeDesc(studentId);
    }

}