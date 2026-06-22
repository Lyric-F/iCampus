package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "follow")
public class Follow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String followerId;  // 关注者

    @Column(nullable = false)
    private String followeeId;  // 被关注者

    private LocalDateTime createTime;

    public Follow() {}

    public Follow(String followerId, String followeeId, LocalDateTime createTime) {
        this.followerId = followerId;
        this.followeeId = followeeId;
        this.createTime = createTime;
    }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFollowerId() { return followerId; }
    public void setFollowerId(String followerId) { this.followerId = followerId; }
    public String getFolloweeId() { return followeeId; }
    public void setFolloweeId(String followeeId) { this.followeeId = followeeId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}