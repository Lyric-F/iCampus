package com.example.demo.controller;

import com.example.demo.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/follow")
public class FollowController {

    @Autowired
    private FollowService followService;

    @PostMapping("/follow")
    public ResponseEntity<Map<String, Object>> follow(@RequestParam String followerId, @RequestParam String followeeId) {
        boolean success = followService.follow(followerId, followeeId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "关注成功" : "关注失败");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/unfollow")
    public ResponseEntity<Map<String, Object>> unfollow(@RequestParam String followerId, @RequestParam String followeeId) {
        boolean success = followService.unfollow(followerId, followeeId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "取消关注成功" : "取消关注失败");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/following")
    public ResponseEntity<List<String>> getFollowing(@RequestParam String userId) {
        return ResponseEntity.ok(followService.getFollowing(userId));
    }

    @GetMapping("/followers")
    public ResponseEntity<List<String>> getFollowers(@RequestParam String userId) {
        return ResponseEntity.ok(followService.getFollowers(userId));
    }

    @GetMapping("/mutual")
    public ResponseEntity<List<String>> getMutual(@RequestParam String userId) {
        return ResponseEntity.ok(followService.getMutual(userId));
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkFollow(@RequestParam String followerId, @RequestParam String followeeId) {
        boolean isFollowing = followService.isFollowing(followerId, followeeId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("isFollowing", isFollowing);
        return ResponseEntity.ok(response);
    }
}