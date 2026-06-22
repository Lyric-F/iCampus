package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private LikeDislikeRepository likeDislikeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentLikeDislikeRepository commentLikeDislikeRepository;

    // ==================== 辅助方法（匿名处理） ====================

    public void fillNicknames(List<Post> posts) {
        if (posts == null || posts.isEmpty()) return;
        List<String> studentIds = posts.stream()
                .map(Post::getStudentId)
                .distinct()
                .collect(Collectors.toList());
        List<User> users = userRepository.findAllById(studentIds);
        Map<String, String> nicknameMap = users.stream()
                .collect(Collectors.toMap(User::getStudentId,
                        user -> user.getNickname() != null ? user.getNickname() : user.getStudentId()));
        for (Post post : posts) {
            if (post.isAnonymous()) {
                // 匿名：生成随机昵称（基于帖子ID或当前时间）
                String anonNick = "匿名用户_" + (post.getId() == null ? System.currentTimeMillis() : post.getId() % 10000);
                post.setNickname(anonNick);
            } else {
                post.setNickname(nicknameMap.getOrDefault(post.getStudentId(), post.getStudentId()));
            }
        }
    }

    public void enrichPosts(List<Post> posts, String studentId) {
        if (posts == null || posts.isEmpty()) return;
        // 填充头像
        List<String> studentIds = posts.stream().map(Post::getStudentId).collect(Collectors.toList());
        List<User> users = userRepository.findAllById(studentIds);
        Map<String, String> avatarMap = users.stream()
                .collect(Collectors.toMap(User::getStudentId, u -> u.getAvatar() != null ? u.getAvatar() : "", (a, b) -> a));
        for (Post post : posts) {
            if (post.isAnonymous()) {
                // 匿名：使用默认头像
                post.setAvatar("/assets/anonymous.png");
            } else {
                post.setAvatar(avatarMap.getOrDefault(post.getStudentId(), ""));
            }
        }
        // 填充点赞/踩状态（使用批量查询）
        if (studentId != null && !studentId.isEmpty()) {
            List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
            List<LikeDislike> likes = likeDislikeRepository.findByStudentIdAndPostIdIn(studentId, postIds);
            Map<Long, String> likeMap = likes.stream()
                    .collect(Collectors.toMap(LikeDislike::getPostId, LikeDislike::getType));
            for (Post post : posts) {
                String type = likeMap.get(post.getId());
                if ("LIKE".equals(type)) {
                    post.setUserLiked(true);
                } else if ("DISLIKE".equals(type)) {
                    post.setUserDisliked(true);
                }
            }
        }
    }

    @Transactional
    public void incrementViewCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
    }

    // ==================== 批量获取帖子（保持顺序） ====================
    public List<Post> getPostsByIds(List<Long> ids, String studentId) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Post> posts = postRepository.findAllById(ids);
        enrichPosts(posts, studentId);
        fillNicknames(posts);
        Map<Long, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));
        List<Post> ordered = new ArrayList<>();
        for (Long id : ids) {
            Post p = postMap.get(id);
            if (p != null) ordered.add(p);
        }
        return ordered;
    }

    // ==================== 发布帖子（支持匿名、图片、置顶） ====================
    @Transactional
    public Post publish(String studentId, String type, String title, String content,
                        boolean anonymous, String images, boolean top) {
        Post post = new Post(studentId, type, title, content, LocalDateTime.now());
        post.setAnonymous(anonymous);
        post.setImages(images);
        post.setTop(top);
        return postRepository.save(post);
    }

    // ==================== 原有方法（保持兼容） ====================
    public Post getPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
    }

    @Transactional
    public boolean likePost(String studentId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        Optional<LikeDislike> existing = likeDislikeRepository.findByStudentIdAndPostId(studentId, postId);
        if (existing.isPresent()) {
            LikeDislike ld = existing.get();
            if ("LIKE".equals(ld.getType())) {
                likeDislikeRepository.delete(ld);
                post.setLikeCount(post.getLikeCount() - 1);
                postRepository.save(post);
                return false;
            } else {
                ld.setType("LIKE");
                likeDislikeRepository.save(ld);
                post.setDislikeCount(post.getDislikeCount() - 1);
                post.setLikeCount(post.getLikeCount() + 1);
                postRepository.save(post);
                if (!studentId.equals(post.getStudentId())) {
                    notificationService.createNotification(post.getStudentId(), studentId, postId, "LIKE",
                            "用户 " + studentId + " 赞了你的帖子");
                }
                return true;
            }
        } else {
            LikeDislike ld = new LikeDislike();
            ld.setStudentId(studentId);
            ld.setPostId(postId);
            ld.setType("LIKE");
            likeDislikeRepository.save(ld);
            post.setLikeCount(post.getLikeCount() + 1);
            postRepository.save(post);
            if (!studentId.equals(post.getStudentId())) {
                notificationService.createNotification(post.getStudentId(), studentId, postId, "LIKE",
                        "用户 " + studentId + " 赞了你的帖子");
            }
            return true;
        }
    }

    @Transactional
    public boolean dislikePost(String studentId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        Optional<LikeDislike> existing = likeDislikeRepository.findByStudentIdAndPostId(studentId, postId);
        if (existing.isPresent()) {
            LikeDislike ld = existing.get();
            if ("DISLIKE".equals(ld.getType())) {
                likeDislikeRepository.delete(ld);
                post.setDislikeCount(post.getDislikeCount() - 1);
                postRepository.save(post);
                return false;
            } else {
                ld.setType("DISLIKE");
                likeDislikeRepository.save(ld);
                post.setLikeCount(post.getLikeCount() - 1);
                post.setDislikeCount(post.getDislikeCount() + 1);
                postRepository.save(post);
                return true;
            }
        } else {
            LikeDislike ld = new LikeDislike();
            ld.setStudentId(studentId);
            ld.setPostId(postId);
            ld.setType("DISLIKE");
            likeDislikeRepository.save(ld);
            post.setDislikeCount(post.getDislikeCount() + 1);
            postRepository.save(post);
            return true;
        }
    }

    @Transactional
    public Comment commentPost(String studentId, Long postId, String content, Long parentId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));

        Comment comment = new Comment();
        comment.setStudentId(studentId);
        comment.setPostId(postId);
        comment.setContent(content);
        comment.setCreateTime(LocalDateTime.now());

        if (parentId != null) {
            Comment parentComment = commentRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("父评论不存在"));
            comment.setParentId(parentId);
            comment.setReplyToStudentId(parentComment.getStudentId());
            parentComment.setReplyCount(parentComment.getReplyCount() + 1);
            commentRepository.save(parentComment);
        }

        Comment saved = commentRepository.save(comment);
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        if (!studentId.equals(post.getStudentId())) {
            String shortContent = content.length() > 30 ? content.substring(0, 30) + "..." : content;
            notificationService.createNotification(post.getStudentId(), studentId, postId, "COMMENT",
                    "用户 " + studentId + " 评论了你的帖子: " + shortContent);
        }
        if (parentId != null && !studentId.equals(comment.getReplyToStudentId())) {
            String shortContent = content.length() > 30 ? content.substring(0, 30) + "..." : content;
            notificationService.createNotification(comment.getReplyToStudentId(), studentId, postId, "COMMENT_REPLY",
                    "用户 " + studentId + " 回复了你的评论: " + shortContent);
        }
        return saved;
    }

    // 获取帖子详情（自动增加浏览量，并处理匿名/头像/图片）
    public Post getPostDetail(Long postId, String studentId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        incrementViewCount(postId);
        List<Post> posts = Arrays.asList(post);
        enrichPosts(posts, studentId);
        fillNicknames(posts);
        return post;
    }

    public List<Post> getUserPosts(String studentId) {
        List<Post> posts = postRepository.findByStudentIdOrderByCreateTimeDesc(studentId);
        fillNicknames(posts);
        return posts;
    }

    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreateTimeDesc(postId);
    }

    public List<Comment> getCommentsWithReplies(Long postId, String currentStudentId) {
        Post post = postRepository.findById(postId).orElse(null);
        String posterId = (post != null) ? post.getStudentId() : null;
        List<Comment> topComments = commentRepository.findByPostIdAndParentIdIsNullOrderByCreateTimeDesc(postId);
        for (Comment comment : topComments) {
            enrichComment(comment, posterId, currentStudentId);
            loadRepliesRecursively(comment, posterId, currentStudentId);
        }
        return topComments;
    }

    private void loadRepliesRecursively(Comment comment, String posterId, String currentStudentId) {
        List<Comment> replies = commentRepository.findByParentIdOrderByCreateTimeAsc(comment.getId());
        for (Comment reply : replies) {
            enrichComment(reply, posterId, currentStudentId);
            loadRepliesRecursively(reply, posterId, currentStudentId);
        }
        comment.setReplies(replies);
    }

    private void enrichComment(Comment comment, String posterId, String currentStudentId) {
        User user = userRepository.findById(comment.getStudentId()).orElse(null);
        if (user != null) {
            comment.setUserAvatar(user.getAvatar());
            comment.setUserNickname(user.getNickname() != null ? user.getNickname() : user.getStudentId());
        } else {
            comment.setUserNickname(comment.getStudentId());
        }
        if (posterId != null && posterId.equals(comment.getStudentId())) {
            comment.setIsPoster(true);
        }
        if (comment.getReplyToStudentId() != null) {
            User replyToUser = userRepository.findById(comment.getReplyToStudentId()).orElse(null);
            if (replyToUser != null) {
                comment.setReplyToNickname(replyToUser.getNickname() != null ? replyToUser.getNickname() : replyToUser.getStudentId());
            } else {
                comment.setReplyToNickname(comment.getReplyToStudentId());
            }
        }
        if (currentStudentId != null && !currentStudentId.isEmpty()) {
            Optional<CommentLikeDislike> ld = commentLikeDislikeRepository
                    .findByStudentIdAndCommentId(currentStudentId, comment.getId());
            if (ld.isPresent()) {
                if ("LIKE".equals(ld.get().getType())) {
                    comment.setUserLiked(true);
                } else if ("DISLIKE".equals(ld.get().getType())) {
                    comment.setUserDisliked(true);
                }
            }
        }
    }

    public Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
    }

    @Transactional
    public boolean likeComment(String studentId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        Optional<CommentLikeDislike> existing = commentLikeDislikeRepository
                .findByStudentIdAndCommentId(studentId, commentId);
        if (existing.isPresent()) {
            CommentLikeDislike ld = existing.get();
            if ("LIKE".equals(ld.getType())) {
                commentLikeDislikeRepository.delete(ld);
                comment.setLikeCount(comment.getLikeCount() - 1);
                commentRepository.save(comment);
                return false;
            } else {
                ld.setType("LIKE");
                commentLikeDislikeRepository.save(ld);
                comment.setDislikeCount(comment.getDislikeCount() - 1);
                comment.setLikeCount(comment.getLikeCount() + 1);
                commentRepository.save(comment);
                if (!studentId.equals(comment.getStudentId())) {
                    notificationService.createNotification(comment.getStudentId(), studentId,
                            comment.getPostId(), "COMMENT_LIKE",
                            "用户 " + studentId + " 赞了你的评论");
                }
                return true;
            }
        } else {
            CommentLikeDislike ld = new CommentLikeDislike();
            ld.setStudentId(studentId);
            ld.setCommentId(commentId);
            ld.setType("LIKE");
            commentLikeDislikeRepository.save(ld);
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentRepository.save(comment);
            if (!studentId.equals(comment.getStudentId())) {
                notificationService.createNotification(comment.getStudentId(), studentId,
                        comment.getPostId(), "COMMENT_LIKE",
                        "用户 " + studentId + " 赞了你的评论");
            }
            return true;
        }
    }

    @Transactional
    public boolean dislikeComment(String studentId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        Optional<CommentLikeDislike> existing = commentLikeDislikeRepository
                .findByStudentIdAndCommentId(studentId, commentId);
        if (existing.isPresent()) {
            CommentLikeDislike ld = existing.get();
            if ("DISLIKE".equals(ld.getType())) {
                commentLikeDislikeRepository.delete(ld);
                comment.setDislikeCount(comment.getDislikeCount() - 1);
                commentRepository.save(comment);
                return false;
            } else {
                ld.setType("DISLIKE");
                commentLikeDislikeRepository.save(ld);
                comment.setLikeCount(comment.getLikeCount() - 1);
                comment.setDislikeCount(comment.getDislikeCount() + 1);
                commentRepository.save(comment);
                return true;
            }
        } else {
            CommentLikeDislike ld = new CommentLikeDislike();
            ld.setStudentId(studentId);
            ld.setCommentId(commentId);
            ld.setType("DISLIKE");
            commentLikeDislikeRepository.save(ld);
            comment.setDislikeCount(comment.getDislikeCount() + 1);
            commentRepository.save(comment);
            return true;
        }
    }

    // 带置顶排序的通用查询（示例：getPosts 和 getSquarePosts 已改造）
    public List<Post> getPosts(String category, String keyword, String studentId) {
        List<Post> posts;
        if (category != null && !category.isEmpty() && !category.equals("最新")) {
            if (keyword != null && !keyword.isEmpty()) {
                posts = postRepository.findByTypeAndKeyword(category, keyword);
            } else {
                posts = postRepository.findByTypeOrderByCreateTimeDesc(category);
            }
        } else {
            if (keyword != null && !keyword.isEmpty()) {
                posts = postRepository.searchByKeyword(keyword);
            } else {
                posts = postRepository.findAllByOrderByCreateTimeDesc();
            }
        }
        // 置顶优先排序
        posts.sort((a, b) -> {
            if (a.isTop() != b.isTop()) return Boolean.compare(b.isTop(), a.isTop());
            return b.getCreateTime().compareTo(a.getCreateTime());
        });
        for (Post post : posts) {
            Optional<User> userOpt = userRepository.findById(post.getStudentId());
            if (userOpt.isPresent() && userOpt.get().getAvatar() != null) {
                post.setAvatar(userOpt.get().getAvatar());
            }
            if (studentId != null && !studentId.isEmpty()) {
                Optional<LikeDislike> ld = likeDislikeRepository.findByStudentIdAndPostId(studentId, post.getId());
                if (ld.isPresent()) {
                    if ("LIKE".equals(ld.get().getType())) {
                        post.setUserLiked(true);
                    } else if ("DISLIKE".equals(ld.get().getType())) {
                        post.setUserDisliked(true);
                    }
                }
            }
        }
        fillNicknames(posts);
        return posts;
    }

    public List<Post> getHotPosts(int limit) {
        List<Post> posts = postRepository.findTopByLikeCount(PageRequest.of(0, limit));
        fillNicknames(posts);
        return posts;
    }

    public List<Post> searchPosts(String keyword) {
        List<Post> posts = postRepository.findByCategoryAndKeyword(null, keyword);
        fillNicknames(posts);
        return posts;
    }

    @Transactional
    public void deletePost(String studentId, Long postId, boolean isAdmin) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        if (!isAdmin && !post.getStudentId().equals(studentId)) {
            throw new RuntimeException("无权删除此帖子");
        }
        deleteCommentsByPostId(postId);
        likeDislikeRepository.deleteByPostId(postId);
        notificationService.deleteByPostId(postId);
        favoriteRepository.deleteByPostId(postId);
        postRepository.deleteById(postId);
    }

    private void deleteCommentsByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreateTimeDesc(postId);
        for (Comment comment : comments) {
            commentLikeDislikeRepository.deleteByCommentId(comment.getId());
        }
        commentRepository.deleteByPostId(postId);
    }

    public List<Post> getSquarePosts(String sort, String category, String keyword, String studentId) {
        List<String> allowedCategories = Arrays.asList("二手交易", "失物招领", "助农", "互助", "树洞", "表白墙", "吐槽");
        List<Post> posts;
        if (category != null && allowedCategories.contains(category)) {
            if (keyword != null && !keyword.isEmpty()) {
                posts = postRepository.findByTypeAndKeyword(category, keyword);
            } else {
                posts = postRepository.findByTypeOrderByCreateTimeDesc(category);
            }
        } else {
            if (keyword != null && !keyword.isEmpty()) {
                posts = postRepository.findByTypesAndKeyword(allowedCategories, keyword);
            } else {
                posts = postRepository.findByTypesOrderByCreateTimeDesc(allowedCategories);
            }
        }

        // 置顶优先，再按热度或时间排序
        if ("hot".equals(sort)) {
            posts.sort((a, b) -> {
                if (a.isTop() != b.isTop()) return Boolean.compare(b.isTop(), a.isTop());
                return Integer.compare(b.getLikeCount(), a.getLikeCount());
            });
        } else if ("latest".equals(sort) || sort == null) {
            posts.sort((a, b) -> {
                if (a.isTop() != b.isTop()) return Boolean.compare(b.isTop(), a.isTop());
                return b.getCreateTime().compareTo(a.getCreateTime());
            });
        }

        for (Post post : posts) {
            Optional<User> userOpt = userRepository.findById(post.getStudentId());
            userOpt.ifPresent(user -> post.setAvatar(user.getAvatar()));
            if (studentId != null && !studentId.isEmpty()) {
                Optional<LikeDislike> ld = likeDislikeRepository.findByStudentIdAndPostId(studentId, post.getId());
                if (ld.isPresent()) {
                    if ("LIKE".equals(ld.get().getType())) {
                        post.setUserLiked(true);
                    } else if ("DISLIKE".equals(ld.get().getType())) {
                        post.setUserDisliked(true);
                    }
                }
            }
        }
        fillNicknames(posts);
        return posts;
    }
}