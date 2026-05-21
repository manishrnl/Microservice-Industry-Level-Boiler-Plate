package com.company.platform.file;

import com.company.platform.commons.dto.FileMetadataDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
class FileController {
    private final String publicBaseUrl;

    FileController(@Value("${file.public-base-url}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    @PostMapping("/upload")
    FileMetadataDto upload() {
        return metadata(UUID.randomUUID());
    }

    @GetMapping("/{id}/download-url")
    Map<String, String> downloadUrl(@PathVariable UUID id) {
        return Map.of("url", publicBaseUrl + "/" + id);
    }

    @GetMapping("/{id}/metadata")
    FileMetadataDto metadata(@PathVariable UUID id) {
        return new FileMetadataDto(id, UUID.randomUUID(), "file.bin", "application/octet-stream", 0, false, LocalDateTime.now());
    }

    @GetMapping("/my-files")
    List<FileMetadataDto> myFiles() {
        return List.of(metadata(UUID.randomUUID()));
    }

    @DeleteMapping("/{id}")
    void delete(@PathVariable UUID id) {
    }
}
