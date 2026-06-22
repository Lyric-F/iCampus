// FavoriteRepository.java
package com.example.demo.repository;

import com.example.demo.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByStudentId(String studentId);
    boolean existsByStudentIdAndPostId(String studentId, Long postId);
    void deleteByStudentIdAndPostId(String studentId, Long postId);
    int countByStudentId(String studentId);
    // 在 FavoriteRepository 接口中添加
    void deleteByPostId(Long postId);
}

