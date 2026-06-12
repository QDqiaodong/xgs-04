package com.bookexchange.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StatisticsQueryDTO {

    private LocalDate startDate;

    private LocalDate endDate;

    private String dimension;

    private Integer topN = 10;
}
