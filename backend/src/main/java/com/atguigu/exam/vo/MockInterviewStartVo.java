package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 开始模拟面试请求VO
 */
@Data
@Schema(description = "开始模拟面试请求")
public class MockInterviewStartVo implements Serializable {

    @Schema(description = "技术方向：java/frontend/bigdata/algorithm/devops/testing")
    private String direction;

    @Schema(description = "题目数量 3-20")
    private Integer questionCount;

    @Schema(description = "难度：easy/medium/hard")
    private String difficulty;

    @Schema(description = "公司类型：large/medium/startup")
    private String companyType;

    @Schema(description = "面试时长(分钟) 15-120")
    private Integer duration;
}