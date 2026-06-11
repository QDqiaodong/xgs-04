package com.bookexchange.dto.export;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BorrowRecordExportRequestDTO {

    private List<String> fields;

    private Long currentUserId;
    private List<String> statuses;
    private LocalDate startDateFrom;
    private LocalDate startDateTo;
    private Long categoryId;
    private Long borrowerId;
    private Long ownerId;
    private String borrowerKeyword;
    private String ownerKeyword;
}
