package com.apnabazaar.controller;
import com.apnabazaar.service.ExcelIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/{communitySlug}/ingest")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IngestionController {
    private final ExcelIngestionService ingestionService;

    @PostMapping("/daily-menu")
    public ResponseEntity<?> uploadDailyMenu(@PathVariable String communitySlug,
                                              @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(ingestionService.ingestDailyMenu(file, communitySlug));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
