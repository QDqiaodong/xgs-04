package com.bookexchange.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BorrowRecordQueryDTO {

    private Long currentUserId;

    private Long bookId;

    private List<String> statuses;

    private LocalDate startDateFrom;

    private LocalDate startDateTo;

    private Long categoryId;

    private Long borrowerId;

    private Long ownerId;

    private String borrowerKeyword;

    private String ownerKeyword;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
