package com.flashflow.auth.controller;

import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传控制器（主要用于商品图片上传）
 * 安全措施：魔数校验 + 扩展名白名单 + 文件大小限制
 */
@Slf4j
@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/flashflow/auth/upload")
public class UploadController {

    @Value("${upload.dir:flashflow-frontend/public/assets/products}")
    private String uploadDir;

    @Value("${upload.url-prefix:/assets/products}")
    private String urlPrefix;

    /** 允许的文件扩展名白名单 */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp"));
    /** 最大文件大小 10MB */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /** 常见图片格式的魔数（文件头字节） */
    private static final byte[][] IMAGE_MAGIC_BYTES = {
        {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},         // JPEG
        {(byte) 0x89, 0x50, 0x4E, 0x47},                   // PNG
        {(byte) 0x47, 0x49, 0x46, 0x38},                   // GIF
        {(byte) 0x52, 0x49, 0x46, 0x46},                   // WEBP (RIFF)
    };

    @Operation(summary = "上传图片")
    @PostMapping
    public R<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail(ErrorCode.PARAM_ERROR);
        }

        // 1. 文件大小校验
        if (file.getSize() > MAX_FILE_SIZE) {
            return R.fail(ErrorCode.PARAM_ERROR, "文件大小不能超过 10MB");
        }

        // 2. 扩展名校验
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return R.fail(ErrorCode.PARAM_ERROR, "文件名不能为空");
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return R.fail(ErrorCode.PARAM_ERROR, "不支持的文件类型");
        }
        String ext = filename.substring(dotIndex).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return R.fail(ErrorCode.PARAM_ERROR, "不支持的文件类型: " + ext);
        }

        // 3. 魔数校验（文件头字节验证真实类型）
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4 || !isValidImageMagic(header)) {
                return R.fail(ErrorCode.PARAM_ERROR, "文件内容与扩展名不匹配");
            }
        } catch (IOException e) {
            return R.fail(ErrorCode.PARAM_ERROR, "无法读取文件内容");
        }

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String newFilename = UUID.randomUUID().toString().replace("-", "").substring(0, 12) + ext;
            Path filePath = uploadPath.resolve(newFilename);
            file.transferTo(filePath.toFile());

            String url = urlPrefix + "/" + newFilename;
            log.info("图片上传成功: {} -> {}", filename, url);
            return R.ok(url);
        } catch (IOException e) {
            log.error("图片上传失败: ", e);
            return R.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    /** 校验文件头是否匹配已知图片格式的魔数 */
    private boolean isValidImageMagic(byte[] header) {
        for (byte[] magic : IMAGE_MAGIC_BYTES) {
            if (header.length >= magic.length) {
                boolean match = true;
                for (int i = 0; i < magic.length; i++) {
                    if (header[i] != magic[i]) { match = false; break; }
                }
                if (match) return true;
            }
        }
        return false;
    }
}
