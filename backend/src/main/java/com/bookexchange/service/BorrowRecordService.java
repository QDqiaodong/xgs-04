package com.bookexchange.service;

import com.bookexchange.dto.BorrowRecordDTO;
import com.bookexchange.dto.BorrowRecordQueryDTO;
import com.bookexchange.dto.OverdueQueryDTO;
import com.bookexchange.dto.ValidationResult;
import com.bookexchange.entity.Book;
import com.bookexchange.entity.BorrowRecord;
import com.bookexchange.entity.BorrowRule;
import com.bookexchange.entity.Reservation;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BorrowRecordService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookService bookService;
    private final BorrowRuleService borrowRuleService;
    private final UserPointsService userPointsService;
    private final NotificationService notificationService;
    private final ReservationService reservationService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BORROWED_RECORD_KEY = "borrowed:record:";
    private static final String USER_FILTER_KEY = "user:filter:";
    private static final String BORROW_LOCK_KEY = "borrow:lock:";
    private static final List<String> ACTIVE_STATUSES = Arrays.asList("PENDING", "APPROVED", "BORROWING", "OVERDUE");

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

    public ValidationResult validateCreateBorrowRecord(BorrowRecordDTO dto) {
        Book book = bookRepository.findById(dto.getBookId()).orElse(null);
        User borrower = userRepository.findById(dto.getBorrowerId()).orElse(null);

        if (book == null || borrower == null) {
            return ValidationResult.fail("图书或用户不存在");
        }

        if (!book.getCanBorrow()) {
            return ValidationResult.fail("该图书不允许借阅");
        }

        boolean hasActiveBorrow = borrowRecordRepository.existsByBookIdAndBorrowerIdAndStatusIn(
            dto.getBookId(), dto.getBorrowerId(), ACTIVE_STATUSES
        );
        if (hasActiveBorrow) {
            return ValidationResult.fail("您已对该图书存在借阅申请，请勿重复提交");
        }

        Reservation activeReservation = reservationService.getActiveReservationForUserAndBook(
            dto.getBorrowerId(), dto.getBookId()
        );
        boolean isReservationUser = activeReservation != null
            && Reservation.STATUS_NOTIFIED.equals(activeReservation.getStatus());

        if (!isReservationUser) {
            if (!book.getAvailable()) {
                return ValidationResult.fail("该图书当前不可用，您可以预约排队");
            }

            long bookActiveCount = borrowRecordRepository.countByBookIdAndStatusIn(
                dto.getBookId(), ACTIVE_STATUSES
            );
            if (bookActiveCount > 0) {
                return ValidationResult.fail("该图书已被他人申请借阅，暂时不可用，您可以预约排队");
            }
        }

        BorrowRule rule = borrowRuleService.getBorrowRule();
        int maxBorrowCount = userPointsService.getMaxBorrowCount(dto.getBorrowerId());

        long activeCount = borrowRecordRepository.countByBorrowerIdAndStatusIn(
            dto.getBorrowerId(), ACTIVE_STATUSES
        );
        if (activeCount >= maxBorrowCount) {
            return ValidationResult.fail(
                "已达最大借阅数量限制，当前等级最多可同时借阅 " + maxBorrowCount + " 本图书"
            );
        }

        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                return ValidationResult.fail("结束日期不能早于开始日期");
            }
            long days = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate());
            if (days > rule.getMaxBorrowDays()) {
                return ValidationResult.fail(
                    "借阅天数超过最大限制，单次最长可借阅 " + rule.getMaxBorrowDays() + " 天"
                );
            }
        }

        return ValidationResult.success();
    }

    @Transactional(rollbackFor = Exception.class)
    public BorrowRecord createBorrowRecord(BorrowRecordDTO dto) {
        String lockKey = BORROW_LOCK_KEY + dto.getBookId() + ":" + dto.getBorrowerId();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            return null;
        }

        try {
            ValidationResult validation = validateCreateBorrowRecord(dto);
            if (!validation.isValid()) {
                return null;
            }

            Book book = bookRepository.findById(dto.getBookId()).orElse(null);
            User borrower = userRepository.findById(dto.getBorrowerId()).orElse(null);

            BorrowRecord record = new BorrowRecord();
            record.setBook(book);
            record.setBorrower(borrower);
            record.setOwner(book.getOwner());
            record.setStatus("PENDING");
            record.setStartDate(dto.getStartDate());
            record.setEndDate(dto.getEndDate());
            record.setRemark(dto.getRemark());

            BorrowRecord saved = borrowRecordRepository.save(record);
            notificationService.notifyBorrowRequest(saved);

            Reservation activeReservation = reservationService.getActiveReservationForUserAndBook(
                dto.getBorrowerId(), dto.getBookId()
            );
            if (activeReservation != null
                && Reservation.STATUS_NOTIFIED.equals(activeReservation.getStatus())) {
                reservationService.confirmBorrowReservation(activeReservation.getId(), dto.getBorrowerId());
            }

            return saved;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    public ValidationResult validateApproveBorrowRecord(Long id) {
        BorrowRecord record = borrowRecordRepository.findById(id).orElse(null);
        if (record == null) {
            return ValidationResult.fail("借阅记录不存在");
        }
        if (!"PENDING".equals(record.getStatus())) {
            return ValidationResult.fail("该借阅申请当前状态不允许审核");
        }

        BorrowRule rule = borrowRuleService.getBorrowRule();
        int maxBorrowCount = userPointsService.getMaxBorrowCount(record.getBorrower().getId());

        long activeCount = borrowRecordRepository.countByBorrowerIdAndStatusIn(
            record.getBorrower().getId(), ACTIVE_STATUSES
        );
        if (activeCount >= maxBorrowCount) {
            return ValidationResult.fail(
                "借阅人已达最大借阅数量限制，当前等级最多可同时借阅 " + maxBorrowCount + " 本图书"
            );
        }

        if (record.getStartDate() != null && record.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(record.getStartDate(), record.getEndDate());
            if (days > rule.getMaxBorrowDays()) {
                return ValidationResult.fail(
                    "借阅天数超过最大限制，单次最长可借阅 " + rule.getMaxBorrowDays() + " 天"
                );
            }
        }

        return ValidationResult.success();
    }

    public BorrowRecord approveBorrowRecord(Long id) {
        ValidationResult validation = validateApproveBorrowRecord(id);
        if (!validation.isValid()) {
            return null;
        }

        BorrowRecord record = borrowRecordRepository.findById(id).orElse(null);

        record.setStatus("APPROVED");
        bookService.updateBookAvailability(record.getBook().getId(), false);

        BorrowRecord saved = borrowRecordRepository.save(record);
        redisTemplate.opsForValue().set(BORROWED_RECORD_KEY + id, saved, 1, TimeUnit.HOURS);

        userPointsService.awardLendPoints(record.getOwner().getId(), id);
        notificationService.notifyApprovalResult(saved, true);

        return saved;
    }

    public BorrowRecord rejectBorrowRecord(Long id) {
        BorrowRecord record = borrowRecordRepository.findById(id).orElse(null);
        if (record == null || !"PENDING".equals(record.getStatus())) {
            return null;
        }

        record.setStatus("REJECTED");
        BorrowRecord saved = borrowRecordRepository.save(record);
        notificationService.notifyApprovalResult(saved, false);
        return saved;
    }

    public BorrowRecord confirmBorrow(Long id) {
        BorrowRecord record = borrowRecordRepository.findById(id).orElse(null);
        if (record == null || !"APPROVED".equals(record.getStatus())) {
            return null;
        }

        record.setStatus("BORROWING");
        record.setBorrowTime(LocalDateTime.now());
        BorrowRecord saved = borrowRecordRepository.save(record);

        Reservation reservation = reservationService.getActiveReservationForUserAndBook(
            record.getBorrower().getId(), record.getBook().getId()
        );
        if (reservation != null
            && Reservation.STATUS_CONFIRMED.equals(reservation.getStatus())) {
            reservationService.linkBorrowRecord(reservation.getId(), id);
        }

        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public BorrowRecord confirmReturn(Long id) {
        BorrowRecord record = borrowRecordRepository.findById(id).orElse(null);
        if (record == null) {
            return null;
        }
        if (!"BORROWING".equals(record.getStatus()) && !"OVERDUE".equals(record.getStatus())) {
            return null;
        }

        boolean wasOverdue = "OVERDUE".equals(record.getStatus());
        int overdueDays = 0;

        if (wasOverdue) {
            BorrowRule rule = borrowRuleService.getBorrowRule();
            overdueDays = calculateOverdueDays(record);
            double dailyRate = rule.getDailyFineRate() != null ? rule.getDailyFineRate() : 0.5;
            record.setOverdueDays(overdueDays);
            record.setOverdueFine(Math.round(overdueDays * dailyRate * 100.0) / 100.0);
        } else {
            if (record.getEndDate() != null && LocalDate.now().isAfter(record.getEndDate())) {
                overdueDays = (int) ChronoUnit.DAYS.between(record.getEndDate(), LocalDate.now());
            }
        }

        record.setStatus("RETURNED");
        record.setReturnTime(LocalDateTime.now());

        boolean hasWaitingReservations = reservationService.getReservationsByBookId(record.getBook().getId())
            .stream()
            .anyMatch(r -> Reservation.STATUS_WAITING.equals(r.getStatus()));

        if (!hasWaitingReservations) {
            bookService.updateBookAvailability(record.getBook().getId(), true);
        }

        BorrowRecord saved = borrowRecordRepository.save(record);

        if (overdueDays > 0) {
            userPointsService.deductOverduePoints(record.getBorrower().getId(), overdueDays, id);
        } else {
            userPointsService.awardReturnOnTimePoints(record.getBorrower().getId(), id);
        }

        notificationService.notifyReturnConfirm(saved);

        if (hasWaitingReservations) {
            reservationService.notifyNextWaiter(record.getBook().getId());
        }

        redisTemplate.delete(BORROWED_RECORD_KEY + id);
        return saved;
    }

    public void saveUserFilterConditions(Long userId, Object filterConditions) {
        String key = USER_FILTER_KEY + userId;
        redisTemplate.opsForValue().set(key, filterConditions, 7, TimeUnit.DAYS);
    }

    public Object getUserFilterConditions(Long userId) {
        String key = USER_FILTER_KEY + userId;
        return redisTemplate.opsForValue().get(key);
    }

    private int calculateOverdueDays(BorrowRecord record) {
        if (record.getEndDate() == null) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(record.getEndDate(), today);
        return days > 0 ? (int) days : 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public int identifyOverdueRecords() {
        LocalDate today = LocalDate.now();
        List<BorrowRecord> overdueRecords = borrowRecordRepository.findOverdueNotMarked(today);
        int count = 0;
        for (BorrowRecord record : overdueRecords) {
            int overdueDays = calculateOverdueDays(record);
            borrowRecordRepository.markAsOverdue(record.getId(), overdueDays);
            count++;
        }
        return count;
    }

    public Page<BorrowRecord> queryOverdueRecords(OverdueQueryDTO queryDTO) {
        Sort sort;
        if ("overdueDays".equals(queryDTO.getSortBy())) {
            Sort.Direction direction = "asc".equalsIgnoreCase(queryDTO.getSortOrder())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, "overdueDays");
        } else {
            sort = Sort.by(Sort.Direction.DESC, "overdueDays");
        }

        Pageable pageable = PageRequest.of(
            queryDTO.getPageNum() - 1,
            queryDTO.getPageSize(),
            sort
        );

        Specification<BorrowRecord> spec = buildOverdueSpecification(queryDTO);
        return borrowRecordRepository.findAll(spec, pageable);
    }

    private Specification<BorrowRecord> buildOverdueSpecification(OverdueQueryDTO queryDTO) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), "OVERDUE"));

            if (queryDTO.getBorrowerId() != null) {
                predicates.add(cb.equal(root.get("borrower").get("id"), queryDTO.getBorrowerId()));
            }

            if (queryDTO.getOwnerId() != null) {
                predicates.add(cb.equal(root.get("owner").get("id"), queryDTO.getOwnerId()));
            }

            if (queryDTO.getBookId() != null) {
                predicates.add(cb.equal(root.get("book").get("id"), queryDTO.getBookId()));
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

            if (StringUtils.hasText(queryDTO.getBookTitleKeyword())) {
                Join<BorrowRecord, Book> bookJoin = root.join("book", JoinType.INNER);
                String keyword = "%" + queryDTO.getBookTitleKeyword().trim() + "%";
                predicates.add(cb.like(bookJoin.get("title"), keyword));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
