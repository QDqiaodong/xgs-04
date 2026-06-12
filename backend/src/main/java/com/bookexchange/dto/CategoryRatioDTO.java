package com.bookexchange.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRatioDTO {

    private Long categoryId;

    private String categoryName;

    private Long borrowCount;

    private BigDecimal ratio;
}
