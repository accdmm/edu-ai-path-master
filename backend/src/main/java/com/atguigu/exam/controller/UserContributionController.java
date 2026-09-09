package com.atguigu.exam.controller;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.service.UserContributionService;
import com.atguigu.exam.utils.UserContextUtil;
import com.atguigu.exam.vo.ContributionReviewVo;
import com.atguigu.exam.vo.UserContributionVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户上传真题审核控制器
 */
@RestController
@RequestMapping("/api")
@CrossOrigin
@Tag(name = "用户上传真题")
public class UserContributionController {

    @Autowired
    private UserContributionService contributionService;
    @Autowired
    private UserContextUtil userContextUtil;

    @Operation(summary = "用户上传真题")
    @PostMapping("/user-contributions/upload")
    public Result<Map<String, Object>> upload(@RequestBody UserContributionVo vo) {
        return contributionService.submitContribution(
                userContextUtil.getUserId(), vo.getContent(), vo.getImageUrls(), vo.getContact());
    }

    @Operation(summary = "用户上传真题（pending-questions 兼容）")
    @PostMapping("/pending-questions")
    public Result<Map<String, Object>> uploadV2(@RequestBody Map<String, Object> body) {
        String content = body.get("content") == null ? null : body.get("content").toString();
        String contact = body.get("contact") == null ? null : body.get("contact").toString();
        List<String> imageUrls = null;
        Object imgObj = body.get("image_urls");
        if (imgObj instanceof List<?> list) {
            imageUrls = list.stream().map(Object::toString).toList();
        }
        return contributionService.submitContribution(userContextUtil.getUserId(), content, imageUrls, contact);
    }

    @Operation(summary = "我的上传记录")
    @GetMapping("/user-contributions/mine")
    public Result<IPage<Map<String, Object>>> mine(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer size) {
        return contributionService.getMyContributions(userContextUtil.getUserId(), page, size);
    }

    @Operation(summary = "管理端：待审核列表")
    @GetMapping("/pending-questions")
    public Result<IPage<Map<String, Object>>> pending(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "10") Integer size,
                                                      @RequestParam(required = false) Integer status) {
        return contributionService.getPendingContributions(page, size, status);
    }

    @Operation(summary = "管理端：记录详情")
    @GetMapping("/pending-questions/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        IPage<Map<String, Object>> page = contributionService.getPendingContributions(1, 100, null).getData();
        if (page == null) {
            return Result.error("记录不存在");
        }
        return page.getRecords().stream()
                .filter(m -> id.equals(m.get("id")))
                .findFirst()
                .map(Result::<Map<String, Object>>success)
                .orElseGet(() -> Result.error(404, "记录不存在"));
    }

    @Operation(summary = "管理端：审核")
    @PostMapping("/pending-questions/review")
    public Result<Void> review(@RequestBody Map<String, Object> body) {
        ContributionReviewVo vo = new ContributionReviewVo();
        vo.setId(body.get("id") == null ? null : Long.valueOf(body.get("id").toString()));
        vo.setStatus(body.get("status") == null ? null : Integer.valueOf(body.get("status").toString()));
        vo.setAdminRemark(body.get("admin_remark") == null
                ? null : body.get("admin_remark").toString());
        return contributionService.reviewContribution(vo);
    }
}