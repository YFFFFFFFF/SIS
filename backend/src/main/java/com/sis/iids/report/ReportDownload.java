package com.sis.iids.report;

/**
 * 报告下载载荷。fileType 为 ReportDocument.fileType（EXCEL/PDF），供控制器决定 Content-Type。
 */
public record ReportDownload(String fileName, byte[] content, String fileType) {
}
