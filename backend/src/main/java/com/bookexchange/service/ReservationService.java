package com.bookexchange.service;

import com.bookexchange.dto.ReservationDTO;
import com.bookexchange.dto.ReservationPositionDTO;
import com.bookexchange.dto.ReservationQueryDTO;
import com.bookexchange.dto.ValidationResult;
import com.bookexchange.entity.Book;
import com.bookexchange.entity.BorrowRecord;
import com.bookexchange.entity.Reservation;
import com.bookexchange.entity.User;
import com.bookexchange.repository.BookRepository;
import com.bookexchange.repository.BorrowRecordRepository;
import com.bookexchange.repository.ReservationRepository;
import com.bookexchange.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final BorrowRuleService borrowRuleService;
    private final NotificationService notificationService;
    private final BookService bookService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RESERVATION_LOCK_KEY = "reservation:lock:";
    private static final List<String> ACTIVE_RESERVATION_STATUSES = Arrays.asList(
        Reservation.STATUS_WAITING, Reservation.STATUS_NOTIFIED
    );
    private static final List<String> BORROW_ACTIVE_STATUSES = Arrays.asList(
        "PENDING", "APPROVED", "BORROWING", "OVERDUE"
    );

    public ValidationResult validateCreateReservation(ReservationDTO dto) {
        Book book = bookRepository.findById(dto.getBookId()).orElse(null);
        User user = userRepository.findById(dto.getUserId()).orElse(null);

        if (book == null || user == null) {
            return ValidationResult.fail("图书或用户不存在");
        }

        if (!book.getCanBorrow()) {
            return ValidationResult.fail("该图书不允许借阅");
        }

        if (book.getOwner().getId().equals(dto.getUserId())) {
            return ValidationResult.fail("不能预约自己的图书");
        }

        boolean hasActiveReservation = reservationRepository.existsByBookIdAndUserIdAndStatusIn(
            dto.getBookId(), dto.getUserId(), ACTIVE_RESERVATION_STATUSES
        );
        if (hasActiveReservation) {
            return ValidationResult.fail("您已预约该图书，请勿重复预约");
        }

        boolean hasActiveBorrow = borrowRecordRepository.existsByBookIdAndBorrowerIdAndStatusIn(
            dto.getBookId(), dto.getUserId(), BORROW_ACTIVE_STATUSES
        );
        if (hasActiveBorrow) {
            return ValidationResult.fail("您已借阅或正在申请借阅该图书");
        }

        return ValidationResult.success();
    }

    @Transactional(rollbackFor = Exception.class)
    public Reservation createReservation(ReservationDTO dto) {
        String lockKey = RESERVATION_LOCK_KEY + dto.getBookId() + ":" + dto.getUserId();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            return null;
        }

        try {
            ValidationResult validation = validateCreateReservation(dto);
            if (!validation.isValid()) {
                return null;
            }

            Book book = bookRepository.findById(dto.getBookId()).orElse(null);
            User user = userRepository.findById(dto.getUserId()).orElse(null);

            Reservation reservation = new Reservation();
            reservation.setBook(book);
            reservation.setUser(user);
            reservation.setOwner(book.getOwner());
            reservation.setStatus(Reservation.STATUS_WAITING);

            Reservation saved = reservationRepository.save(reservation);

            int queuePosition = getReservationPosition(saved.getId());
            notificationService.notifyReservationCreated(saved, queuePosition);

            return saved;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Reservation cancelReservation(Long reservationId, Long userId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null) {
            return null;
        }
        if (!reservation.getUser().getId().equals(userId)) {
            return null;
        }
        if (!Reservation.STATUS_WAITING.equals(reservation.getStatus())
            && !Reservation.STATUS_NOTIFIED.equals(reservation.getStatus())) {
            return null;
        }

        boolean wasNotified = Reservation.STATUS_NOTIFIED.equals(reservation.getStatus());
        reservation.setStatus(Reservation.STATUS_CANCELLED);
        Reservation saved = reservationRepository.save(reservation);

        notificationService.notifyReservationCancelled(saved);

        if (wasNotified) {
            boolean hasMoreWaiters = notifyNextWaiter(saved.getBook().getId());
            if (!hasMoreWaiters) {
                bookService.updateBookAvailability(saved.getBook().getId(), true);
            }
        }

        return saved;
    }

    public Reservation getReservationById(Long id) {
        return reservationRepository.findById(id).orElse(null);
    }

    public List<Reservation> getReservationsByUserId(Long userId) {
        return reservationRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    public List<Reservation> getReservationsByBookId(Long bookId) {
        return reservationRepository.findByBookIdOrderByCreateTimeAsc(bookId);
    }

    public Page<Reservation> queryReservations(ReservationQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
            queryDTO.getPageNum() - 1,
            queryDTO.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createTime")
        );

        Specification<Reservation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (queryDTO.getUserId() != null) {
                predicates.add(cb.equal(root.get("user").get("id"), queryDTO.getUserId()));
            }
            if (queryDTO.getBookId() != null) {
                predicates.add(cb.equal(root.get("book").get("id"), queryDTO.getBookId()));
            }
            if (StringUtils.hasText(queryDTO.getStatus())) {
                predicates.add(cb.equal(root.get("status"), queryDTO.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return reservationRepository.findAll(spec, pageable);
    }

    public int getReservationPosition(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null) {
            return -1;
        }
        if (Reservation.STATUS_CANCELLED.equals(reservation.getStatus())
            || Reservation.STATUS_EXPIRED.equals(reservation.getStatus())
            || Reservation.STATUS_COMPLETED.equals(reservation.getStatus())) {
            return -1;
        }
        long position = reservationRepository.countPositionInQueue(
            reservation.getBook().getId(), reservationId
        );
        return (int) position + 1;
    }

    public ReservationPositionDTO getReservationPositionInfo(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null) {
            return null;
        }

        int totalQueue = (int) reservationRepository.countByBookIdAndStatusIn(
            reservation.getBook().getId(), ACTIVE_RESERVATION_STATUSES
        );

        ReservationPositionDTO dto = new ReservationPositionDTO();
        dto.setReservationId(reservationId);
        dto.setStatus(reservation.getStatus());
        dto.setExpireTime(reservation.getExpireTime());
        dto.setTotalQueue(totalQueue);

        if (Reservation.STATUS_CANCELLED.equals(reservation.getStatus())
            || Reservation.STATUS_EXPIRED.equals(reservation.getStatus())
            || Reservation.STATUS_COMPLETED.equals(reservation.getStatus())) {
            dto.setPosition(-1);
        } else if (Reservation.STATUS_NOTIFIED.equals(reservation.getStatus())) {
            dto.setPosition(1);
        } else {
            long position = reservationRepository.countPositionInQueue(
                reservation.getBook().getId(), reservationId
            );
            dto.setPosition((int) position + 1);
        }

        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean notifyNextWaiter(Long bookId) {
        Optional<Reservation> nextOpt = reservationRepository.findFirstByBookIdAndStatusOrderByCreateTimeAsc(
            bookId, Reservation.STATUS_WAITING
        );

        if (nextOpt.isPresent()) {
            Reservation nextReservation = nextOpt.get();
            int reservationHours = borrowRuleService.getBorrowRule().getReservationHours();
            if (reservationHours <= 0) {
                reservationHours = 24;
            }

            nextReservation.setStatus(Reservation.STATUS_NOTIFIED);
            nextReservation.setNotifyTime(LocalDateTime.now());
            nextReservation.setExpireTime(LocalDateTime.now().plusHours(reservationHours));
            reservationRepository.save(nextReservation);

            notificationService.notifyReservationAvailable(nextReservation, reservationHours);
            log.info("图书 {} 已通知预约用户 {}，锁定时长 {} 小时",
                bookId, nextReservation.getUser().getId(), reservationHours);
            return true;
        }
        return false;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean confirmBorrowReservation(Long reservationId, Long userId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null) {
            return false;
        }
        if (!reservation.getUser().getId().equals(userId)) {
            return false;
        }
        if (!Reservation.STATUS_NOTIFIED.equals(reservation.getStatus())) {
            return false;
        }
        if (reservation.getExpireTime() != null && LocalDateTime.now().isAfter(reservation.getExpireTime())) {
            return false;
        }

        reservation.setStatus(Reservation.STATUS_CONFIRMED);
        reservationRepository.save(reservation);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public void linkBorrowRecord(Long reservationId, Long borrowRecordId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation != null) {
            reservation.setBorrowRecordId(borrowRecordId);
            reservation.setStatus(Reservation.STATUS_COMPLETED);
            reservationRepository.save(reservation);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void processExpiredReservations() {
        log.info("开始处理超时的预约记录...");
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredReservations = reservationRepository.findExpiredNotifiedReservations(now);
        int count = 0;

        for (Reservation reservation : expiredReservations) {
            reservation.setStatus(Reservation.STATUS_EXPIRED);
            reservationRepository.save(reservation);

            notificationService.notifyReservationExpired(reservation);

            boolean hasMoreWaiters = notifyNextWaiter(reservation.getBook().getId());
            if (!hasMoreWaiters) {
                bookService.updateBookAvailability(reservation.getBook().getId(), true);
            }
            count++;
            log.info("预约 {} 已超时，顺延给下一位预约者", reservation.getId());
        }

        log.info("超时预约处理完成，共处理 {} 条记录", count);
    }

    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void scheduledProcessExpiredReservations() {
        processExpiredReservations();
    }

    public Reservation getActiveReservationForUserAndBook(Long userId, Long bookId) {
        List<Reservation> reservations = reservationRepository.findByBookIdAndStatusInOrderByCreateTimeAsc(
            bookId, ACTIVE_RESERVATION_STATUSES
        );
        for (Reservation r : reservations) {
            if (r.getUser().getId().equals(userId)) {
                return r;
            }
        }
        return null;
    }
}
