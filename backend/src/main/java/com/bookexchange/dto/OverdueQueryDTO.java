package com.bookexchange.dto;

import lombok.Data;

@Data
public class OverdueQueryDTO {

    private Long borrowerId;
    private Long ownerId;
    private Long bookId;
    private String borrowerKeyword;
    private String ownerKeyword;
    private String bookTitleKeyword;
    private String sortBy;
    private String sortOrder;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
