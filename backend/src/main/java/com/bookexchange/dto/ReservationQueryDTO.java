package com.bookexchange.dto;

import lombok.Data;

@Data
public class ReservationQueryDTO {

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Long userId;
    private Long bookId;
    private String status;
}
