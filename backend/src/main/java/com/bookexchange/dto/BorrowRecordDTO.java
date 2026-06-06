package com.bookexchange.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BorrowRecordDTO {

    private Long bookId;
    private Long borrowerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String remark;
}
