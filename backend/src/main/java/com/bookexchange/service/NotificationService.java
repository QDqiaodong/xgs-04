package com.bookexchange.service;

import com.bookexchange.dto.NotificationQueryDTO;
import com.bookexchange.entity.BorrowRecord;
import com.bookexchange.entity.Notification;
import com.bookexchange.repository.BorrowRecordRepository;
import com.bookexchange.repository.NotificationRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    public static final String TYPE_BORROW_REQUEST = "BORROW_REQUEST";
    public static final String TYPE_APPROVAL_RESULT = "APPROVAL_RESULT";
    public static final String TYPE_DUE_REMINDER = "DUE_REMINDER";
    public static final String TYPE_RETURN_CONFIRM = "RETURN_CONFIRM";

    @Transactional(rollbackFor = Exception.class)
    public Notification createNotification(Long userId, String type, String title, String content, Long relatedId, String relatedType) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setRelatedType(relatedType);
        notification.setIsRead(false);
        return notificationRepository.save(notification);
    }

    public Page<Notification> queryNotifications(NotificationQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
            queryDTO.getPageNum() - 1,
            queryDTO.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createTime")
        );

        Specification<Notification> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (queryDTO.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), queryDTO.getUserId()));
            }

            if (StringUtils.hasText(queryDTO.getType())) {
                predicates.add(cb.equal(root.get("type"), queryDTO.getType()));
            }

            if (queryDTO.getIsRead() != null) {
                predicates.add(cb.equal(root.get("isRead"), queryDTO.getIsRead()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return notificationRepository.findAll(spec, pageable);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markAsRead(Long notificationId, Long userId) {
        int updated = notificationRepository.markAsReadByIdAndUserId(notificationId, userId);
        return updated > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsReadByUserId(userId);
    }

    public void notifyBorrowRequest(BorrowRecord record) {
        Long ownerId = record.getOwner().getId();
        String bookTitle = record.getBook().getTitle();
        String borrowerName = record.getBorrower().getNickname();

        createNotification(
            ownerId,
            TYPE_BORROW_REQUEST,
            "收到新的借阅申请",
            borrowerName + " 申请借阅您的图书《" + bookTitle + "》，请及时处理。",
            record.getId(),
            "BORROW_RECORD"
        );
    }

    public void notifyApprovalResult(BorrowRecord record, boolean approved) {
        Long borrowerId = record.getBorrower().getId();
        String bookTitle = record.getBook().getTitle();
        String ownerName = record.getOwner().getNickname();

        if (approved) {
            createNotification(
                borrowerId,
                TYPE_APPROVAL_RESULT,
                "借阅申请已通过",
                ownerName + " 同意了您借阅《" + bookTitle + "》的申请，请尽快确认借出。",
                record.getId(),
                "BORROW_RECORD"
            );
        } else {
            createNotification(
                borrowerId,
                TYPE_APPROVAL_RESULT,
                "借阅申请被拒绝",
                ownerName + " 拒绝了您借阅《" + bookTitle + "》的申请。",
                record.getId(),
                "BORROW_RECORD"
            );
        }
    }

    public void notifyReturnConfirm(BorrowRecord record) {
        Long ownerId = record.getOwner().getId();
        String bookTitle = record.getBook().getTitle();
        String borrowerName = record.getBorrower().getNickname();

        createNotification(
            ownerId,
            TYPE_RETURN_CONFIRM,
            "图书已归还确认",
            borrowerName + " 已归还《" + bookTitle + "》，请确认归还。",
            record.getId(),
            "BORROW_RECORD"
        );

        Long borrowerId = record.getBorrower().getId();
        createNotification(
            borrowerId,
            TYPE_RETURN_CONFIRM,
            "图书归还成功",
            "您已成功归还《" + bookTitle + "》，感谢使用！",
            record.getId(),
            "BORROW_RECORD"
        );
    }

    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void sendDueReminders() {
        log.info("开始执行到期提醒定时任务...");
        LocalDate threeDaysLater = LocalDate.now().plusDays(3);
        LocalDate today = LocalDate.now();

        List<BorrowRecord> records = borrowRecordRepository.findByStatus("BORROWING");
        int count = 0;
        for (BorrowRecord record : records) {
            if (record.getEndDate() == null) continue;

            if (!record.getEndDate().isAfter(threeDaysLater) && !record.getEndDate().isBefore(today)) {
                boolean alreadyNotified = notificationRepository.existsByUserIdAndTypeAndRelatedIdAndIsReadFalse(
                    record.getBorrower().getId(), TYPE_DUE_REMINDER, record.getId()
                );
                if (!alreadyNotified) {
                    String bookTitle = record.getBook().getTitle();
                    long daysLeft = ChronoUnit.DAYS.between(today, record.getEndDate());

                    createNotification(
                        record.getBorrower().getId(),
                        TYPE_DUE_REMINDER,
                        "借阅即将到期提醒",
                        "您借阅的《" + bookTitle + "》将于" + (daysLeft == 0 ? "今天" : daysLeft + "天后") + "到期，请及时归还。",
                        record.getId(),
                        "BORROW_RECORD"
                    );
                    count++;
                }
            }
        }
        log.info("到期提醒定时任务完成，共发送 {} 条提醒", count);
    }
}
