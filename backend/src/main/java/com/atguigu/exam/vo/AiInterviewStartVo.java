package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 开始 AI 面试官请求 VO（BOSS 直聘式多轮对话面试）
 */
@Data
@Schema(description = "开始 AI 面试官请求")
public class AiInterviewStartVo implements Serializable {

    @Schema(description = "技术方向：java/frontend/bigdata/algorithm/devops/testing")
    private String direction;

    @Schema(description = "难度：easy/medium/hard")
    private String difficulty;

    @Schema(description = "个性化：结合答题诊断优先考察薄弱知识点")
    private Boolean personalized;

    @Schema(description = "最大轮数 3-12，默认 6（超出后 AI 自动总结结束）")
    private Integer maxRounds;
}
