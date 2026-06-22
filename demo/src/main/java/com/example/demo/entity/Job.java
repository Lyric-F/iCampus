package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, length = 20)
    private String studentId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "work_time", length = 100)
    private String workTime;

    @Column(length = 200)
    private String location;

    @Column(name = "people_count", length = 50)
    private String peopleCount;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(length = 50)
    private String nickname;

    // 添加 getter 和 setter
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    // 无参构造
    public Job() {}

    // 带参构造（方便）
    public Job(String studentId, String title, String workTime, String location,
               String peopleCount, String requirements, String description) {
        this.studentId = studentId;
        this.title = title;
        this.workTime = workTime;
        this.location = location;
        this.peopleCount = peopleCount;
        this.requirements = requirements;
        this.description = description;
        this.createTime = LocalDateTime.now();
    }

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getWorkTime() { return workTime; }
    public void setWorkTime(String workTime) { this.workTime = workTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPeopleCount() { return peopleCount; }
    public void setPeopleCount(String peopleCount) { this.peopleCount = peopleCount; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}