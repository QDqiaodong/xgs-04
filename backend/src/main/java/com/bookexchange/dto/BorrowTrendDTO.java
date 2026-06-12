package com.bookexchange.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowTrendDTO {

    private String period;

    private Long borrowCount;

    private Long returnCount;

    private Long totalCount;
}
