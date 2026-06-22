package com.example.demo.service;

import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.entity.Favorite;
import com.example.demo.entity.Order;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private LikeDislikeRepository likeDislikeRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;   // 新增注入

    // ========== 个人主页相关方法 ==========
    public User getUserInfo(String studentId) {
        return userRepository.findById(studentId).orElse(null);
    }

    public void updateAvatar(String studentId, String avatarUrl) {
        User user = userRepository.findById(studentId).orElse(null);
        if (user != null) {
            user.setAvatar(avatarUrl);
            userRepository.save(user);
        }
    }

    public void updateProfile(String studentId, String name, String bio, String avatar, String nickname) {
        User user = userRepository.findById(studentId).orElse(null);
        if (user != null) {
            if (name != null) user.setName(name);
            if (bio != null) user.setBio(bio);
            if (avatar != null) user.setAvatar(avatar);
            if (nickname != null && !nickname.trim().isEmpty()) user.setNickname(nickname);
            userRepository.save(user);
        }
    }

    public Map<String, Integer> getUserStats(String studentId) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("postCount", postRepository.countByStudentId(studentId));
        stats.put("favoriteCount", favoriteRepository.countByStudentId(studentId));
        // 统计用户发布或接单的订单总数
        stats.put("orderCount", orderService.countMyOrders(studentId));
        return stats;
    }

    public List<Post> getUserPosts(String studentId) {
        return postRepository.findByStudentIdOrderByCreateTimeDesc(studentId);
    }

    public List<Post> getUserFavorites(String studentId) {
        List<Favorite> favorites = favoriteRepository.findByStudentId(studentId);
        List<Long> postIds = favorites.stream()
                .map(Favorite::getPostId)
                .collect(Collectors.toList());
        return postRepository.findAllById(postIds);
    }

    public List<Order> getUserOrders(String studentId) {
        return orderRepository.findByStudentIdOrderByCreateTimeDesc(studentId);
    }

    // ========== 原有用户管理方法 ==========
    public boolean register(String studentId, String password) {
        if (userRepository.findById(studentId).isPresent()) {
            return false;
        }
        User user = new User(studentId, password);
        user.setNickname(studentId);
        userRepository.save(user);
        return true;
    }

    public boolean login(String studentId, String password) {
        Optional<User> userOpt = userRepository.findById(studentId);
        return userOpt.isPresent() && userOpt.get().getPassword().equals(password);
    }

    @Transactional
    public boolean deleteUser(String studentId) {
        List<Post> posts = postRepository.findByStudentId(studentId);
        for (Post post : posts) {
            commentRepository.deleteByPostId(post.getId());
            likeDislikeRepository.deleteByPostId(post.getId());
            notificationRepository.deleteByPostId(post.getId());
        }
        postRepository.deleteByStudentId(studentId);
        commentRepository.deleteByStudentId(studentId);
        likeDislikeRepository.deleteByStudentId(studentId);
        notificationRepository.deleteByTargetStudentId(studentId);
        userRepository.deleteById(studentId);
        return true;
    }
}