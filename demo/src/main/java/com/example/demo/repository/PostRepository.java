package com.example.demo.repository;

import com.example.demo.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {


    // 根据发布者学号查询
    List<Post> findByStudentId(String studentId);

    // 根据发布者学号删除
    void deleteByStudentId(String studentId);

    // 按分类查询，按创建时间倒序
    List<Post> findByTypeOrderByCreateTimeDesc(String type);

    // 查询所有，按创建时间倒序
    List<Post> findAllByOrderByCreateTimeDesc();

    // 按关键词搜索标题或内容
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword%")
    List<Post> searchByKeyword(@Param("keyword") String keyword);

    // 分类+关键词组合查询
    @Query("SELECT p FROM Post p WHERE p.type = :type AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    List<Post> findByTypeAndKeyword(@Param("type") String type, @Param("keyword") String keyword);

    // 新增：按分类和关键词搜索帖子
    @Query("SELECT p FROM Post p WHERE " +
            "(:category IS NULL OR p.type = :category) AND " +
            "(:keyword IS NULL OR p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
            "ORDER BY p.createTime DESC")
    List<Post> findByCategoryAndKeyword(@Param("category") String category,
                                        @Param("keyword") String keyword);
    int countByStudentId(String studentId);
    List<Post> findByStudentIdOrderByCreateTimeDesc(String studentId);

    // 新增：获取热榜（按点赞数降序）
    @Query("SELECT p FROM Post p ORDER BY p.likeCount DESC")
    List<Post> findTopByLikeCount(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.type IN :types ORDER BY p.createTime DESC")
    List<Post> findByTypesOrderByCreateTimeDesc(@Param("types") List<String> types);

    @Query("SELECT p FROM Post p WHERE p.type IN :types AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) ORDER BY p.createTime DESC")
    List<Post> findByTypesAndKeyword(@Param("types") List<String> types, @Param("keyword") String keyword);
}