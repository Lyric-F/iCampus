package com.example.demo.controller;

import com.example.demo.entity.Post;
import com.example.demo.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 切换收藏状态（收藏/取消）
     */
    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleFavorite(@RequestParam String studentId,
                                                              @RequestParam Long postId) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean favorited = favoriteService.toggleFavorite(studentId, postId);
            response.put("success", true);
            response.put("favorited", favorited);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 检查是否已收藏
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(@RequestParam String studentId,
                                                              @RequestParam Long postId) {
        boolean favorited = favoriteService.isFavorited(studentId, postId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("favorited", favorited);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前用户的收藏列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<Post>> getFavorites(@RequestParam String studentId,
                                                   @RequestParam(required = false) String currentStudentId) {
        List<Post> posts = favoriteService.getUserFavorites(studentId, currentStudentId);
        return ResponseEntity.ok(posts);
    }

    /**
     * 获取收藏数量
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getFavoriteCount(@RequestParam String studentId) {
        int count = favoriteService.getFavoriteCount(studentId);
        Map<String, Integer> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
}