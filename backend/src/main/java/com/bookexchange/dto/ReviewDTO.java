package com.bookexchange.dto;

import lombok.Data;

@Data
public class ReviewDTO {

    private Long bookId;
    private Long userId;
    private Long borrowRecordId;
    private Integer rating;
    private String content;
}
