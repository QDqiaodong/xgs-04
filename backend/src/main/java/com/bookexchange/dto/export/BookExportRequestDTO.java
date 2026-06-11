package com.bookexchange.dto.export;

import lombok.Data;

import java.util.List;

@Data
public class BookExportRequestDTO {

    private List<String> fields;

    private Long cityId;
    private Long categoryId;
    private Boolean available;
    private String keyword;
    private List<Long> tagIds;
    private Boolean matchAllTags = false;
    private Long ownerId;
}
