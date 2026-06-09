package com.bookexchange.dto;

import lombok.Data;

import java.util.List;

@Data
public class BookDTO {

    private String title;
    private String author;
    private String isbn;
    private Long categoryId;
    private String conditionLevel;
    private String description;
    private Boolean canBorrow;
    private Long ownerId;
    private Long cityId;
    private List<Long> tagIds;
}
