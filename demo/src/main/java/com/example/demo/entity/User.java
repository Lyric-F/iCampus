package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
public class User {
    @Id
    private String studentId;
    private String password;
    private String name;          // 昵称
    private String avatar;        // 头像URL
    private String bio;           // 个性签名
    private LocalDateTime createTime;
    @Column(length = 50)
    private String nickname;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Column(name = "role", length = 20)
    private String role = "USER";   // 默认普通用户

    // getter/setter
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    // 无参构造方法
    public User() {}

    // 带参构造方法（用于快速创建用户）
    public User(String studentId, String password) {
        this.studentId = studentId;
        this.password = password;
        this.createTime = LocalDateTime.now();
    }

    // Getter 和 Setter

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    }
