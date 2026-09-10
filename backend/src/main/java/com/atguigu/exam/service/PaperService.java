package com.atguigu.exam.service;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.Paper;
import com.atguigu.exam.vo.AiPaperVo;
import com.atguigu.exam.vo.PaperVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * 试卷服务接口
 */
public interface PaperService extends IService<Paper> {

    /**
     * AI 解析试卷内某道题（消费积分，每日 3 次免费，与企业真题共用类型 ai-analysis 额度）
     *
     * @param paperId    试卷id
     * @param questionId 题目id
     * @param userId     当前用户id
     * @return {analysis, free, remainingFree, activeCredits}
     */
    Result<Map<String, Object>> aiAnalysis(Long paperId, Long questionId, Long userId);

    /**
     * 根据试卷id试卷详情
     *    试卷对象
     *    题目集合
     *    注意： 题目的选项sort正序
     *    注意： 所有题目根据类型排序
     * @param id 试卷id
     * @return
     */
    Paper customPaperDetailById(Long id);

    /**
     * 根据试卷id查详情并校验访问权限
     * 发布(PUBLISHED)试卷所有人可看；草稿(DRAFT)试卷仅归属用户可见
     * @param id 试卷id
     * @param userId 访问者用户id
     * @return
     */
    Paper customPaperDetailByIdWithAuth(Long id, Long userId);

    /**
     * 手动组卷
     * @param paperVo
     * @return
     */
    Paper customCreatePaper(PaperVo paperVo);

    /**
     * 智能组卷
     * @param aiPaperVo
     * @return
     */
    Paper customAiCreatePaper(AiPaperVo aiPaperVo);

    /**
     * 更新试卷内信息
     * @param id
     * @param paperVo
     * @return
     */
    Paper customUpdatePaper(Integer id, PaperVo paperVo);

    /**
     * 根据id修改状态
     * @param id
     * @param status
     */
    void customUpdatePaperStatus(Integer id, String status);

    /**
     * 根据id删除试卷功能
     * @param id
     */
    void customRemoveId(Integer id);
}