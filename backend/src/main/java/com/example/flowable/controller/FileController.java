package com.example.flowable.controller;

import com.example.flowable.common.Result;
import com.example.flowable.dto.FileVO;
import com.example.flowable.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "上传文件")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileStorageService.upload(file));
    }

    @Operation(summary = "下载/预览文件")
    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        FileStorageService.StoredFile stored = fileStorageService.load(id);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (stored.metadata().getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(stored.metadata().getContentType());
            } catch (Exception ignored) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        String disposition = "image".equalsIgnoreCase(mediaType.getType()) ? "inline" : "attachment";
        String filename = URLEncoder.encode(stored.metadata().getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename*=UTF-8''" + filename);
        if (stored.metadata().getFileSize() != null) {
            builder.contentLength(stored.metadata().getFileSize());
        }
        return builder.body(stored.resource());
    }
}
