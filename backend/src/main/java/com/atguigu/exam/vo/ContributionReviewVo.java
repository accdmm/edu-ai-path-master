package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 上传审核VO
 */
@Data
@Schema(description = "真题审核请求")
public class ContributionReviewVo implements Serializable {

    @Schema(description = "待审核记录ID")
    private Long id;

    @Schema(description = "状态：1=采纳/2=不采纳")
    private Integer status;

    @Schema(description = "审核备注")
    private String adminRemark;
}