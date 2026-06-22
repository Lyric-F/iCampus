package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "like_dislike")
public class LikeDislike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(nullable = false)
    private String type;   // 'LIKE' or 'DISLIKE'

    public LikeDislike() {}

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}