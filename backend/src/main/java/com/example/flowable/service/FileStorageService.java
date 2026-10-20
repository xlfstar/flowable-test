package com.example.flowable.service;

import com.example.flowable.common.BizException;
import com.example.flowable.dto.FileVO;
import com.example.flowable.entity.SysFile;
import com.example.flowable.mapper.SysFileMapper;
import com.example.flowable.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024L;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "zip", "rar", "7z", "txt", "csv"
    );

    private final SysFileMapper sysFileMapper;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public FileVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException("文件不能超过 20MB");
        }
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        originalName = Paths.get(originalName).getFileName().toString();
        if (!StringUtils.hasText(originalName) || originalName.contains("..")) {
            throw new BizException("文件名不合法");
        }
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BizException("不支持的文件类型: " + extension);
        }

        String storedName = UUID.randomUUID() + "." + extension;
        Path root = uploadRoot();
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new BizException("文件路径不合法");
        }
        try {
            Files.createDirectories(root);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new BizException("文件保存失败: " + e.getMessage());
        }

        SysFile entity = new SysFile();
        entity.setOriginalName(originalName);
        entity.setStoredName(storedName);
        entity.setContentType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setStoragePath(target.toString());
        entity.setUploaderId(SecurityUtils.getUserId());
        entity.setUploaderName(SecurityUtils.getRealName());
        entity.setCreateTime(LocalDateTime.now());
        sysFileMapper.insert(entity);

        return toVO(entity);
    }

    public StoredFile load(Long id) {
        SysFile entity = sysFileMapper.selectById(id);
        if (entity == null) {
            throw new BizException("文件不存在");
        }
        Path root = uploadRoot();
        Path target = root.resolve(entity.getStoredName()).normalize();
        if (!target.startsWith(root)) {
            throw new BizException("文件路径不合法");
        }
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new BizException("文件已丢失");
        }
        try {
            Resource resource = new UrlResource(target.toUri());
            return new StoredFile(entity, resource);
        } catch (MalformedURLException e) {
            throw new BizException("文件读取失败");
        }
    }

    private Path uploadRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private String extensionOf(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private FileVO toVO(SysFile entity) {
        return FileVO.builder()
                .id(entity.getId())
                .name(entity.getOriginalName())
                .url("/api/files/" + entity.getId())
                .size(entity.getFileSize())
                .contentType(entity.getContentType())
                .build();
    }

    public record StoredFile(SysFile metadata, Resource resource) {
    }
}
