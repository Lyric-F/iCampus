package com.example.demo.service;

import com.example.demo.entity.Follow;
import com.example.demo.repository.FollowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FollowService {

    @Autowired
    private FollowRepository followRepository;

    @Transactional
    public boolean follow(String followerId, String followeeId) {
        if (followerId.equals(followeeId)) return false;
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            return false;
        }
        Follow follow = new Follow(followerId, followeeId, LocalDateTime.now());
        followRepository.save(follow);
        return true;
    }

    @Transactional
    public boolean unfollow(String followerId, String followeeId) {
        if (!followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            return false;
        }
        followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
        return true;
    }

    public List<String> getFollowing(String userId) {
        return followRepository.findByFollowerId(userId).stream()
                .map(Follow::getFolloweeId)
                .collect(Collectors.toList());
    }

    public List<String> getFollowers(String userId) {
        return followRepository.findByFolloweeId(userId).stream()
                .map(Follow::getFollowerId)
                .collect(Collectors.toList());
    }

    public List<String> getMutual(String userId) {
        List<String> following = getFollowing(userId);
        List<String> followers = getFollowers(userId);
        return following.stream().filter(followers::contains).collect(Collectors.toList());
    }

    public boolean isFollowing(String followerId, String followeeId) {
        return followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }
}