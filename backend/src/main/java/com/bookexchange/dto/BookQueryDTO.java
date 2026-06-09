package com.bookexchange.dto;

import lombok.Data;

import java.util.List;

@Data
public class BookQueryDTO {

    private Long cityId;
    private Long categoryId;
    private Boolean available;
    private String keyword;
    private List<Long> tagIds;
    private Boolean matchAllTags = false;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
