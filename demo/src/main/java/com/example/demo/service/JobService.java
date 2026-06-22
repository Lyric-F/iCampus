package com.example.demo.service;

import com.example.demo.entity.Job;
import com.example.demo.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    // 获取所有兼职（按发布时间倒序）
    public List<Job> getAllJobs() {
        return jobRepository.findAllByOrderByCreateTimeDesc();
    }

    // 根据ID获取兼职
    public Job getJobById(Long id) {
        return jobRepository.findById(id).orElseThrow(() -> new RuntimeException("兼职不存在"));
    }

    // 发布兼职
    public Job publish(String studentId, String title, String workTime,
                       String location, String peopleCount,
                       String requirements, String description) {
        Job job = new Job(studentId, title, workTime, location, peopleCount, requirements, description);
        return jobRepository.save(job);
    }

    // 获取当前用户发布的兼职
    public List<Job> getMyJobs(String studentId) {
        return jobRepository.findByStudentIdOrderByCreateTimeDesc(studentId);
    }

    // 发布兼职
    public Job publish(String studentId, String title, String workTime,
                       String location, String peopleCount,
                       String requirements, String description, String phone) {
        Job job = new Job(studentId, title, workTime, location, peopleCount, requirements, description);
        job.setPhone(phone);  // 单独设置电话
        return jobRepository.save(job);
    }
    // 删除兼职（仅作者可删）
    @Transactional
    public void deleteJob(String studentId, Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("兼职不存在"));
        if (!job.getStudentId().equals(studentId)) {
            throw new RuntimeException("无权删除");
        }
        jobRepository.deleteById(jobId);
    }
}