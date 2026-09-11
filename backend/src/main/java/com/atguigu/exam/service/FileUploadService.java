package com.atguigu.exam.service;


import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件上传服务
 * 支持MinIO和本地文件存储两种方式
 */

public interface FileUploadService {

    /**
     * 实现文件上传的核心业务方法！
     * @param folder 不同上传位置的文件夹！ 例如： 轮播图 banners
     * @param file 上传的文件封装对象！
     * @return 可以访问的文件地址
     */
    String uploadFile(String folder, MultipartFile file) throws Exception;

    /**
     * 根据回显 URL 删除 MinIO 中的对象（用于记录删除时同步清理存储）。
     * 文件不存在或解析失败时静默忽略，不影响业务。
     * @param url 回显地址（形如 http://host:port/bucket/object）
     */
    void deleteFile(String url);

} 