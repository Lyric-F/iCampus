package com.example.demo.repository;

import com.example.demo.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByTargetStudentIdOrderByCreateTimeDesc(String studentId);
    void deleteByTargetStudentId(String studentId);
    void deleteByPostId(Long postId);

    // 新增：删除单条通知（需校验所属用户）
    void deleteByIdAndTargetStudentId(Long id, String studentId);

    // 新增：统计未读数量
    int countByTargetStudentIdAndIsReadFalse(String studentId);

    // 新增：将所有未读标记为已读
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.targetStudentId = :studentId AND n.isRead = false")
    int markAllAsReadByStudentId(String studentId);
}