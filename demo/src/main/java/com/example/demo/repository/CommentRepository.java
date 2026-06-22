package com.example.demo.repository;

import com.example.demo.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreateTimeDesc(Long postId);
    List<Comment> findByPostIdAndParentIdIsNullOrderByCreateTimeDesc(Long postId);
    List<Comment> findByParentIdOrderByCreateTimeAsc(Long parentId);
    void deleteByStudentId(String studentId);
    void deleteByPostId(Long postId);
}