package com.sis.iids.report;

import java.nio.file.Path;
import java.time.LocalDateTime;

public record ReportDocumentResponse(
        Long id,
        Long scenarioId,
        Long taskId,
        String title,
        String fileName,
        String fileType,
        ReportDocumentStatus status,
        LocalDateTime createdAt
) {
    public static ReportDocumentResponse from(ReportDocument document) {
        String fileName = Path.of(document.getFilePath()).getFileName().toString();
        return new ReportDocumentResponse(
                document.getId(),
                document.getScenarioId(),
                document.getTaskId(),
                document.getTitle(),
                fileName,
                document.getFileType(),
                document.getStatus(),
                document.getCreatedAt());
    }
}