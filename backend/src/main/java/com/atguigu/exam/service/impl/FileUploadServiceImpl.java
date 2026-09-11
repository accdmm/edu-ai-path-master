package com.atguigu.exam.service.impl;

import com.atguigu.exam.config.properties.MinioProperties;
import com.atguigu.exam.service.FileUploadService;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * projectName: com.atguigu.exam.service.impl
 *
 * @author: 赵伟风
 * description:
 */
@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private MinioClient minioClient;
    @Autowired
    private MinioProperties minioProperties;

    @Override
    public String uploadFile(String folder, MultipartFile file) throws Exception {
        //1. 判断桶是否存在
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
        //2. 不存在，创建桶，同时设置访问权限
        if (!bucketExists) {
            //创建桶
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
            String config = """
                        {
                              "Statement" : [ {
                                "Action" : "s3:GetObject",
                                "Effect" : "Allow",
                                "Principal" : "*",
                                "Resource" : "arn:aws:s3:::%s/*"
                              } ],
                              "Version" : "2012-10-17"
                        }
                    """.formatted(minioProperties.getBucketName());
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .config(config)
                    .build());
        }
        //3. 处理上传的对象名（影响，minio桶中的文件结构！）
        //现在： 桶名 / folder / ai.png  缺点： 所有文件都平铺（banner，video）不好区分！ 核心缺点，可能覆盖！
        //小知识点： x/x/x.png -> exam0625 /x/x/ x.png
        //解决覆盖问题： 确保对象和文件的名字唯一即可！！ uuid - - -
        //1.需要添加文件夹 2.添加uuid确保不重复
        String objectName = folder + "/" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "/" +
                UUID.randomUUID().toString().replaceAll("-","")+"_"+ file.getOriginalFilename();

        log.debug("文件上传核心业务方法，处理后的文件对象名：{}",objectName);

        //4. 上传文件 putObject方法
        //putObject . 上传文件数据 .steam(文件输入流)
        //uploadObject .上传文件数据 .filename(文件的磁盘地址 c:\\)
        minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .contentType(resolveContentType(file))
                        .object(objectName) //对象
                        .stream(file.getInputStream(),file.getSize(),-1) //-1 我们不指定文件切割大小！让minio自动处理！
                .build());

        //5. 拼接回显地址 【端点 + 桶 + 对象名】
        String url = String.join("/", minioProperties.getEndpoint(), minioProperties.getBucketName(), objectName);
        log.info("文件上传核心业务，完成{}文件上传，返回地址为：{}",objectName,url);
        return url;
    }

    /**
     * 兜底解析 Content-Type：部分客户端（如 curl/低版本浏览器）对 mp4/jpg 传 application/octet-stream，
     * 会导致 MinIO 回显 URL 播放时类型不准确，按扩展名纠正，保证视频/图片可正常播放展示。
     */
    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isEmpty()
                && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            return contentType;
        }
        String name = file.getOriginalFilename();
        if (name == null) {
            return contentType == null || contentType.isEmpty() ? "application/octet-stream" : contentType;
        }
        String lower = name.toLowerCase();
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".avi")) return "video/x-msvideo";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    @Override
    public void deleteFile(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            java.net.URI uri = new java.net.URI(url);
            String path = uri.getPath();
            if (path == null || path.length() <= 1) {
                return;
            }
            String p = path.startsWith("/") ? path.substring(1) : path;
            int idx = p.indexOf('/');
            if (idx <= 0) {
                // 仅 bucket 无 object，无可删对象
                return;
            }
            String bucket = p.substring(0, idx);
            String object = p.substring(idx + 1);
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(object)
                    .build());
            log.info("已删除 MinIO 文件：{}/{}", bucket, object);
        } catch (Exception e) {
            // 存储删除失败不影响业务记录删除，仅记录日志
            log.warn("删除 MinIO 文件失败（不影响业务）：url={}，原因：{}", url, e.getMessage());
        }
    }
}
