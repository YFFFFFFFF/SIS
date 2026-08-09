package com.sis.iids.report;

import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;

import java.util.Locale;

/**
 * 报告导出格式（FR-01-06：Excel + PDF）。
 */
public enum ReportFormat {
    EXCEL("xlsx"),
    PDF("pdf");

    private final String extension;

    ReportFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    public static ReportFormat from(String format) {
        if (format == null || format.isBlank()) {
            return EXCEL;
        }
        try {
            return ReportFormat.valueOf(format.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的报告格式：" + format + "（支持 EXCEL/PDF）");
        }
    }
}
