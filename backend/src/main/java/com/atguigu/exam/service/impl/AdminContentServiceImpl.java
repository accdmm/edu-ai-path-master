package com.atguigu.exam.service.impl;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.InterviewCompany;
import com.atguigu.exam.entity.InterviewQuestion;
import com.atguigu.exam.entity.InterviewQuestionCategory;
import com.atguigu.exam.mapper.InterviewCompanyMapper;
import com.atguigu.exam.mapper.InterviewQuestionCategoryMapper;
import com.atguigu.exam.mapper.InterviewQuestionMapper;
import com.atguigu.exam.service.AdminContentService;
import com.atguigu.exam.vo.InterviewQuestionManageQueryVo;
import com.atguigu.exam.vo.InterviewQuestionSaveVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端内容服务实现
 */
@Slf4j
@Service
public class AdminContentServiceImpl implements AdminContentService {

    @Autowired
    private InterviewQuestionMapper questionMapper;
    @Autowired
    private InterviewCompanyMapper companyMapper;
    @Autowired
    private InterviewQuestionCategoryMapper categoryMapper;

    @Override
    public Result<IPage<Map<String, Object>>> manageQuestionPage(InterviewQuestionManageQueryVo query) {
        try {
            LambdaQueryWrapper<InterviewQuestion> wrapper = new LambdaQueryWrapper<>();
            if (query.getCompanyId() != null) {
                wrapper.eq(InterviewQuestion::getCompanyId, query.getCompanyId());
            }
            if (query.getCategoryId() != null) {
                wrapper.eq(InterviewQuestion::getCategoryId, query.getCategoryId());
            }
            if (query.getDifficulty() != null && query.getDifficulty() >= 1 && query.getDifficulty() <= 3) {
                String difficultyLevel = switch (query.getDifficulty()) {
                    case 1 -> "easy";
                    case 2 -> "medium";
                    default -> "hard";
                };
                wrapper.eq(InterviewQuestion::getDifficultyLevel, difficultyLevel);
            }
            if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
                wrapper.and(w -> w.like(InterviewQuestion::getQuestionContent, query.getKeyword())
                        .or().like(InterviewQuestion::getReferenceAnswer, query.getKeyword()));
            }
            wrapper.orderByDesc(InterviewQuestion::getCreateTime);

            Page<InterviewQuestion> pg = new Page<>(query.getPage() == null ? 1 : query.getPage(),
                    query.getSize() == null ? 10 : query.getSize());
            IPage<InterviewQuestion> result = questionMapper.selectPage(pg, wrapper);
            IPage<Map<String, Object>> data = result.convert(this::toManageView);
            return Result.success(data);
        } catch (Exception e) {
            log.error("管理端真题分页查询失败", e);
            return Result.error("管理端真题分页查询失败");
        }
    }

    @Override
    public Result<Map<String, Object>> saveQuestion(InterviewQuestionSaveVo vo) {
        try {
            InterviewQuestion q = toEntity(vo);
            if (vo.getId() == null) {
                q.setCreateTime(new Date());
                q.setStatus("approved");
                questionMapper.insert(q);
            } else {
                q.setUpdateTime(new Date());
                questionMapper.updateById(q);
            }
            return Result.success(toManageView(q), "保存成功");
        } catch (Exception e) {
            log.error("保存真题失败", e);
            return Result.error("保存真题失败");
        }
    }

    @Override
    public Result<Void> deleteQuestion(Long id) {
        try {
            questionMapper.deleteById(id);
            return Result.success(null, "删除成功");
        } catch (Exception e) {
            log.error("删除真题失败", e);
            return Result.error("删除真题失败");
        }
    }

    @Override
    public Result<List<Map<String, Object>>> listCompanies() {
        try {
            List<InterviewCompany> list = companyMapper.selectList(
                    new LambdaQueryWrapper<InterviewCompany>().orderByDesc(InterviewCompany::getTotalQuestions));
            List<Map<String, Object>> data = list.stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("name", c.getName());
                return m;
            }).toList();
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询企业列表失败", e);
            return Result.error("查询企业列表失败");
        }
    }

    @Override
    public Result<List<Map<String, Object>>> listCategories(Long companyId) {
        try {
            LambdaQueryWrapper<InterviewQuestionCategory> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(companyId != null, InterviewQuestionCategory::getCompanyId, companyId)
                    .orderByAsc(InterviewQuestionCategory::getSort);
            List<InterviewQuestionCategory> list = categoryMapper.selectList(wrapper);
            List<Map<String, Object>> data = list.stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("name", c.getName());
                m.put("direction", c.getDirection());
                m.put("companyId", c.getCompanyId());
                return m;
            }).toList();
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询题集分类失败", e);
            return Result.error("查询题集分类失败");
        }
    }

    @Override
    public InterviewQuestion toEntity(InterviewQuestionSaveVo vo) {
        InterviewQuestion q = new InterviewQuestion();
        q.setId(vo.getId());
        q.setCompanyId(vo.getCompanyId());
        q.setCategoryId(vo.getCategoryId());
        String content = vo.getContent();
        if (vo.getTitle() != null && !vo.getTitle().isBlank()) {
            content = (content == null || content.isBlank()) ? vo.getTitle()
                    : vo.getTitle() + "\n" + content;
        }
        q.setQuestionContent(content);
        if (vo.getDifficulty() != null && vo.getDifficulty() >= 1 && vo.getDifficulty() <= 3) {
            q.setDifficultyLevel(switch (vo.getDifficulty()) {
                case 1 -> "easy";
                case 2 -> "medium";
                default -> "hard";
            });
        } else {
            q.setDifficultyLevel("medium");
        }
        q.setReferenceAnswer(vo.getAnswer());
        String direction = (vo.getCategoryId() != null) ? getCategoryDirection(vo.getCategoryId()) : "java";
        q.setDirection(direction == null || direction.isBlank() ? "java" : direction);
        q.setStatus(vo.getStatus() != null && vo.getStatus() == 0 ? "disabled" : "approved");
        return q;
    }

    private String getCategoryDirection(Long categoryId) {
        InterviewQuestionCategory category = categoryMapper.selectById(categoryId);
        return category == null ? "java" : category.getDirection();
    }

    private Map<String, Object> toManageView(InterviewQuestion q) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", q.getId());
        m.put("companyId", q.getCompanyId());
        m.put("categoryId", q.getCategoryId());
        m.put("title", q.getQuestionContent());
        m.put("content", q.getQuestionContent());
        m.put("difficulty", switch (q.getDifficultyLevel() == null ? "medium" : q.getDifficultyLevel()) {
            case "easy" -> 1;
            case "hard" -> 3;
            default -> 2;
        });
        m.put("difficultyLevel", q.getDifficultyLevel());
        m.put("direction", q.getDirection());
        m.put("answer", q.getReferenceAnswer());
        m.put("status", "approved".equals(q.getStatus()) ? 1 : 0);
        m.put("companyName", getCompanyName(q.getCompanyId()));
        m.put("categoryName", getCategoryName(q.getCategoryId()));
        m.put("viewCount", q.getViewCount());
        m.put("createTime", q.getCreateTime());
        return m;
    }

    private String getCompanyName(Long companyId) {
        if (companyId == null) {
            return null;
        }
        InterviewCompany company = companyMapper.selectById(companyId);
        return company == null ? null : company.getName();
    }

    private String getCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        InterviewQuestionCategory category = categoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }
}