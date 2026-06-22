package com.example.demo.repository;

import com.example.demo.entity.LikeDislike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LikeDislikeRepository extends JpaRepository<LikeDislike, Long> {
    void deleteByStudentId(String studentId);
    void deleteByPostId(Long postId);
    Optional<LikeDislike> findByStudentIdAndPostId(String studentId, Long postId);

    // 新增：批量查询指定用户对多个帖子的点赞/踩状态
    List<LikeDislike> findByStudentIdAndPostIdIn(String studentId, List<Long> postIds);
}