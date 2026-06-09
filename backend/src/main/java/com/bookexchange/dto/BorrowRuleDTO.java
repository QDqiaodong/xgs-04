package com.bookexchange.dto;

import lombok.Data;

@Data
public class BorrowRuleDTO {

    private Integer maxBorrowCount;
    private Integer maxBorrowDays;
    private Integer reservationHours;
    private Boolean allowRenew;
    private Integer maxRenewCount;
    private String description;
}
