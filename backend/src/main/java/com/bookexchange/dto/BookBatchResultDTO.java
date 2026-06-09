package com.bookexchange.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookBatchResultDTO {

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private List<Long> successIds = new ArrayList<>();

    private List<BookBatchFailDetail> failDetails = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookBatchFailDetail {
        private Long bookId;
        private String bookTitle;
        private String reason;
    }
}
