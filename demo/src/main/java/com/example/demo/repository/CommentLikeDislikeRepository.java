package com.example.demo.repository;

import com.example.demo.entity.CommentLikeDislike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CommentLikeDislikeRepository extends JpaRepository<CommentLikeDislike, Long> {
    Optional<CommentLikeDislike> findByStudentIdAndCommentId(String studentId, Long commentId);
    void deleteByCommentId(Long commentId);
    int countByCommentIdAndType(Long commentId, String type);
}