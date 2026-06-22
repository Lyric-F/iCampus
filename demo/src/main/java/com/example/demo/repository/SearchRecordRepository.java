package com.example.demo.repository;

import com.example.demo.entity.SearchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface SearchRecordRepository extends JpaRepository<SearchRecord, Integer> {

    // 获取某个用户的所有搜索记录，按时间倒序
    List<SearchRecord> findByStudentIdOrderByCreateTimeDesc(String studentId);

    // 删除单条记录
    @Modifying
    @Transactional
    @Query("DELETE FROM SearchRecord r WHERE r.studentId = :studentId AND r.id = :recordId")
    void deleteByIdAndStudentId(@Param("studentId") String studentId, @Param("recordId") Integer recordId);

    // 清空用户所有搜索记录
    @Modifying
    @Transactional
    @Query("DELETE FROM SearchRecord r WHERE r.studentId = :studentId")
    void deleteAllByStudentId(@Param("studentId") String studentId);
}