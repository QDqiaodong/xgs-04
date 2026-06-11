package com.bookexchange.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.bookexchange.dto.export.ExportTaskDTO;
import com.bookexchange.entity.Book;
import com.bookexchange.entity.BookTag;
import com.bookexchange.entity.BorrowRecord;
import com.bookexchange.entity.Category;
import com.bookexchange.entity.City;
import com.bookexchange.entity.Tag;
import com.bookexchange.entity.User;
import com.bookexchange.repository.BookRepository;
import com.bookexchange.repository.BookTagRepository;
import com.bookexchange.repository.BorrowRecordRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportAsyncService {

    private static final Logger log = LoggerFactory.getLogger(ExportAsyncService.class);

    private static final String EXPORT_TASK_KEY = "export:task:";
    private static final long TASK_EXPIRE_HOURS = 24;

    private final BookRepository bookRepository;
    private final BookTagRepository bookTagRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${export.file.path:/tmp/exports}")
    private String exportFilePath;

    @Value("${export.download.base-url:/api/exports/download}")
    private String downloadBaseUrl;

    private static final Map<String, String> BOOK_FIELD_LABELS = new LinkedHashMap<>() {{
        put("title", "书名");
        put("author", "作者");
        put("isbn", "ISBN");
        put("category", "分类");
        put("conditionLevel", "成色");
        put("description", "描述");
        put("available", "状态");
        put("canBorrow", "是否可借");
        put("owner", "所有者");
        put("city", "所在城市");
        put("createTime", "创建时间");
        put("updateTime", "更新时间");
        put("tags", "标签");
    }};

    private static final Map<String, String> BORROW_RECORD_FIELD_LABELS = new LinkedHashMap<>() {{
        put("bookTitle", "图书名称");
        put("bookCategory", "图书分类");
        put("borrower", "借阅人");
        put("owner", "所有者");
        put("startDate", "开始日期");
        put("endDate", "结束日期");
        put("borrowTime", "借出时间");
        put("returnTime", "归还时间");
        put("status", "状态");
        put("remark", "备注");
        put("overdueDays", "逾期天数");
        put("overdueFine", "逾期罚金");
        put("createTime", "创建时间");
    }};

    public static final List<String> BOOK_EXPORT_FIELDS = Arrays.asList(
        "title", "author", "isbn", "category", "conditionLevel",
        "description", "available", "canBorrow", "owner", "city",
        "createTime", "updateTime", "tags"
    );

    public static final List<String> BORROW_RECORD_EXPORT_FIELDS = Arrays.asList(
        "bookTitle", "bookCategory", "borrower", "owner",
        "startDate", "endDate", "borrowTime", "returnTime",
        "status", "remark", "overdueDays", "overdueFine",
        "createTime"
    );

    public static Map<String, String> getBookFieldLabels() {
        return BOOK_FIELD_LABELS;
    }

    public static Map<String, String> getBorrowRecordFieldLabels() {
        return BORROW_RECORD_FIELD_LABELS;
    }

    @Async("exportTaskExecutor")
    public void asyncExportBooks(String taskId, List<Book> books, List<String> fields) {
        ExportTaskDTO task = getTask(taskId);
        if (task == null) {
            return;
        }

        try {
            task.setStatus("PROCESSING");
            saveTask(task);

            List<String> exportFields = (fields != null && !fields.isEmpty()) ? fields : BOOK_EXPORT_FIELDS;

            List<Map<String, Object>> dataList = new ArrayList<>();
            int processed = 0;

            for (Book book : books) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String field : exportFields) {
                    row.put(BOOK_FIELD_LABELS.getOrDefault(field, field), getBookFieldValue(book, field));
                }
                dataList.add(row);
                processed++;
                if (processed % 50 == 0) {
                    task.setProcessedCount((long) processed);
                    saveTask(task);
                }
            }

            String fileName = generateFileName("图书列表");
            Path filePath = Paths.get(exportFilePath, fileName);
            Files.createDirectories(filePath.getParent());

            generateExcel(filePath.toFile(), dataList);

            task.setStatus("COMPLETED");
            task.setFileName(fileName);
            task.setDownloadUrl(downloadBaseUrl + "/" + taskId);
            task.setProcessedCount((long) processed);
            task.setFinishTime(LocalDateTime.now());
            saveTask(task);

            log.info("Book export completed: taskId={}, count={}", taskId, processed);

        } catch (Exception e) {
            log.error("Book export failed: taskId={}", taskId, e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setFinishTime(LocalDateTime.now());
            saveTask(task);
        }
    }

    @Async("exportTaskExecutor")
    public void asyncExportBorrowRecords(String taskId, List<BorrowRecord> records, List<String> fields) {
        ExportTaskDTO task = getTask(taskId);
        if (task == null) {
            return;
        }

        try {
            task.setStatus("PROCESSING");
            saveTask(task);

            List<String> exportFields = (fields != null && !fields.isEmpty()) ? fields : BORROW_RECORD_EXPORT_FIELDS;

            List<Map<String, Object>> dataList = new ArrayList<>();
            int processed = 0;

            for (BorrowRecord record : records) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String field : exportFields) {
                    row.put(BORROW_RECORD_FIELD_LABELS.getOrDefault(field, field),
                        getBorrowRecordFieldValue(record, field));
                }
                dataList.add(row);
                processed++;
                if (processed % 50 == 0) {
                    task.setProcessedCount((long) processed);
                    saveTask(task);
                }
            }

            String fileName = generateFileName("借阅记录");
            Path filePath = Paths.get(exportFilePath, fileName);
            Files.createDirectories(filePath.getParent());

            generateExcel(filePath.toFile(), dataList);

            task.setStatus("COMPLETED");
            task.setFileName(fileName);
            task.setDownloadUrl(downloadBaseUrl + "/" + taskId);
            task.setProcessedCount((long) processed);
            task.setFinishTime(LocalDateTime.now());
            saveTask(task);

            log.info("Borrow record export completed: taskId={}, count={}", taskId, processed);

        } catch (Exception e) {
            log.error("Borrow record export failed: taskId={}", taskId, e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setFinishTime(LocalDateTime.now());
            saveTask(task);
        }
    }

    public void enrichBooksWithTags(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return;
        }
        for (Book book : books) {
            List<BookTag> bookTags = bookTagRepository.findByBookIdWithTag(book.getId());
            List<Tag> tags = bookTags.stream()
                .map(BookTag::getTag)
                .collect(Collectors.toList());
            book.setTags(tags);
        }
    }

    private ExportTaskDTO getTask(String taskId) {
        String key = EXPORT_TASK_KEY + taskId;
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj instanceof ExportTaskDTO) {
            return (ExportTaskDTO) obj;
        }
        if (obj instanceof Map) {
            return convertMapToTask((Map<?, ?>) obj);
        }
        return null;
    }

    private ExportTaskDTO convertMapToTask(Map<?, ?> map) {
        ExportTaskDTO task = new ExportTaskDTO();
        task.setTaskId((String) map.get("taskId"));
        task.setType((String) map.get("type"));
        task.setStatus((String) map.get("status"));
        task.setFileName((String) map.get("fileName"));
        task.setDownloadUrl((String) map.get("downloadUrl"));
        task.setErrorMessage((String) map.get("errorMessage"));

        Object totalCount = map.get("totalCount");
        if (totalCount != null) {
            task.setTotalCount(((Number) totalCount).longValue());
        }
        Object processedCount = map.get("processedCount");
        if (processedCount != null) {
            task.setProcessedCount(((Number) processedCount).longValue());
        }

        return task;
    }

    private void saveTask(ExportTaskDTO task) {
        String key = EXPORT_TASK_KEY + task.getTaskId();
        redisTemplate.opsForValue().set(key, task, TASK_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    private void generateExcel(File file, List<Map<String, Object>> dataList) {
        if (dataList.isEmpty()) {
            EasyExcel.write(file).head(Arrays.asList("导出数据为空")).sheet("导出数据").doWrite(new ArrayList<>());
            return;
        }

        List<String> headList = new ArrayList<>(dataList.get(0).keySet());
        List<List<String>> head = headList.stream()
            .map(h -> Arrays.asList(h))
            .collect(Collectors.toList());

        List<List<Object>> data = dataList.stream()
            .map(row -> new ArrayList<>(row.values()))
            .collect(Collectors.toList());

        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        headWriteCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        WriteFont headWriteFont = new WriteFont();
        headWriteFont.setFontHeightInPoints((short) 11);
        headWriteFont.setBold(true);
        headWriteCellStyle.setWriteFont(headWriteFont);
        headWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);

        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.LEFT);

        HorizontalCellStyleStrategy strategy = new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle);

        EasyExcel.write(file)
            .head(head)
            .registerWriteHandler(strategy)
            .sheet("导出数据")
            .doWrite(data);
    }

    private Object getBookFieldValue(Book book, String field) {
        switch (field) {
            case "title":
                return book.getTitle();
            case "author":
                return book.getAuthor();
            case "isbn":
                return book.getIsbn() != null ? book.getIsbn() : "";
            case "category":
                Category category = book.getCategory();
                return category != null ? category.getName() : "";
            case "conditionLevel":
                return book.getConditionLevel() != null ? book.getConditionLevel() : "";
            case "description":
                return book.getDescription() != null ? book.getDescription() : "";
            case "available":
                return Boolean.TRUE.equals(book.getAvailable()) ? "上架" : "下架";
            case "canBorrow":
                return Boolean.TRUE.equals(book.getCanBorrow()) ? "可借" : "不可借";
            case "owner":
                User owner = book.getOwner();
                return owner != null ? owner.getNickname() : "";
            case "city":
                City city = book.getCity();
                return city != null ? city.getName() : "";
            case "createTime":
                return book.getCreateTime() != null ?
                    book.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
            case "updateTime":
                return book.getUpdateTime() != null ?
                    book.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
            case "tags":
                if (book.getTags() != null && !book.getTags().isEmpty()) {
                    return book.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.joining("、"));
                }
                return "";
            default:
                return "";
        }
    }

    private Object getBorrowRecordFieldValue(BorrowRecord record, String field) {
        switch (field) {
            case "bookTitle":
                Book book = record.getBook();
                return book != null ? book.getTitle() : "";
            case "bookCategory":
                Book b = record.getBook();
                if (b != null && b.getCategory() != null) {
                    return b.getCategory().getName();
                }
                return "";
            case "borrower":
                User borrower = record.getBorrower();
                return borrower != null ? borrower.getNickname() : "";
            case "owner":
                User owner = record.getOwner();
                return owner != null ? owner.getNickname() : "";
            case "startDate":
                return record.getStartDate() != null ?
                    record.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
            case "endDate":
                return record.getEndDate() != null ?
                    record.getEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
            case "borrowTime":
                return record.getBorrowTime() != null ?
                    record.getBorrowTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
            case "returnTime":
                return record.getReturnTime() != null ?
                    record.getReturnTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
            case "status":
                return getStatusText(record.getStatus());
            case "remark":
                return record.getRemark() != null ? record.getRemark() : "";
            case "overdueDays":
                return record.getOverdueDays() != null ? record.getOverdueDays() : 0;
            case "overdueFine":
                return record.getOverdueFine() != null ? record.getOverdueFine() : 0;
            case "createTime":
                return record.getCreateTime() != null ?
                    record.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
            default:
                return "";
        }
    }

    private String getStatusText(String status) {
        if (status == null) return "";
        switch (status) {
            case "PENDING":
                return "待审核";
            case "APPROVED":
                return "已同意";
            case "REJECTED":
                return "已拒绝";
            case "BORROWING":
                return "借阅中";
            case "OVERDUE":
                return "已逾期";
            case "RETURNED":
                return "已归还";
            default:
                return status;
        }
    }

    private String generateFileName(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return prefix + "_" + timestamp + ".xlsx";
    }
}
