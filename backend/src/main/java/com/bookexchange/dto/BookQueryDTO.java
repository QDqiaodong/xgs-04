package com.bookexchange.dto;

import lombok.Data;

@Data
public class BookQueryDTO {

    private Long cityId;
    private Long categoryId;
    private Boolean available;
    private String keyword;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
