package com.company.platform.file.controller;

import com.company.platform.commons.dto.DemoUserRequestDto;
import com.company.platform.commons.exception.ApiExceptions;
import com.company.platform.file.model.FileMetadata;
import com.company.platform.file.repository.FileMetadataRepository;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

class FileControllerTest {
    @Mock
    private FileMetadataRepository repository;

    private FileController controller;

    @BeforeMethod
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new FileController(repository);
        ReflectionTestUtils.setField(controller, "downloadBaseUrl", "http://localhost:8080/");
        given(repository.save(any(FileMetadata.class))).willAnswer(invocation -> {
            FileMetadata metadata = invocation.getArgument(0);
            if (metadata.getId() == null) {
                metadata.setId(UUID.randomUUID());
            }
            if (metadata.getCreatedAt() == null) {
                metadata.setCreatedAt(LocalDateTime.now());
            }
            return metadata;
        });
    }

    @Test
    void uploadRejectsMissingFile() {
        ApiExceptions.ValidationException exception = expectThrows(
                ApiExceptions.ValidationException.class,
                () -> controller.upload(UUID.randomUUID(), null)
        );
        assertTrue(exception.getMessage().contains("Choose a file"));
    }

    @Test
    void uploadNormalizesFilenameAndPrefersKnownExtensionContentType() throws Exception {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "C:/temp/report.pdf",
                MediaType.TEXT_PLAIN_VALUE, "pdf".getBytes(StandardCharsets.UTF_8));

        var dto = controller.upload(userId, file);

        assertEquals(dto.userId(), userId);
        assertEquals(dto.originalName(), "report.pdf");
        assertEquals(dto.contentType(), MediaType.APPLICATION_PDF_VALUE);
        assertEquals(dto.sizeBytes(), 3);
    }

    @Test
    void downloadUrlAndMetadataReadFromRepository() {
        UUID id = UUID.randomUUID();
        FileMetadata metadata = metadata(id, UUID.randomUUID(), "readme.md", "text/markdown; charset=UTF-8", "# Hi");
        given(repository.findById(id)).willReturn(Optional.of(metadata));

        assertEquals(controller.downloadUrl(id).get("url"), "http://localhost:8080/api/v1/files/" + id + "/download");
        assertEquals(controller.downloadUrl(id).get("filename"), "readme.md");
        assertEquals(controller.metadata(id).contentType(), "text/markdown; charset=UTF-8");
    }

    @Test
    void downloadDecodesBase64DataUrlsAndUsesInlineDisposition() {
        UUID id = UUID.randomUUID();
        String encoded = "data:text/plain;base64," + Base64.getEncoder().encodeToString("hello".getBytes(StandardCharsets.UTF_8));
        FileMetadata metadata = metadata(id, UUID.randomUUID(), "hello.txt", MediaType.TEXT_PLAIN_VALUE, encoded);
        given(repository.findById(id)).willReturn(Optional.of(metadata));

        var response = controller.download(id, "inline");

        assertEquals(response.getHeaders().getContentType(), MediaType.TEXT_PLAIN);
        assertTrue(String.valueOf(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains(String.valueOf("inline")));
        assertEquals(response.getBody(), "hello".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void downloadFallsBackForPlainOrInvalidContentAndMissingContentType() {
        UUID id = UUID.randomUUID();
        FileMetadata metadata = metadata(id, UUID.randomUUID(), "blob.bin", "", "not-base64***");
        given(repository.findById(id)).willReturn(Optional.of(metadata));

        var response = controller.download(id, "attachment");

        assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_OCTET_STREAM);
        assertEquals(new String(response.getBody(), StandardCharsets.UTF_8), "not-base64***");
    }

    @Test
    void missingFilesThrowNotFound() {
        UUID id = UUID.randomUUID();
        given(repository.findById(id)).willReturn(Optional.empty());

        expectThrows(ApiExceptions.ResourceNotFoundException.class, () -> controller.downloadUrl(id));
        expectThrows(ApiExceptions.ResourceNotFoundException.class, () -> controller.metadata(id));
        expectThrows(ApiExceptions.ResourceNotFoundException.class, () -> controller.download(id, "attachment"));
    }

    @Test
    void myFilesSeedsDefaultReadmeForResolvedUser() {
        UUID userId = UUID.randomUUID();
        given(repository.existsByUserIdAndOriginalName(userId, "README.md")).willReturn(false);
        given(repository.findByUserIdOrderByCreatedAtDesc(userId)).willReturn(List.of());

        assertTrue(controller.myFiles(userId).isEmpty());

        verify(repository).deleteByUserIdAndOriginalName(userId, "welcome-platform-guide.txt");
        verify(repository).save(any(FileMetadata.class));
    }

    @Test
    void seedDemoDataCreatesMissingFilesAndSkipsExistingOnes() {
        UUID userId = UUID.randomUUID();
        given(repository.existsByUserIdAndOriginalName(userId, "README.md")).willReturn(true);
        given(repository.findByUserIdOrderByCreatedAtDesc(userId)).willReturn(List.of());

        assertTrue(controller.seedDemoData(new DemoUserRequestDto(userId, "u@example.com", "User", "user", null)).isEmpty());

        ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
        verify(repository, org.mockito.Mockito.times(4)).save(captor.capture());
        List<String> names = captor.getAllValues().stream().map(FileMetadata::getOriginalName).toList();
        assertTrue(names.contains("audit-log-sample.json"));
        assertTrue(names.contains("prometheus-targets.csv"));
        assertTrue(names.contains("grafana-dashboard-notes.md"));
        assertTrue(names.contains("payment-receipt-demo.txt"));
    }

    @Test
    void deleteDelegatesToRepository() {
        UUID id = UUID.randomUUID();

        controller.delete(id);

        verify(repository).deleteById(id);
    }

    private FileMetadata metadata(UUID id, UUID userId, String name, String contentType, String content) {
        FileMetadata metadata = new FileMetadata();
        metadata.setId(id);
        metadata.setUserId(userId);
        metadata.setOriginalName(name);
        metadata.setContentType(contentType);
        metadata.setSizeBytes(content == null ? 0 : content.length());
        metadata.setContent(content);
        metadata.setCreatedAt(LocalDateTime.now());
        return metadata;
    }
}
