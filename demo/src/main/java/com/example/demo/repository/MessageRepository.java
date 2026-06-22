package com.example.demo.repository;

import com.example.demo.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    // 原有方法保持不变...
    List<Message> findByFromUserAndToUserOrFromUserAndToUserOrderByCreateTimeAsc(
            String from1, String to1, String from2, String to2);

    @Query("SELECT m FROM Message m WHERE m.id IN " +
            "(SELECT MAX(m2.id) FROM Message m2 WHERE m2.fromUser = :userId OR m2.toUser = :userId GROUP BY " +
            "CASE WHEN m2.fromUser = :userId THEN m2.toUser ELSE m2.fromUser END) " +
            "ORDER BY m.createTime DESC")
    List<Message> findLatestMessagesForUser(@Param("userId") String userId);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.fromUser = :fromUser AND m.toUser = :toUser")
    void markMessagesAsRead(@Param("fromUser") String fromUser, @Param("toUser") String toUser);

    @Modifying
    @Transactional
    @Query("DELETE FROM Message m WHERE (m.fromUser = :from1 AND m.toUser = :to1) OR (m.fromUser = :from2 AND m.toUser = :to2)")
    void deleteByFromUserAndToUserOrFromUserAndToUser(String from1, String to1, String from2, String to2);

    @Modifying
    @Transactional
    @Query("DELETE FROM Message m WHERE m.fromUser = :userId OR m.toUser = :userId")
    void deleteByFromUserOrToUser(String userId);

    int countByToUserAndIsReadFalse(String toUser);

    // 新增：批量查询未读消息数（按发送者分组）
    @Query("SELECT m.fromUser, COUNT(m) FROM Message m WHERE m.toUser = :userId AND m.isRead = false GROUP BY m.fromUser")
    List<Object[]> countUnreadByFromUser(@Param("userId") String userId);
}