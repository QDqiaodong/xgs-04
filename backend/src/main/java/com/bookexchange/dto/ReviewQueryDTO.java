package com.bookexchange.dto;

import lombok.Data;

@Data
public class ReviewQueryDTO {

    private Long bookId;
    private Long userId;
    private String sortBy;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
