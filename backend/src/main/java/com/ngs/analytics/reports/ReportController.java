package com.ngs.analytics.reports;

import com.ngs.analytics.auth.SecurityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/{sampleId}")
    public Map<String, Object> json(@PathVariable UUID sampleId) {
        return reportService.buildJson(sampleId, SecurityUtils.currentUser());
    }

    @GetMapping(value = "/{sampleId}/csv", produces = "text/csv")
    public ResponseEntity<byte[]> csv(@PathVariable UUID sampleId) {
        byte[] body = reportService.buildCsv(sampleId, SecurityUtils.currentUser());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + sampleId + ".csv\"")
                .body(body);
    }

    @GetMapping(value = "/{sampleId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable UUID sampleId) {
        byte[] body = reportService.buildPdf(sampleId, SecurityUtils.currentUser());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + sampleId + ".pdf\"")
                .body(body);
    }
}
