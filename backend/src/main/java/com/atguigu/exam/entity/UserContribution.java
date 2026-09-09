package com.atguigu.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户上传真题审核实体类
 */
@Data
@TableName("user_contribution")
@Schema(description = "用户上传真题")
public class UserContribution implements Serializable {

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "上传用户ID")
    private Long userId;

    @Schema(description = "题目描述")
    private String content;

    @Schema(description = "图片URL(JSON数组)")
    private String imageUrls;

    @Schema(description = "联系方式")
    private String contact;

    @Schema(description = "状态：0待审核/1已采纳/2未采纳")
    private Integer status;

    @Schema(description = "审核备注")
    private String adminRemark;

    @Schema(description = "上传时间")
    private Date createdTime;

    @Schema(description = "审核时间")
    private Date reviewedTime;
}