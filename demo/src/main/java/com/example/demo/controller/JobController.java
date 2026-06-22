package com.example.demo.controller;

import com.example.demo.entity.Job;
import com.example.demo.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/job")
@CrossOrigin(origins = "*")
public class JobController {

    @Autowired
    private JobService jobService;

    // 获取所有兼职
    @GetMapping("/list")
    public List<Job> getAllJobs() {
        return jobService.getAllJobs();
    }

    // 获取单个兼职详情
    @GetMapping("/{id}")
    public Job getJobById(@PathVariable Long id) {
        return jobService.getJobById(id);
    }

    // 发布兼职
    @PostMapping("/publish")
    public Map<String, Object> publish(@RequestParam String studentId,
                                       @RequestParam String title,
                                       @RequestParam(required = false) String workTime,
                                       @RequestParam(required = false) String location,
                                       @RequestParam(required = false) String peopleCount,
                                       @RequestParam(required = false) String requirements,
                                       @RequestParam(required = false) String description,
                                       @RequestParam(required = false) String phone) {
        Map<String, Object> result = new HashMap<>();
        try {
            Job job = jobService.publish(studentId, title, workTime, location,
                    peopleCount, requirements, description, phone);
            result.put("success", true);
            result.put("job", job);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 获取当前用户的兼职
    @GetMapping("/my")
    public List<Job> getMyJobs(@RequestParam String studentId) {
        return jobService.getMyJobs(studentId);
    }

    // 删除兼职
    @PostMapping("/delete")
    public Map<String, Object> deleteJob(@RequestParam String studentId,
                                         @RequestParam Long jobId) {
        Map<String, Object> result = new HashMap<>();
        try {
            jobService.deleteJob(studentId, jobId);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}