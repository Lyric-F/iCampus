package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    // 因为主键是 studentId，所以不需要 findByStudentId 方法，直接使用 findById 即可
}