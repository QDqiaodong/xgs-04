package com.bookexchange.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookRankDTO {

    private Long bookId;

    private String bookTitle;

    private String author;

    private String isbn;

    private String categoryName;

    private Long borrowCount;

    private Integer rank;
}
