package com.bookexchange.dto;

import lombok.Data;

@Data
public class ReservationDTO {

    private Long bookId;
    private Long userId;
    private String remark;
}
