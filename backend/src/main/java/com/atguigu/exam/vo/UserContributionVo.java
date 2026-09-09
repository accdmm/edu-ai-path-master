package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户上传真题请求VO
 */
@Data
@Schema(description = "用户上传真题请求")
public class UserContributionVo implements Serializable {

    @Schema(description = "题目描述")
    private String content;

    @Schema(description = "图片URL数组")
    private List<String> imageUrls;

    @Schema(description = "联系方式")
    private String contact;
}