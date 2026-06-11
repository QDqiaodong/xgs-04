package com.bookexchange.dto.export;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportTaskDTO {

    private String taskId;
    private String type;
    private String status;
    private String fileName;
    private String downloadUrl;
    private Long totalCount;
    private Long processedCount;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}
