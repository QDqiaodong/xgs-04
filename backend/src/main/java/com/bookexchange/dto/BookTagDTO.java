package com.bookexchange.dto;

import lombok.Data;

import java.util.List;

@Data
public class BookTagDTO {

    private List<Long> bookIds;
    private List<Long> tagIds;
}
