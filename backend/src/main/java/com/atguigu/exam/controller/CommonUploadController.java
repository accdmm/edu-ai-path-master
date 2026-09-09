package com.atguigu.exam.controller;

import com.atguigu.exam.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 通用上传接口（本地磁盘降级方案，MinIO 不可用时可用）
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@CrossOrigin
@Tag(name = "通用上传")
public class CommonUploadController {

    @Value("${file.upload.path:./uploads/}")
    private String localUploadPath;

    @Operation(summary = "上传文件到本地")
    @PostMapping
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "folder", required = false) String folder) {
        String safeFolder = (folder == null || folder.isBlank() || folder.contains("..")) ? "common" : folder;
        try {
            String original = file.getOriginalFilename();
            String ext = original != null && original.contains(".")
                    ? original.substring(original.lastIndexOf(".")) : "";
            String filename = UUID.randomUUID().toString().replaceAll("-", "") + ext;
            String relative = safeFolder + "/" + filename;

            File dir = new File(localUploadPath, safeFolder);
            if (!dir.exists() && !dir.mkdirs()) {
                return Result.error("创建上传目录失败");
            }
            File target = new File(dir, filename);
            file.transferTo(target);

            Map<String, Object> data = new HashMap<>();
            data.put("url", "/files/" + relative);
            data.put("size", file.getSize());
            return Result.success(data, "上传成功");
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败");
        }
    }
}