package com.company.platform.file;

import com.company.platform.commons.dto.FileMetadataDto;
import com.company.platform.commons.exception.ApiExceptions;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
class FileController {
    private static final String DEFAULT_FILE_NAME = "README.md";
    private static final String OLD_SAMPLE_FILE_NAME = "welcome-platform-guide.txt";
    private static final UUID FALLBACK_USER_ID = UUID.fromString("6dc557b4-32d2-4b5e-8b7c-357a88b9de14");
    private static final String FALLBACK_README_CONTENT = "# Microservice Industry\n\nProject README was not available at runtime.\n";
    private static final Map<String, String> CONTENT_TYPES_BY_EXTENSION = Map.ofEntries(
            Map.entry("txt", MediaType.TEXT_PLAIN_VALUE),
            Map.entry("text", MediaType.TEXT_PLAIN_VALUE),
            Map.entry("md", "text/markdown; charset=UTF-8"),
            Map.entry("markdown", "text/markdown; charset=UTF-8"),
            Map.entry("pdf", MediaType.APPLICATION_PDF_VALUE),
            Map.entry("png", MediaType.IMAGE_PNG_VALUE),
            Map.entry("jpg", MediaType.IMAGE_JPEG_VALUE),
            Map.entry("jpeg", MediaType.IMAGE_JPEG_VALUE),
            Map.entry("gif", MediaType.IMAGE_GIF_VALUE),
            Map.entry("webp", "image/webp"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("ico", "image/x-icon"),
            Map.entry("csv", "text/csv; charset=UTF-8"),
            Map.entry("json", MediaType.APPLICATION_JSON_VALUE),
            Map.entry("xml", MediaType.APPLICATION_XML_VALUE),
            Map.entry("html", MediaType.TEXT_HTML_VALUE),
            Map.entry("htm", MediaType.TEXT_HTML_VALUE),
            Map.entry("css", "text/css"),
            Map.entry("js", "text/javascript"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("webm", "video/webm"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("zip", "application/zip"),
            Map.entry("7z", "application/x-7z-compressed"),
            Map.entry("rar", "application/vnd.rar"),
            Map.entry("tar", "application/x-tar"),
            Map.entry("gz", "application/gzip"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation")
    );

    private final FileMetadataRepository repository;
    @Value("${file.download-base-url:http://localhost:8080}")
    private String downloadBaseUrl;

    @PostMapping("/upload")
    FileMetadataDto upload(@RequestHeader(value = "X-User-Id", required = false) UUID userId,
                           @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        if (file == null) {
            throw new ApiExceptions.ValidationException("Choose a file to upload");
        }
        FileMetadata metadata = new FileMetadata();
        metadata.setUserId(resolveUserId(userId));
        metadata.setOriginalName(resolveOriginalName(file));
        metadata.setContentType(resolveContentType(metadata.getOriginalName(), file.getContentType()));
        metadata.setSizeBytes(file.getSize());
        metadata.setPublic(false);
        metadata.setContent(Base64.getEncoder().encodeToString(file.getBytes()));
        return toDto(repository.save(metadata));
    }

    @GetMapping("/{id}/download-url")
    Map<String, String> downloadUrl(@PathVariable UUID id) {
        FileMetadata metadata = repository.findById(id)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("File not found"));
        return Map.of(
                "url", downloadUrlFor(metadata.getId()),
                "filename", metadata.getOriginalName(),
                "contentType", metadata.getContentType()
        );
    }

    @GetMapping("/{id}/download")
    ResponseEntity<byte[]> download(@PathVariable UUID id,
                                    @RequestParam(defaultValue = "attachment") String disposition) {
        FileMetadata metadata = repository.findById(id)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("File not found"));
        byte[] content = decodeContent(metadata.getContent());
        String responseDisposition = "inline".equalsIgnoreCase(disposition) ? "inline" : "attachment";

        return ResponseEntity.ok()
                .contentType(resolveMediaType(metadata.getContentType()))
                .contentLength(content.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.builder(responseDisposition)
                        .filename(metadata.getOriginalName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(content);
    }

    @GetMapping("/{id}/metadata")
    FileMetadataDto metadata(@PathVariable UUID id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("File not found"));
    }

    @GetMapping("/my-files")
    @Transactional
    List<FileMetadataDto> myFiles(@RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID ownerId = resolveUserId(userId);
        seedDefaultReadme(ownerId);
        return repository.findByUserIdOrderByCreatedAtDesc(ownerId).stream()
                .map(this::toDto)
                .toList();
    }

    @DeleteMapping("/{id}")
    void delete(@PathVariable UUID id) {
        repository.deleteById(id);
    }

    private void seedDefaultReadme(UUID userId) {
        repository.deleteByUserIdAndOriginalName(userId, OLD_SAMPLE_FILE_NAME);
        if (repository.existsByUserIdAndOriginalName(userId, DEFAULT_FILE_NAME)) {
            return;
        }
        byte[] content = readDefaultReadme();
        FileMetadata readme = new FileMetadata();
        readme.setUserId(userId);
        readme.setOriginalName(DEFAULT_FILE_NAME);
        readme.setContentType(resolveContentType(DEFAULT_FILE_NAME, null));
        readme.setContent(Base64.getEncoder().encodeToString(content));
        readme.setSizeBytes(content.length);
        readme.setPublic(false);
        repository.save(readme);
    }

    private byte[] readDefaultReadme() {
        try {
            Path readme = Path.of(DEFAULT_FILE_NAME);
            if (Files.isRegularFile(readme)) {
                return Files.readAllBytes(readme);
            }
        } catch (IOException ignored) {
        }
        return FALLBACK_README_CONTENT.getBytes(StandardCharsets.UTF_8);
    }

    private UUID resolveUserId(UUID userId) {
        return userId == null ? FALLBACK_USER_ID : userId;
    }

    private String downloadUrlFor(UUID id) {
        String baseUrl = downloadBaseUrl == null || downloadBaseUrl.isBlank()
                ? "http://localhost:8080"
                : downloadBaseUrl.trim();
        return StringUtils.trimTrailingCharacter(baseUrl, '/') + "/api/v1/files/" + id + "/download";
    }

    private String resolveOriginalName(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            return "uploaded-file.bin";
        }
        String cleanName = StringUtils.cleanPath(originalName);
        String normalizedName = cleanName.replace("\\", "/");
        int separator = normalizedName.lastIndexOf('/');
        String fileName = separator >= 0 ? normalizedName.substring(separator + 1) : normalizedName;
        return fileName.isBlank() ? "uploaded-file.bin" : fileName;
    }

    private String resolveContentType(String filename, String suppliedContentType) {
        String extensionContentType = contentTypeFromExtension(filename);
        if (extensionContentType != null) {
            return extensionContentType;
        }
        if (suppliedContentType != null && !suppliedContentType.isBlank()) {
            return suppliedContentType;
        }
        String guessedContentType = URLConnection.guessContentTypeFromName(filename);
        return guessedContentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : guessedContentType;
    }

    private String contentTypeFromExtension(String filename) {
        String extension = extension(filename);
        return extension == null ? null : CONTENT_TYPES_BY_EXTENSION.get(extension);
    }

    private String extension(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private byte[] decodeContent(String content) {
        if (content == null || content.isBlank()) {
            return new byte[0];
        }
        String payload = content.strip();
        boolean dataUrl = payload.startsWith("data:");
        int dataUrlSeparator = payload.indexOf(',');
        if (dataUrl && dataUrlSeparator >= 0) {
            payload = payload.substring(dataUrlSeparator + 1);
        }
        if (dataUrl || looksLikeBase64(payload)) {
            try {
                return Base64.getDecoder().decode(payload);
            } catch (IllegalArgumentException ignored) {
                return payload.getBytes(StandardCharsets.UTF_8);
            }
        }
        return payload.getBytes(StandardCharsets.UTF_8);
    }

    private boolean looksLikeBase64(String payload) {
        return payload.length() % 4 == 0 && payload.matches("[A-Za-z0-9+/]+={0,2}");
    }

    private FileMetadataDto toDto(FileMetadata metadata) {
        return new FileMetadataDto(
                metadata.getId(),
                metadata.getUserId(),
                metadata.getOriginalName(),
                metadata.getContentType(),
                metadata.getSizeBytes(),
                metadata.isPublic(),
                metadata.getCreatedAt()
        );
    }
}
