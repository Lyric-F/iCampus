package com.example.demo.controller;

import com.example.demo.entity.Comment;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.service.PostService;
import com.example.demo.service.UserService;
import com.example.demo.util.FileUploadUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/post")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private RestTemplate restTemplate;

    // 头像上传配置（保留，可能用于其他功能）
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.access-url}")
    private String accessUrl;

    // 帖子图片上传配置（新增）
    @Value("${post.image.upload-dir}")
    private String postImageUploadDir;

    @Value("${post.image.access-url}")
    private String postImageAccessUrl;

    // 发布动态（支持匿名、图片、置顶）
    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publish(@RequestParam String studentId,
                                                       @RequestParam String type,
                                                       @RequestParam(required = false) String title,
                                                       @RequestParam String content,
                                                       @RequestParam(required = false, defaultValue = "false") boolean anonymous,
                                                       @RequestParam(required = false) String images,
                                                       @RequestParam(required = false, defaultValue = "false") boolean top) {
        Map<String, Object> response = new HashMap<>();
        if (studentId == null || studentId.trim().isEmpty() ||
                type == null || type.trim().isEmpty() ||
                content == null || content.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "学生ID、分类和内容不能为空");
            return ResponseEntity.ok(response);
        }
        // 如果没有提供标题，则从内容中截取前30个字符作为标题
        if (title == null || title.trim().isEmpty()) {
            title = content.length() > 30 ? content.substring(0, 30) : content;
        }
        try {
            User user = userService.getUserInfo(studentId);
            boolean isAdmin = (user != null && "ADMIN".equals(user.getRole()));
            if (("校园公告".equals(type) || "活动报名".equals(type)) && !isAdmin) {
                response.put("success", false);
                response.put("message", "只有管理员可以发布公告或活动");
                return ResponseEntity.ok(response);
            }
            Post post = postService.publish(studentId, type, title, content, anonymous, images, top);
            response.put("success", true);
            response.put("message", "发布成功");
            response.put("postId", post.getId());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "发布失败：" + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // 图片上传接口（多图）—— 使用帖子图片独立目录
    @PostMapping("/upload-images")
    public ResponseEntity<Map<String, Object>> uploadImages(@RequestParam("images") List<MultipartFile> images,
                                                            @RequestParam String studentId) {
        Map<String, Object> response = new HashMap<>();
        List<String> urls = new ArrayList<>();
        // 限制最多上传9张图片
        if (images.size() > 9) {
            response.put("success", false);
            response.put("message", "最多上传9张图片");
            return ResponseEntity.badRequest().body(response);
        }
        try {
            for (MultipartFile file : images) {
                // 校验文件类型
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    response.put("success", false);
                    response.put("message", "只能上传图片文件");
                    return ResponseEntity.badRequest().body(response);
                }
                String fileName = FileUploadUtil.saveFile(file, postImageUploadDir);  // 使用帖子图片目录
                String url = postImageAccessUrl + fileName;                           // 使用帖子图片访问URL
                urls.add(url);
            }
            response.put("success", true);
            response.put("urls", urls);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "上传失败：" + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // ========== 以下所有接口保持不变 ==========
    @GetMapping("/list")
    public ResponseEntity<List<Post>> list(@RequestParam(required = false) String category,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String studentId) {
        List<Post> posts = postService.getPosts(category, keyword, studentId);
        return ResponseEntity.ok(posts);
    }

    @PostMapping("/like")
    public ResponseEntity<?> likePost(@RequestParam String studentId, @RequestParam Long postId) {
        boolean liked = postService.likePost(studentId, postId);
        Post post = postService.getPostById(postId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("liked", liked);
        result.put("likeCount", post.getLikeCount());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/dislike")
    public ResponseEntity<?> dislikePost(@RequestParam String studentId, @RequestParam Long postId) {
        boolean disliked = postService.dislikePost(studentId, postId);
        Post post = postService.getPostById(postId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("disliked", disliked);
        result.put("dislikeCount", post.getDislikeCount());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/comment")
    public ResponseEntity<?> addComment(@RequestParam String studentId,
                                        @RequestParam Long postId,
                                        @RequestParam String content,
                                        @RequestParam(required = false) Long parentId) {
        try {
            Comment comment = postService.commentPost(studentId, postId, content, parentId);
            Post post = postService.getPostById(postId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("comment", comment);
            result.put("commentCount", post.getCommentCount());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/comments")
    public ResponseEntity<?> getComments(@RequestParam Long postId,
                                         @RequestParam(required = false) String studentId) {
        try {
            List<Comment> comments = postService.getCommentsWithReplies(postId, studentId);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/comment/like")
    public ResponseEntity<?> likeComment(@RequestParam String studentId,
                                         @RequestParam Long commentId) {
        try {
            boolean liked = postService.likeComment(studentId, commentId);
            Comment comment = postService.getCommentById(commentId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("liked", liked);
            result.put("likeCount", comment.getLikeCount());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/comment/dislike")
    public ResponseEntity<?> dislikeComment(@RequestParam String studentId,
                                            @RequestParam Long commentId) {
        try {
            boolean disliked = postService.dislikeComment(studentId, commentId);
            Comment comment = postService.getCommentById(commentId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("disliked", disliked);
            result.put("dislikeCount", comment.getDislikeCount());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/hot")
    public List<Post> getHotPosts(@RequestParam(defaultValue = "10") int limit) {
        return postService.getHotPosts(limit);
    }

    @GetMapping("/search")
    public List<Post> searchPosts(@RequestParam String keyword) {
        return postService.searchPosts(keyword);
    }

    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deletePost(@RequestParam String studentId,
                                                          @RequestParam Long postId) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.getUserInfo(studentId);
            boolean isAdmin = user != null && "ADMIN".equals(user.getRole());
            postService.deletePost(studentId, postId, isAdmin);
            response.put("success", true);
            response.put("message", "删除成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/detail")
    public ResponseEntity<?> getPostDetail(@RequestParam Long postId,
                                           @RequestParam(required = false) String studentId) {
        try {
            Post post = postService.getPostDetail(postId, studentId);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/user/posts")
    public ResponseEntity<List<Post>> getUserPosts(@RequestParam String studentId) {
        List<Post> posts = postService.getUserPosts(studentId);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/square/posts")
    public List<Post> getSquarePosts(@RequestParam(required = false) String sort,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String studentId) {
        if ("recommend".equals(sort)) {
            if (studentId == null || studentId.isEmpty()) {
                return Collections.emptyList();
            }
            // 直接使用原始 studentId，不需要加 "user_" 前缀
            String url = "http://127.0.0.1:8000/recommend";
            Map<String, Object> request = Map.of("user_id", studentId, "top_k", 20);
            try {
                ResponseEntity<RecommendResponse[]> response = restTemplate.postForEntity(url, request, RecommendResponse[].class);
                RecommendResponse[] recs = response.getBody();
                if (recs == null || recs.length == 0) {
                    // 推荐为空时降级到热门帖子
                    return getHotPostsFallback(studentId);
                }
                List<Long> postIds = Arrays.stream(recs)
                        .map(RecommendResponse::getPostId)
                        .collect(Collectors.toList());
                List<Post> posts = postService.getPostsByIds(postIds, studentId);
                if (posts.isEmpty()) {
                    return getHotPostsFallback(studentId);
                }
                Map<Long, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));
                List<Post> ordered = postIds.stream()
                        .map(postMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (ordered.isEmpty()) {
                    return getHotPostsFallback(studentId);
                }
                return ordered;
            } catch (Exception e) {
                e.printStackTrace();
                // 异常时降级到热门帖子
                return getHotPostsFallback(studentId);
            }
        }
        return postService.getSquarePosts(sort, category, keyword, studentId);
    }

    // 降级方法：返回热门帖子（按点赞数排序）
    private List<Post> getHotPostsFallback(String studentId) {
        List<Post> hotPosts = postService.getHotPosts(20);
        if (studentId != null && !studentId.isEmpty()) {
            postService.enrichPosts(hotPosts, studentId);
        }
        postService.fillNicknames(hotPosts);
        return hotPosts;
    }

    // 内部类，用于映射推荐服务返回的 JSON（忽略未知字段）
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RecommendResponse {
        @JsonProperty("post_id")
        private Long post_id;
        public Long getPostId() { return post_id; }
        public void setPostId(Long post_id) { this.post_id = post_id; }
    }
}