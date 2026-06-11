package com.bookexchange.service;

import com.bookexchange.dto.export.BookExportRequestDTO;
import com.bookexchange.dto.export.BorrowRecordExportRequestDTO;
import com.bookexchange.dto.export.ExportTaskDTO;
import com.bookexchange.entity.Book;
import com.bookexchange.entity.BorrowRecord;
import com.bookexchange.entity.User;
import com.bookexchange.repository.BookRepository;
import com.bookexchange.repository.BookTagRepository;
import com.bookexchange.repository.BorrowRecordRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private static final String EXPORT_TASK_KEY = "export:task:";
    private static final long TASK_EXPIRE_HOURS = 24;

    private final BookRepository bookRepository;
    private final BookTagRepository bookTagRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final ExportAsyncService exportAsyncService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${export.file.path:/tmp/exports}")
    private String exportFilePath;

    public List<String> getBookExportFields() {
        return ExportAsyncService.BOOK_EXPORT_FIELDS;
    }

    public Map<String, String> getBookFieldLabels() {
        return ExportAsyncService.getBookFieldLabels();
    }

    public List<String> getBorrowRecordExportFields() {
        return ExportAsyncService.BORROW_RECORD_EXPORT_FIELDS;
    }

    public Map<String, String> getBorrowRecordFieldLabels() {
        return ExportAsyncService.getBorrowRecordFieldLabels();
    }

    public ExportTaskDTO createBookExportTask(BookExportRequestDTO requestDTO) {
        String taskId = generateTaskId();
        ExportTaskDTO task = new ExportTaskDTO();
        task.setTaskId(taskId);
        task.setType("BOOK");
        task.setStatus("PENDING");
        task.setCreateTime(LocalDateTime.now());
        task.setTotalCount(0L);
        task.setProcessedCount(0L);

        saveTask(task);

        List<Book> books = queryBooksForExport(requestDTO);
        task.setTotalCount((long) books.size());
        saveTask(task);

        exportAsyncService.asyncExportBooks(taskId, books, requestDTO.getFields());

        return task;
    }

    public ExportTaskDTO createBorrowRecordExportTask(BorrowRecordExportRequestDTO requestDTO) {
        String taskId = generateTaskId();
        ExportTaskDTO task = new ExportTaskDTO();
        task.setTaskId(taskId);
        task.setType("BORROW_RECORD");
        task.setStatus("PENDING");
        task.setCreateTime(LocalDateTime.now());
        task.setTotalCount(0L);
        task.setProcessedCount(0L);

        saveTask(task);

        List<BorrowRecord> records = queryBorrowRecordsForExport(requestDTO);
        task.setTotalCount((long) records.size());
        saveTask(task);

        exportAsyncService.asyncExportBorrowRecords(taskId, records, requestDTO.getFields());

        return task;
    }

    public ExportTaskDTO getTaskStatus(String taskId) {
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

    public Path getExportFilePath(String taskId) {
        ExportTaskDTO task = getTaskStatus(taskId);
        if (task == null || !"COMPLETED".equals(task.getStatus())) {
            return null;
        }
        if (task.getFileName() == null) {
            return null;
        }
        Path path = Paths.get(exportFilePath, task.getFileName());
        if (Files.exists(path)) {
            return path;
        }
        return null;
    }

    private List<Book> queryBooksForExport(BookExportRequestDTO requestDTO) {
        Specification<Book> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (requestDTO.getCityId() != null) {
                predicates.add(cb.equal(root.get("city").get("id"), requestDTO.getCityId()));
            }
            if (requestDTO.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), requestDTO.getCategoryId()));
            }
            if (requestDTO.getAvailable() != null) {
                predicates.add(cb.equal(root.get("available"), requestDTO.getAvailable()));
            }
            if (requestDTO.getKeyword() != null && !requestDTO.getKeyword().isEmpty()) {
                String keyword = "%" + requestDTO.getKeyword() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("title"), keyword),
                    cb.like(root.get("author"), keyword)
                ));
            }
            if (requestDTO.getOwnerId() != null) {
                predicates.add(cb.equal(root.get("owner").get("id"), requestDTO.getOwnerId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Book> books = bookRepository.findAll(spec);

        if (requestDTO.getTagIds() != null && !requestDTO.getTagIds().isEmpty()) {
            List<Long> filteredBookIds;
            if (Boolean.TRUE.equals(requestDTO.getMatchAllTags())) {
                filteredBookIds = bookTagRepository.findBookIdsByAllTagIds(
                    requestDTO.getTagIds(), (long) requestDTO.getTagIds().size());
            } else {
                filteredBookIds = bookTagRepository.findBookIdsByTagIds(requestDTO.getTagIds());
            }
            books = books.stream()
                .filter(book -> filteredBookIds.contains(book.getId()))
                .collect(Collectors.toList());
        }

        exportAsyncService.enrichBooksWithTags(books);

        return books;
    }

    private List<BorrowRecord> queryBorrowRecordsForExport(BorrowRecordExportRequestDTO queryDTO) {
        Specification<BorrowRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (queryDTO.getCurrentUserId() != null) {
                predicates.add(cb.or(
                    cb.equal(root.get("borrower").get("id"), queryDTO.getCurrentUserId()),
                    cb.equal(root.get("owner").get("id"), queryDTO.getCurrentUserId())
                ));
            }

            if (queryDTO.getStatuses() != null && !queryDTO.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(queryDTO.getStatuses()));
            }

            if (queryDTO.getStartDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), queryDTO.getStartDateFrom()));
            }

            if (queryDTO.getStartDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), queryDTO.getStartDateTo()));
            }

            if (queryDTO.getCategoryId() != null) {
                Join<BorrowRecord, Book> bookJoin = root.join("book", JoinType.INNER);
                predicates.add(cb.equal(bookJoin.get("category").get("id"), queryDTO.getCategoryId()));
            }

            if (queryDTO.getBorrowerId() != null) {
                predicates.add(cb.equal(root.get("borrower").get("id"), queryDTO.getBorrowerId()));
            }

            if (queryDTO.getOwnerId() != null) {
                predicates.add(cb.equal(root.get("owner").get("id"), queryDTO.getOwnerId()));
            }

            if (queryDTO.getBorrowerKeyword() != null && !queryDTO.getBorrowerKeyword().trim().isEmpty()) {
                Join<BorrowRecord, User> borrowerJoin = root.join("borrower", JoinType.INNER);
                String keyword = "%" + queryDTO.getBorrowerKeyword().trim() + "%";
                predicates.add(cb.or(
                    cb.like(borrowerJoin.get("nickname"), keyword),
                    cb.like(borrowerJoin.get("username"), keyword)
                ));
            }

            if (queryDTO.getOwnerKeyword() != null && !queryDTO.getOwnerKeyword().trim().isEmpty()) {
                Join<BorrowRecord, User> ownerJoin = root.join("owner", JoinType.INNER);
                String keyword = "%" + queryDTO.getOwnerKeyword().trim() + "%";
                predicates.add(cb.or(
                    cb.like(ownerJoin.get("nickname"), keyword),
                    cb.like(ownerJoin.get("username"), keyword)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return borrowRecordRepository.findAll(spec);
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

        Object createTime = map.get("createTime");
        if (createTime != null && createTime instanceof String) {
            task.setCreateTime(LocalDateTime.parse((String) createTime));
        }
        Object finishTime = map.get("finishTime");
        if (finishTime != null && finishTime instanceof String) {
            task.setFinishTime(LocalDateTime.parse((String) finishTime));
        }

        return task;
    }

    private String generateTaskId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void saveTask(ExportTaskDTO task) {
        String key = EXPORT_TASK_KEY + task.getTaskId();
        redisTemplate.opsForValue().set(key, task, TASK_EXPIRE_HOURS, TimeUnit.HOURS);
    }
}
