package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String content;

    private LocalDateTime createTime;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false, length = 20)
    private String studentId;

    // 新增字段（持久化）
    private Long parentId;
    private String replyToStudentId;
    private int likeCount = 0;
    private int dislikeCount = 0;
    private int replyCount = 0;

    // 临时字段
    @Transient
    private String userAvatar;
    @Transient
    private String userNickname;
    @Transient
    private Boolean isPoster;
    @Transient
    private String replyToNickname;
    @Transient
    private List<Comment> replies;
    @Transient
    private boolean userLiked;
    @Transient
    private boolean userDisliked;

    // getter/setter（请确保每个字段都有对应的方法）
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getReplyToStudentId() { return replyToStudentId; }
    public void setReplyToStudentId(String replyToStudentId) { this.replyToStudentId = replyToStudentId; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public int getDislikeCount() { return dislikeCount; }
    public void setDislikeCount(int dislikeCount) { this.dislikeCount = dislikeCount; }
    public int getReplyCount() { return replyCount; }
    public void setReplyCount(int replyCount) { this.replyCount = replyCount; }
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public String getUserNickname() { return userNickname; }
    public void setUserNickname(String userNickname) { this.userNickname = userNickname; }
    public Boolean getIsPoster() { return isPoster; }
    public void setIsPoster(Boolean isPoster) { this.isPoster = isPoster; }
    public String getReplyToNickname() { return replyToNickname; }
    public void setReplyToNickname(String replyToNickname) { this.replyToNickname = replyToNickname; }
    public List<Comment> getReplies() { return replies; }
    public void setReplies(List<Comment> replies) { this.replies = replies; }
    public boolean isUserLiked() { return userLiked; }
    public void setUserLiked(boolean userLiked) { this.userLiked = userLiked; }
    public boolean isUserDisliked() { return userDisliked; }
    public void setUserDisliked(boolean userDisliked) { this.userDisliked = userDisliked; }
}