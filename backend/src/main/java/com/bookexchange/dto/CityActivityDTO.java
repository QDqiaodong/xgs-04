package com.bookexchange.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CityActivityDTO {

    private Long cityId;

    private String province;

    private String cityName;

    private Long borrowCount;

    private Long lendCount;

    private Long totalActivity;

    private Integer rank;
}
