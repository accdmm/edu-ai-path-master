package com.atguigu.exam.controller;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.service.InviteCodeService;
import com.atguigu.exam.utils.UserContextUtil;
import com.atguigu.exam.vo.InviteCodeActivateVo;
import com.atguigu.exam.vo.InviteCodeGenerateVo;
import com.atguigu.exam.vo.InviteCodeRequestVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 邀请码控制器
 */
@RestController
@RequestMapping("/interview/codes")
@CrossOrigin
@Tag(name = "邀请码")
public class InviteCodeController {

    @Autowired
    private InviteCodeService inviteCodeService;
    @Autowired
    private UserContextUtil userContextUtil;

    @Operation(summary = "邀请码列表（管理端）")
    @GetMapping
    public Result<IPage<Map<String, Object>>> list(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer size,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String type) {
        return inviteCodeService.listCodes(page, size, status, type);
    }

    @Operation(summary = "生成邀请码（管理端）")
    @PostMapping("/generate")
    public Result<List<String>> generate(@RequestBody InviteCodeGenerateVo vo) {
        return inviteCodeService.generateCodes(vo);
    }

    @Operation(summary = "激活邀请码")
    @PostMapping("/activate")
    public Result<Map<String, Object>> activate(@RequestBody InviteCodeActivateVo vo) {
        return inviteCodeService.activate(userContextUtil.getUserId(), vo);
    }

    @Operation(summary = "删除邀请码")
    @DeleteMapping("/{codeId}")
    public Result<Void> delete(@PathVariable Long codeId) {
        return inviteCodeService.deleteCode(codeId);
    }

    @Operation(summary = "被邀请用户列表")
    @GetMapping("/invitees")
    public Result<IPage<Map<String, Object>>> invitees(@RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "10") Integer size) {
        return inviteCodeService.listInvitees(page, size);
    }

    @Operation(summary = "申请邀请码")
    @PostMapping("/request")
    public Result<Void> request(@RequestBody InviteCodeRequestVo vo) {
        return inviteCodeService.requestCode(vo);
    }
}