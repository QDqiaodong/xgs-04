package com.bookexchange.dto;

import lombok.Data;

import java.util.List;

@Data
public class BookBatchOperationDTO {

    private String operation;

    private List<Long> bookIds;

    private Long categoryId;

    private Boolean canBorrow;

    private Boolean available;
}
