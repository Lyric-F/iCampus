package com.example.demo.service;

import com.example.demo.entity.Favorite;
import com.example.demo.entity.Post;
import com.example.demo.repository.FavoriteRepository;
import com.example.demo.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostService postService;

    @Transactional
    public boolean toggleFavorite(String studentId, Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("帖子不存在");
        }
        boolean exists = favoriteRepository.existsByStudentIdAndPostId(studentId, postId);
        if (exists) {
            favoriteRepository.deleteByStudentIdAndPostId(studentId, postId);
            return false; // 取消收藏
        } else {
            Favorite favorite = new Favorite();
            favorite.setStudentId(studentId);
            favorite.setPostId(postId);
            favorite.setCreateTime(LocalDateTime.now());
            favoriteRepository.save(favorite);
            return true; // 收藏成功
        }
    }

    public boolean isFavorited(String studentId, Long postId) {
        return favoriteRepository.existsByStudentIdAndPostId(studentId, postId);
    }

    public List<Post> getUserFavorites(String studentId, String currentStudentId) {
        List<Favorite> favorites = favoriteRepository.findByStudentId(studentId);
        if (favorites.isEmpty()) {
            return List.of();
        }
        List<Long> postIds = favorites.stream()
                .map(Favorite::getPostId)
                .collect(Collectors.toList());
        List<Post> posts = postRepository.findAllById(postIds);
        // 按收藏时间倒序（最新收藏在前）
        posts.sort((a, b) -> b.getId().compareTo(a.getId()));
        // 填充昵称、头像、点赞状态
        postService.fillNicknames(posts);
        postService.enrichPosts(posts, currentStudentId);
        return posts;
    }

    public int getFavoriteCount(String studentId) {
        return favoriteRepository.countByStudentId(studentId);
    }
}