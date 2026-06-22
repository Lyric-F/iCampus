package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.entity.Post;
import com.example.demo.entity.Order;
import com.example.demo.service.UserService;
import com.example.demo.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;   // 只有一个 userService 实例

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.access-url}")
    private String accessUrl;

    // 头像上传接口
    @PostMapping("/upload-avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file,
                                          @RequestParam String studentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. 校验文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                result.put("success", false);
                result.put("message", "只能上传图片文件");
                return ResponseEntity.badRequest().body(result);
            }

            // 2. 保存文件
            String fileName = FileUploadUtil.saveFile(file, uploadDir);
            String avatarUrl = accessUrl + fileName;

            // 3. 更新用户头像
            userService.updateAvatar(studentId, avatarUrl);

            result.put("success", true);
            result.put("avatarUrl", avatarUrl);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "文件上传失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    // 用户注册
    @PostMapping("/register")
    public Map<String, Object> register(@RequestParam String studentId, @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();
        boolean success = userService.register(studentId, password);
        result.put("success", success);
        result.put("message", success ? "注册成功" : "学号已存在");
        return result;
    }

    // 用户登录
    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String studentId, @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();
        boolean success = userService.login(studentId, password);
        result.put("success", success);
        result.put("message", success ? "登录成功" : "学号或密码错误");
        return result;
    }

    // 注销账号
    @PostMapping("/delete")
    public ResponseEntity<?> deleteUser(@RequestParam String studentId) {
        boolean success = userService.deleteUser(studentId);
        if (success) {
            return ResponseEntity.ok().body(Map.of("success", true, "message", "账号已注销"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "注销失败"));
        }
    }

    // 获取用户信息
    @GetMapping("/info")
    public User getUserInfo(@RequestParam String studentId) {
        return userService.getUserInfo(studentId);
    }

    // 更新个人资料
    @PostMapping("/update")
    public Map<String, Object> updateProfile(@RequestParam String studentId,
                                             @RequestParam(required = false) String name,
                                             @RequestParam(required = false) String bio,
                                             @RequestParam(required = false) String avatar,
                                             @RequestParam(required = false) String nickname) { // 新增 nickname
        userService.updateProfile(studentId, name, bio, avatar, nickname);
        return Map.of("success", true);
    }

    // 获取用户统计数据
    @GetMapping("/stats")
    public Map<String, Integer> getUserStats(@RequestParam String studentId) {
        return userService.getUserStats(studentId);
    }

    // 获取用户发布的帖子
    @GetMapping("/posts")
    public List<Post> getUserPosts(@RequestParam String studentId) {
        return userService.getUserPosts(studentId);
    }

    // 获取用户收藏的帖子
    @GetMapping("/favorites")
    public List<Post> getUserFavorites(@RequestParam String studentId) {
        return userService.getUserFavorites(studentId);
    }

    // 获取用户订单
    @GetMapping("/orders")
    public List<Order> getUserOrders(@RequestParam String studentId) {
        return userService.getUserOrders(studentId);
    }
}