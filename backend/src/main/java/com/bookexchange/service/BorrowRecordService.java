package com.bookexchange.service;

import com.bookexchange.dto.BorrowRecordDTO;
import com.bookexchange.dto.BorrowRecordQueryDTO;
import com.bookexchange.entity.Book;
import com.bookexchange.entity.BorrowRecord;
import com.bookexchange.entity.User;
import com.bookexchange.repository.BookRepository;
import com.bookexchange.repository.BorrowRecordRepository;
import com.bookexchange.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BorrowRecordService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookService bookService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BORROWED_RECORD_KEY = "borrowed:record:";
    private static final String USER_FILTER_KEY = "user:filter:";

    public List<BorrowRecord> getBorrowRecordsByBorrowerId(Long borrowerId) {
        return borrowRecordRepository.findByBorrowerIdOrderByCreateTimeDesc(borrowerId);
    }

    public List<BorrowRecord> getBorrowRecordsByOwnerId(Long ownerId) {
        return borrowRecordRepository.findByOwnerIdOrderByCreateTimeDesc(ownerId);
    }

    public BorrowRecord getBorrowRecordById(Long id) {
        return borrowRecordRepository.findById(id).orElse(null);
    }

    public Page<BorrowRecord> queryBorrowRecords(BorrowRecordQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
            queryDTO.getPageNum() - 1,
            queryDTO.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createTime")
        );

        Specification<BorrowRecord> spec = buildSpecification(queryDTO);
        return borrowRecordRepository.findAll(spec, pageable);
    }

    private Specification<BorrowRecord> buildSpecification(BorrowRecordQueryDTO queryDTO) {
        return (root, query, cb) -> {
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

            if (StringUtils.hasText(queryDTO.getBorrowerKeyword())) {
                Join<BorrowRecord, User> borrowerJoin = root.join("borrower", JoinType.INNER);
                String keyword = "%" + queryDTO.getBorrowerKeyword().trim() + "%";
                predicates.add(cb.or(
                    cb.like(borrowerJoin.get("nickname"), keyword),
                    cb.like(borrowerJoin.get("username"), keyword)
                ));
            }

            if (StringUtils.hasText(queryDTO.getOwnerKeyword())) {
                Join<BorrowRecord, User> ownerJoin = root.join("owner", JoinType.INNER);
                String keyword = "%" + queryDTO.getOwnerKeyword().trim() + "%";
                predicates.add(cb.or(
                    cb.like(ownerJoin.get("nickname"), keyword),
                    cb.like(ownerJoin.get("username"), keyword)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public BorrowRecord createBorrowRecord(BorrowRecordDTO dto) {
        Book book = bookRepository.findById(dto.getBookId()).orElse(null);
        User borrower = userRepository.findById(dto.getBorrowerId()).orElse(null);

        if (book == null || borrower == null) {
            return null;
        }

        if (!book.getCanBorrow() || !book.getAvailable()) {
            return null;
        }

        BorrowRecord record = new BorrowRecord();
        record.setBook(book);
        record.setBorrower(borrower);
        record.setOwner(book.getOwner());
        record.setStatus("PENDING");
        record.setStartDate(dto.getStartDate());
        record.setEndDate(dto.getEndDate());
        record.setRemark(dto.getRemark());

        return borrowRecordRepository.save(record);
    }

    public BorrowRecord approveBorrowRecord(Long id) {
        BorrowRecord record = borrowRecordRepository.findById(id).orElse(null);
        if (record == null || !"PENDING".equals(record.getStatus())) {
            return null;
        }

        record.setStatus("APPROVED");
        bookService.updateBookAvailability(record.getBook().getId(), false);

        BorrowRecord saved = borrowRecordRepository.save(record);
        redisTemplate.opsForValue().set(BORROWED_RECORD_KEY + id, saved, 1, TimeUnit.HOURS);
        return saved;
    }

    public BorrowRecord rejectBorrowRecord(Long id) {
        BorrowRecord record = borrowRecordRepository.findById(id).orElse(null);
        if (record == null || !"PENDING".equals(record.getStatus())) {
            return null;
        }

        record.setStatus("REJECTED");
        return borrowRecordRepository.save(record);
    }

    public BorrowRecord confirmBorrow(Long id) {
        BorrowRecord record = borrowRecordRepository.findById(id).orElse(null);
        if (record == null || !"APPROVED".equals(record.getStatus())) {
            return null;
        }

        record.setStatus("BORROWING");
        record.setBorrowTime(LocalDateTime.now());
        return borrowRecordRepository.save(record);
    }

    public BorrowRecord confirmReturn(Long id) {
        BorrowRecord record = borrowRecordRepository.findById(id).orElse(null);
        if (record == null || !"BORROWING".equals(record.getStatus())) {
            return null;
        }

        record.setStatus("RETURNED");
        record.setReturnTime(LocalDateTime.now());
        bookService.updateBookAvailability(record.getBook().getId(), true);

        redisTemplate.delete(BORROWED_RECORD_KEY + id);
        return borrowRecordRepository.save(record);
    }

    public void saveUserFilterConditions(Long userId, Object filterConditions) {
        String key = USER_FILTER_KEY + userId;
        redisTemplate.opsForValue().set(key, filterConditions, 7, TimeUnit.DAYS);
    }

    public Object getUserFilterConditions(Long userId) {
        String key = USER_FILTER_KEY + userId;
        return redisTemplate.opsForValue().get(key);
    }
}
