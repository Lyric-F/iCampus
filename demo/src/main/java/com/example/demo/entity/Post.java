package com.example.demo.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String studentId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    private LocalDateTime createTime;

    // 统计字段
    private int viewCount = 0;
    private int likeCount = 0;
    private int dislikeCount = 0;
    private int commentCount = 0;

    // 新增字段
    private boolean isAnonymous = false;       // 是否匿名
    @Column(columnDefinition = "TEXT")
    private String images;                     // 存储图片URL的JSON数组字符串
    private boolean top = false;               // 是否置顶

    // 无参构造
    public Post() {}

    // 带参构造
    public Post(String studentId, String type, String title, String content, LocalDateTime createTime) {
        this.studentId = studentId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.createTime = createTime;
    }

    // ========== getter / setter ==========
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getDislikeCount() { return dislikeCount; }
    public void setDislikeCount(int dislikeCount) { this.dislikeCount = dislikeCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public boolean isAnonymous() { return isAnonymous; }
    public void setAnonymous(boolean anonymous) { isAnonymous = anonymous; }

    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }

    public boolean isTop() { return top; }
    public void setTop(boolean top) { this.top = top; }

    // ========== 临时字段（不存数据库） ==========
    @Transient
    private String avatar;
    @Transient
    private String nickname;
    @Transient
    private boolean userLiked;
    @Transient
    private boolean userDisliked;

    // 解析 images JSON 字符串，返回 List<String>（供前端使用）
    @Transient
    public List<String> getImageList() {
        if (images == null || images.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(images, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // 普通 getter/setter 保持原有风格
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public boolean isUserLiked() { return userLiked; }
    public void setUserLiked(boolean userLiked) { this.userLiked = userLiked; }

    public boolean isUserDisliked() { return userDisliked; }
    public void setUserDisliked(boolean userDisliked) { this.userDisliked = userDisliked; }
}