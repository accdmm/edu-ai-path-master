package com.atguigu.exam.controller;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.InterviewCompany;
import com.atguigu.exam.service.AdminContentService;
import com.atguigu.exam.vo.InterviewQuestionManageQueryVo;
import com.atguigu.exam.vo.InterviewQuestionSaveVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端内容控制器：真题 CRUD、企业与分类
 */
@RestController
@RequestMapping
@CrossOrigin
@Tag(name = "管理端内容")
public class AdminContentController {

    @Autowired
    private AdminContentService adminContentService;

    @Operation(summary = "管理端真题分页")
    @GetMapping("/api/admin/interview-questions")
    public Result<IPage<Map<String, Object>>> page(InterviewQuestionManageQueryVo query) {
        return adminContentService.manageQuestionPage(query);
    }

    @Operation(summary = "新增真题")
    @PostMapping("/api/admin/interview-questions")
    public Result<Map<String, Object>> create(@RequestBody InterviewQuestionSaveVo vo) {
        return adminContentService.saveQuestion(vo);
    }

    @Operation(summary = "编辑真题")
    @PutMapping("/api/admin/interview-questions")
    public Result<Map<String, Object>> update(@RequestBody InterviewQuestionSaveVo vo) {
        return adminContentService.saveQuestion(vo);
    }

    @Operation(summary = "删除真题")
    @DeleteMapping("/api/admin/interview-questions/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return adminContentService.deleteQuestion(id);
    }

    @Operation(summary = "企业列表（下拉）")
    @GetMapping("/api/companies/enabled")
    public Result<List<Map<String, Object>>> companies() {
        return adminContentService.listCompanies();
    }

    @Operation(summary = "企业列表（管理端）")
    @GetMapping("/api/companies/list")
    public Result<Map<String, Object>> companyList(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer size,
                                                   @RequestParam(required = false) String keyword) {
        return adminContentService.pageCompanies(page, size, keyword);
    }

    @Operation(summary = "企业详情（门户）")
    @GetMapping("/api/companies/{id}")
    public Result<Map<String, Object>> companyDetail(@PathVariable Long id) {
        return adminContentService.companyDetail(id);
    }

    @Operation(summary = "新增企业")
    @PostMapping("/api/companies")
    public Result<Map<String, Object>> createCompany(@RequestBody InterviewCompany company) {
        return adminContentService.createCompany(company);
    }

    @Operation(summary = "编辑企业")
    @PutMapping("/api/companies/{id}")
    public Result<Map<String, Object>> updateCompany(@PathVariable Long id, @RequestBody InterviewCompany company) {
        return adminContentService.updateCompany(id, company);
    }

    @Operation(summary = "删除企业")
    @DeleteMapping("/api/companies/{id}")
    public Result<Void> deleteCompany(@PathVariable Long id) {
        return adminContentService.deleteCompany(id);
    }

    @Operation(summary = "题集分类列表（按企业）")
    @GetMapping("/api/company-question-categories/enabled")
    public Result<List<Map<String, Object>>> categories(@RequestParam(required = false) Long companyId) {
        return adminContentService.listCategories(companyId);
    }

    @Operation(summary = "题集分类树（管理端）")
    @GetMapping("/api/company-question-categories/tree")
    public Result<List<Map<String, Object>>> categoryTree() {
        return adminContentService.listCategories(null);
    }
}