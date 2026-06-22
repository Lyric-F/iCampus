package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_student_id", nullable = false)
    private String targetStudentId;   // 接收通知的用户

    @Column(name = "source_student_id", nullable = false)
    private String sourceStudentId;   // 触发通知的用户

    @Column(name = "post_id")
    private Long postId;              // 关联的帖子ID

    @Column(nullable = false)
    private String type;              // LIKE / COMMENT

    private String message;           // 通知内容描述

    @Column(name = "is_read")
    private Boolean isRead = false;   // 是否已读

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    public Notification() {}

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTargetStudentId() { return targetStudentId; }
    public void setTargetStudentId(String targetStudentId) { this.targetStudentId = targetStudentId; }

    public String getSourceStudentId() { return sourceStudentId; }
    public void setSourceStudentId(String sourceStudentId) { this.sourceStudentId = sourceStudentId; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}