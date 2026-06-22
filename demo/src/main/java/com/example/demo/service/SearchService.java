package com.example.demo.service;

import com.example.demo.entity.SearchRecord;
import com.example.demo.repository.SearchRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SearchService {

    @Autowired
    private SearchRecordRepository searchRecordRepository;

    @Transactional
    public void saveSearchRecord(String studentId, String keyword) {
        SearchRecord record = new SearchRecord();
        record.setStudentId(studentId);
        record.setKeyword(keyword);
        record.setCreateTime(LocalDateTime.now());
        searchRecordRepository.save(record);
    }

    public List<SearchRecord> getSearchRecords(String studentId) {
        return searchRecordRepository.findByStudentIdOrderByCreateTimeDesc(studentId);
    }

    @Transactional
    public void deleteSearchRecord(String studentId, Integer recordId) {
        searchRecordRepository.deleteByIdAndStudentId(studentId, recordId);
    }

    @Transactional
    public void clearSearchRecords(String studentId) {
        searchRecordRepository.deleteAllByStudentId(studentId);
    }
}