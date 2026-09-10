package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * AI 学习路径详情 VO（路径头部 + 按阶段分组节点）
 */
@Data
@Schema(description = "AI 学习路径详情")
public class LearningPathDetailVo {

    @Schema(description = "路径ID")
    private Long id;

    @Schema(description = "状态：ACTIVE/OUTDATED/COMPLETED")
    private String status;

    @Schema(description = "AI 规划总结")
    private String summary;

    @Schema(description = "节点总数")
    private Integer nodeCount;

    @Schema(description = "已完成节点数")
    private Integer completedCount;

    @Schema(description = "生成进度（0-100，仅 status=GENERATING 时有值，供前端轮询展示）")
    private Integer progress;

    @Schema(description = "诊断快照：知识点掌握度列表（按得分率升序=最薄弱在前）")
    private List<DiagnosisItemVo> diagnosis;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "阶段列表")
    private List<PhaseVo> phases;

    /**
     * 诊断快照单项
     */
    @Data
    @Schema(description = "知识点掌握度诊断单项")
    public static class DiagnosisItemVo {

        @Schema(description = "知识点分类名")
        private String categoryName;

        @Schema(description = "答题数")
        private Integer answerCount;

        @Schema(description = "得分率（0-100，按分值）")
        private Integer correctRate;
    }

    /**
     * 阶段（含节点列表）
     */
    @Data
    @Schema(description = "路径阶段")
    public static class PhaseVo {

        @Schema(description = "阶段序号（从1开始）")
        private Integer phase;

        @Schema(description = "阶段标题")
        private String phaseTitle;

        @Schema(description = "节点列表")
        private List<NodeVo> nodes;
    }

    /**
     * 节点
     */
    @Data
    @Schema(description = "路径节点")
    public static class NodeVo {

        @Schema(description = "节点ID")
        private Long id;

        @Schema(description = "节点类型：KNOWLEDGE/PAPER/QUESTION")
        private String nodeType;

        @Schema(description = "关联实体ID：PAPER=paper.id / QUESTION=interview_question.id / KNOWLEDGE=null")
        private Long refId;

        @Schema(description = "关联知识点分类名")
        private String categoryName;

        @Schema(description = "节点标题")
        private String title;

        @Schema(description = "节点描述/讲解文本/完成指引")
        private String description;

        @Schema(description = "完成状态：PENDING/COMPLETED")
        private String status;
    }
}
