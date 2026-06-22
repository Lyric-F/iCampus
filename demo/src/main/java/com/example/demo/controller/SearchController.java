package com.example.demo.controller;

import com.example.demo.entity.SearchRecord;
import com.example.demo.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @PostMapping("/save")
    public Map<String, Object> saveSearchRecord(@RequestParam String studentId,
                                                @RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();
        try {
            searchService.saveSearchRecord(studentId, keyword);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/list")
    public List<SearchRecord> getSearchRecords(@RequestParam String studentId) {
        return searchService.getSearchRecords(studentId);
    }

    @PostMapping("/delete")
    public Map<String, Object> deleteSearchRecord(@RequestParam String studentId,
                                                  @RequestParam Integer recordId) {
        Map<String, Object> result = new HashMap<>();
        try {
            searchService.deleteSearchRecord(studentId, recordId);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/clear")
    public Map<String, Object> clearSearchRecords(@RequestParam String studentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            searchService.clearSearchRecords(studentId);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}