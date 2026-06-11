package com.bookexchange.controller;

import com.bookexchange.dto.Result;
import com.bookexchange.dto.export.BookExportRequestDTO;
import com.bookexchange.dto.export.BorrowRecordExportRequestDTO;
import com.bookexchange.dto.export.ExportTaskDTO;
import com.bookexchange.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/books/fields")
    public Result<List<Map<String, String>>> getBookExportFields() {
        List<Map<String, String>> fields = new ArrayList<>();
        Map<String, String> labels = exportService.getBookFieldLabels();
        for (String field : exportService.getBookExportFields()) {
            Map<String, String> item = new HashMap<>();
            item.put("key", field);
            item.put("label", labels.getOrDefault(field, field));
            fields.add(item);
        }
        return Result.success(fields);
    }

    @GetMapping("/borrow-records/fields")
    public Result<List<Map<String, String>>> getBorrowRecordExportFields() {
        List<Map<String, String>> fields = new ArrayList<>();
        Map<String, String> labels = exportService.getBorrowRecordFieldLabels();
        for (String field : exportService.getBorrowRecordExportFields()) {
            Map<String, String> item = new HashMap<>();
            item.put("key", field);
            item.put("label", labels.getOrDefault(field, field));
            fields.add(item);
        }
        return Result.success(fields);
    }

    @PostMapping("/books")
    public Result<ExportTaskDTO> createBookExportTask(@RequestBody BookExportRequestDTO requestDTO) {
        ExportTaskDTO task = exportService.createBookExportTask(requestDTO);
        return Result.success(task);
    }

    @PostMapping("/borrow-records")
    public Result<ExportTaskDTO> createBorrowRecordExportTask(@RequestBody BorrowRecordExportRequestDTO requestDTO) {
        ExportTaskDTO task = exportService.createBorrowRecordExportTask(requestDTO);
        return Result.success(task);
    }

    @GetMapping("/{taskId}")
    public Result<ExportTaskDTO> getExportTaskStatus(@PathVariable String taskId) {
        ExportTaskDTO task = exportService.getTaskStatus(taskId);
        if (task == null) {
            return Result.error("导出任务不存在");
        }
        return Result.success(task);
    }

    @GetMapping("/download/{taskId}")
    public ResponseEntity<Resource> downloadExportFile(@PathVariable String taskId) {
        Path filePath = exportService.getExportFilePath(taskId);
        if (filePath == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String fileName = filePath.getFileName().toString();
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                .replace("+", "%20");

            Resource resource = new PathResource(filePath);

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
