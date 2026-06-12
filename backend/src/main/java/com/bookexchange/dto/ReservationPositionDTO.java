package com.bookexchange.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationPositionDTO {

    private Long reservationId;
    private Integer position;
    private Integer totalQueue;
    private String status;
    private java.time.LocalDateTime expireTime;
}
