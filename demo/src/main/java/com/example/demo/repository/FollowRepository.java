package com.example.demo.repository;

import com.example.demo.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    List<Follow> findByFollowerId(String followerId);
    List<Follow> findByFolloweeId(String followeeId);
    Optional<Follow> findByFollowerIdAndFolloweeId(String followerId, String followeeId);
    boolean existsByFollowerIdAndFolloweeId(String followerId, String followeeId);
    void deleteByFollowerIdAndFolloweeId(String followerId, String followeeId);
}