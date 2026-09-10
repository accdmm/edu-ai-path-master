package com.atguigu.exam.controller;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.service.LearningPathService;
import com.atguigu.exam.utils.UserContextUtil;
import com.atguigu.exam.vo.LearningPathDetailVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 学习路径控制器
 * 需登录（业务级校验：SecurityConfig permitAll 场景下在 controller 层强制）
 */
@Slf4j
@RestController
@RequestMapping("/api/learning-path")
@Tag(name = "AI学习路径", description = "基于答题诊断的 AI 学习路径规划")
public class LearningPathController {

    @Autowired
    private LearningPathService learningPathService;

    @Autowired
    private UserContextUtil userContextUtil;

    @PostMapping("/generate")
    @Operation(summary = "生成学习路径", description = "基于答题诊断数据由 AI 生成结构化学习路径，旧 ACTIVE 路径自动归档。LLM 编排约需 30-60 秒")
    public Result<LearningPathDetailVo> generate() {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "未登录");
        }
        return Result.success(learningPathService.generatePath(userContextUtil.getUserId()));
    }

    @GetMapping("/active")
    @Operation(summary = "查询当前学习路径", description = "返回用户当前路径详情（生成中/失败/生效），读取时自动同步 PAPER 节点完成状态。无则 data=null")
    public Result<LearningPathDetailVo> active() {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "未登录");
        }
        return Result.success(learningPathService.getActivePath(userContextUtil.getUserId()));
    }

    @PostMapping("/node/{nodeId}/toggle")
    @Operation(summary = "切换节点完成状态", description = "KNOWLEDGE/QUESTION 节点手动打勾/取消；PAPER 节点由系统自动判定（拒绝手动切换）。全部节点完成后路径自动置 COMPLETED")
    public Result<Void> toggleNode(@PathVariable Long nodeId) {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "未登录");
        }
        learningPathService.toggleNode(userContextUtil.getUserId(), nodeId);
        return Result.success(null);
    }

    @PostMapping("/diagnostic-paper")
    @Operation(summary = "一键生成综合诊断卷", description = "从题库混合抽取约 10 道题组卷（DRAFT 归属当前用户），返回试卷ID；前端直接跳 /exam/start/{paperId}")
    public Result<Long> diagnosticPaper() {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "未登录");
        }
        return Result.success(learningPathService.generateDiagnosticPaper(userContextUtil.getUserId()));
    }
}
