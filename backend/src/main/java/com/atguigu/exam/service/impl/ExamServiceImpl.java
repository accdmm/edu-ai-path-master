package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.AnswerRecord;
import com.atguigu.exam.entity.CreditRecord;
import com.atguigu.exam.entity.ExamRecord;
import com.atguigu.exam.entity.Paper;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.entity.UserCredit;
import com.atguigu.exam.mapper.CreditRecordMapper;
import com.atguigu.exam.mapper.ExamRecordMapper;
import com.atguigu.exam.mapper.UserCreditMapper;
import com.atguigu.exam.service.AnswerRecordService;
import com.atguigu.exam.service.ExamService;
import com.atguigu.exam.service.KimiAiService;
import com.atguigu.exam.service.PaperService;
import com.atguigu.exam.service.UserPaperService;
import com.atguigu.exam.utils.UserContextUtil;
import com.atguigu.exam.vo.ExamRankingVO;
import com.atguigu.exam.vo.GradingResult;
import com.atguigu.exam.vo.StartExamVo;
import com.atguigu.exam.vo.SubmitAnswerVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.AlgorithmParametersSpi;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


/**
 * 考试服务实现类
 */
@Service
@Slf4j
public class ExamServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamService {

    /** 简答题 AI 评分线程池（判卷并行化，避免 N 道简答题串行累加耗时） */
    private static final ExecutorService AI_GRADING_EXECUTOR = Executors.newFixedThreadPool(4);

    /** Redis Stream：判卷任务队列 */
    private static final String GRADING_STREAM = "exam:grade";

    /** 改卷 AI 计费：超出每日免费份数后每份扣积分 */
    private static final int AI_GRADING_COST = 5;

    /** 改卷 AI 每日免费份数 */
    private static final int AI_GRADING_DAILY_FREE = 3;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Autowired
    private PaperService paperService;

    @Autowired
    private AnswerRecordService answerRecordService;

    @Autowired
    private KimiAiService kimiAiService;

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private UserPaperService userPaperService;

    @Autowired
    private UserContextUtil userContextUtil;

    @Autowired
    private UserCreditMapper userCreditMapper;

    @Autowired
    private CreditRecordMapper creditRecordMapper;

    //开始考试
    @Override
    public ExamRecord startExam(StartExamVo startExamVo) {
        Long currentUserId = userContextUtil.getUserId();
        //宏观： 创建一个考试对象，并存储到数据库即可
        //0. 私有试卷(DRAFT/AI生成卷)开考权限校验：仅归属用户可考；PUBLISHED 对所有用户开放
        Paper paper = paperService.getById(startExamVo.getPaperId());
        if (paper == null) {
            throw new RuntimeException("指定试卷不存在，无法开始考试！");
        }
        if (!"PUBLISHED".equals(paper.getStatus())) {
            boolean allowed = userPaperService.existRelation(currentUserId, startExamVo.getPaperId().longValue());
            if (!allowed) {
                throw new RuntimeException("该试卷为私有试卷，您无权开始考试！");
            }
        }
        //1. 校验，该学生当前试卷是否存在正在考试的记录！ 存在进行中，返回即可
        LambdaQueryWrapper<ExamRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ExamRecord::getStudentName, startExamVo.getStudentName());
        lambdaQueryWrapper.eq(ExamRecord::getExamId,startExamVo.getPaperId());
        lambdaQueryWrapper.eq(ExamRecord::getStatus,"进行中");
        ExamRecord examRecord = getOne(lambdaQueryWrapper);
        if (examRecord != null) {
            log.debug("{}在当前试卷：{}有未完成考试记录！直接返回了！", startExamVo.getStudentName(), startExamVo.getPaperId());
            return examRecord;
        }
        //2. 创建新的考试记录！赋予传入的参数（学生姓名，试卷id） 补全（状态，时间，切屏数）
        examRecord = new ExamRecord();
        examRecord.setStudentName(startExamVo.getStudentName());
        examRecord.setExamId(startExamVo.getPaperId());
        examRecord.setUserId(currentUserId);
        examRecord.setStatus("进行中");
        examRecord.setWindowSwitches(0);
        examRecord.setStartTime(LocalDateTime.now());
        //3. 保存即可！
        save(examRecord);
        //4. 返回即可
        return examRecord;
    }

    @Override
    public ExamRecord customGetExamRecordById(Integer id) {
        //宏观：获取考试记录，考试记录对应的试卷对象，获取考试记录对应的答题记录集合
        //注意： 答题记录和顺序和考试记录的顺序相同！
        //1. 获取考试记录详情
        ExamRecord examRecord = getById(id);
        if (examRecord == null) {
            throw new RuntimeException("开始考试的记录已经被删除！");
        }
        //2. 获取考试记录对应试卷对象详情 【试卷 题目 选项 和 答案】
        Paper paper = paperService.customPaperDetailById(examRecord.getExamId().longValue());
        if (paper == null) {
            throw new RuntimeException("当前考试记录的试卷被删除！获取考试记录详情失败！");
        }
        //3. 获取考试记录对应的答题记录集合
        LambdaQueryWrapper<AnswerRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AnswerRecord::getExamRecordId,id);
        List<AnswerRecord> answerRecords = answerRecordService.list(lambdaQueryWrapper);
        if (!ObjectUtils.isEmpty(answerRecords)){
            //[8,2,1,3,7,4] -> 题目id
            List<Long> questionIdList = paper.getQuestions().stream().map(Question::getId).collect(Collectors.toList());
            //[{questionId:1} -> 2 ,{questionId:2} -> 1 ,{questionId:3} -> 3,{questionId:4} ->5,{questionId:7} -> 4,{questionId:8} -> 0]
            answerRecords.sort((o1, o2) -> {
                int x = questionIdList.indexOf(o1.getQuestionId());
                int y = questionIdList.indexOf(o2.getQuestionId());
                return Integer.compare(o1.getQuestionId(),o2.getQuestionId());
            });
        }
        //4. 数据组装即可
        examRecord.setPaper(paper);
        examRecord.setAnswerRecords(answerRecords);
        return examRecord;
    }

    @Override
    public void customSubmitAnswer(Integer examRecordId, List<SubmitAnswerVo> answers) throws InterruptedException {
        //宏观： 提交答案中间表保存  修改考试记录数据（已完成 ，结束时间）  投递判卷任务（异步）
        //0. 幂等校验：仅"进行中"的考试可交卷，防止重复交卷重复判卷/重复计费
        ExamRecord current = getById(examRecordId);
        if (current == null) {
            throw new RuntimeException("考试记录不存在！");
        }
        if ("判卷中".equals(current.getStatus())) {
            throw new RuntimeException("试卷正在判卷中，请勿重复提交！");
        }
        if ("已批阅".equals(current.getStatus())) {
            throw new RuntimeException("该考试已完成并批阅，不能重复交卷！");
        }
        //1.中间表保存问题
        if (!ObjectUtils.isEmpty(answers)) {
            List<AnswerRecord> answerRecordList = answers.stream().map(vo -> new AnswerRecord(examRecordId, vo.getQuestionId(), vo.getUserAnswer()))
                    .collect(Collectors.toList());
            answerRecordService.saveBatch(answerRecordList);
        }
        //2. 暂时修改下考试记录状态（状态 -》 判卷中 || 结束时间 - 设置），条件更新防重复触发
        ExamRecord examRecord = getById(examRecordId);
        examRecord.setEndTime(LocalDateTime.now());
        examRecord.setStatus("判卷中");
        boolean updated = update(examRecord,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ExamRecord>()
                        .eq(ExamRecord::getId, examRecordId)
                        .eq(ExamRecord::getStatus, "进行中"));
        if (!updated) {
            throw new RuntimeException("考试状态已变化，请勿重复交卷！");
        }

        //3.投递判卷任务到 Redis Stream，由 ExamGradingWorker 异步消费，提交接口立即返回
        try {
            stringRedisTemplate.opsForStream().add(
                    GRADING_STREAM,
                    Collections.singletonMap("examRecordId", String.valueOf(examRecordId)));
        } catch (Exception e) {
            // Redis 不可用时降级为同步判卷，保证交卷功能不因队列故障失效；
            // 同步判卷若仍有失败（如 AI 不可用），强制按已保存答题记录结算，避免状态卡死在"判卷中"
            log.warn("投递 Redis Stream 判卷任务失败，降级同步判卷。原因：{}", e.getMessage());
            try {
                gradeExam(examRecordId);
            } catch (Exception gradingEx) {
                log.error("降级同步判卷失败，强制结算 examRecordId={} 原因: {}",
                        examRecordId, gradingEx.getMessage());
                forceSettleGrading(examRecordId);
            }
        }
    }

    @Override
    public ExamRecord gradeExam(Integer examRecordId) throws InterruptedException {
        //宏观：  获取考试记录相关的信息（考试记录对象 考试记录答题记录 考试对应试卷）
        //  进行循环判断（1.答题记录进行修改 2.总体提到总分数 总正确数量）  修改考试记录（状态 -》 已批阅  修改 -》 总分数）   进行ai评语生成（总正确的题目数量）
        //  修改考试记录表  返回考试记录对象
        //0. 幂等：已批阅的记录直接返回现有结果，避免重复判卷/重复计费
        ExamRecord existed = getById(examRecordId);
        if (existed != null && "已批阅".equals(existed.getStatus())) {
            return customGetExamRecordById(examRecordId);
        }
        //1.获取考试记录和相关的信息（试卷和答题记录）
        ExamRecord examRecord = customGetExamRecordById(examRecordId);
        Paper paper = examRecord.getPaper();
        if (paper == null){
            examRecord.setStatus("已批阅");
            examRecord.setAnswers("考试对应的试卷被删除！无法进行成绩判定！");
            updateById(examRecord);
            throw new RuntimeException("考试对应的试卷被删除！无法进行成绩判定！");
        }
        List<AnswerRecord> answerRecords = examRecord.getAnswerRecords();
        if (ObjectUtils.isEmpty(answerRecords)){
            //没有提交
            examRecord.setStatus("已批阅");
            examRecord.setScore(0);
            examRecord.setAnswers("没有提交记录！成绩为零！继续加油！");
            updateById(examRecord);
            return examRecord;
        }

        //2.进行循环的判卷（1.记录总分数 2.记录正确题目数量 3. 修改每个答题记录的状态（得分，是否正确 0 1 2 ，text-》ai评语））
        //将正确题目转成map,方便每次判断获取正确答案
        Map<Long, Question> questionMap = paper.getQuestions().stream().collect(Collectors.toMap(Question::getId, q -> q));

        //简答题 AI 评分并行执行（每道题一次 LLM 调用），客观题本地比对，整体耗时≈单题最长耗时
        List<CompletableFuture<Void>> futures = answerRecords.stream()
                .map(answerRecord -> CompletableFuture.runAsync(() -> {
                    try {
                        //1.先获取 答题记录对应的题目对象
                        Question question = questionMap.get(answerRecord.getQuestionId().longValue());
                        //重跑幂等：上一轮已成功判分的简答题直接保留，只重判"判题过程出错"的题
                        if ("TEXT".equalsIgnoreCase(question.getType())
                                && answerRecord.getScore() != null
                                && !"判题过程出错！".equals(answerRecord.getAiCorrection())) {
                            return;
                        }
                        String systemAnswer = question.getAnswer().getAnswer();
                        String userAnswer = answerRecord.getUserAnswer();
                        if ("JUDGE".equalsIgnoreCase(question.getType())){
                            //true false
                            userAnswer = normalizeJudgeAnswer(userAnswer);
                        }
                        if (!"TEXT".equals(question.getType())) {
                            //2.判断题目类型(选择和判断直接判卷)
                            if (systemAnswer.equalsIgnoreCase(userAnswer)){
                                answerRecord.setIsCorrect(1); //正确
                                answerRecord.setScore(question.getPaperScore().intValue());
                            }else{
                                answerRecord.setIsCorrect(0); //正确
                                answerRecord.setScore(0);
                            }
                        }else{
                            //3.简答题进行ai判断
                            GradingResult result =
                                    kimiAiService.gradingTextQuestion(question,userAnswer,question.getPaperScore().intValue());

                            //分
                            answerRecord.setScore(result.getScore());
                            //ai评价 正确  feedback  非正确 reason
                            if (result.getScore() == 0){
                                answerRecord.setIsCorrect(0);
                                answerRecord.setAiCorrection(result.getReason());
                            }else if (result.getScore() == question.getPaperScore().intValue()){
                                answerRecord.setIsCorrect(1);
                                answerRecord.setAiCorrection(result.getFeedback());
                            }else{
                                answerRecord.setIsCorrect(2);
                                answerRecord.setAiCorrection(result.getReason());
                            }
                        }
                    } catch (Exception e) {
                        //AI 判分失败：先落 0 分占位（评语标记"判题过程出错"），不直接定稿
                        //gradeExam 会保持"判卷中"并抛异常，由判卷队列延迟重投重试
                        log.warn("简答题 AI 判分失败 examRecordId={} questionId={} 原因: {}",
                                examRecordId, answerRecord.getQuestionId(), e.getMessage());
                        answerRecord.setScore(0);
                        answerRecord.setIsCorrect(0);
                        answerRecord.setAiCorrection("判题过程出错！");
                    }
                }, AI_GRADING_EXECUTOR))
                .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        answerRecordService.updateBatchById(answerRecords);

        //统计 AI 判分失败的简答题：存在失败则保持"判卷中"并抛异常，交由判卷队列死信机制（5 分钟后）重投重判，
        //重跑时仅重判失败的简答题；达到 worker 重试上限后按客观题强制结算。计费在结算处，重跑不会重复扣费。
        int failedTextCount = (int) answerRecords.stream()
                .filter(ar -> {
                    Question q = questionMap.get(ar.getQuestionId().longValue());
                    return q != null && "TEXT".equalsIgnoreCase(q.getType())
                            && "判题过程出错！".equals(ar.getAiCorrection());
                })
                .count();
        if (failedTextCount > 0) {
            throw new RuntimeException(failedTextCount + " 道简答题 AI 判分失败，保持判卷中等待队列重试（客观题得分已保存）");
        }

        //进行总分数与总正确题目数量统计
        int correctNumber = 0;
        int totalScore = 0;
        for (AnswerRecord answerRecord : answerRecords) {
            totalScore += answerRecord.getScore();
            if (answerRecord.getIsCorrect() == 1){
                correctNumber++;
            }
        }
        //（答题记录已在失败检查前统一保存）

        //进行ai生成评价，进行考试记录修改和完善
        //无简答题的试卷不需要 AI 总评（客观题已本地判分），直接规则评语，秒级完成判卷
        boolean hasTextQuestion = paper.getQuestions().stream()
                .anyMatch(q -> "TEXT".equalsIgnoreCase(q.getType()));
        String summary;
        if (!hasTextQuestion) {
            double percentage = paper.getTotalScore().intValue() == 0 ? 0
                    : (double) totalScore / paper.getTotalScore().intValue() * 100;
            summary = String.format("本次考试得分 %d/%d 分，得分率 %.1f%%，共 %d 道题，答对 %d 道。"
                    + "建议回顾错题对应的知识点，针对性练习，再接再厉！",
                    totalScore, paper.getTotalScore().intValue(), percentage,
                    paper.getQuestionCount(), correctNumber);
        } else {
            try {
                summary = kimiAiService.
                        buildSummary(totalScore, paper.getTotalScore().intValue(), paper.getQuestionCount(), correctNumber);
            } catch (Exception e) {
                // AI 总评失败时降级为规则型评语，保证交卷不中断
                log.error("AI生成考试总评失败，使用规则兜底。原因：{}", e.getMessage());
                double percentage = paper.getTotalScore().intValue() == 0 ? 0
                        : (double) totalScore / paper.getTotalScore().intValue() * 100;
                summary = String.format("本次考试得分 %d/%d 分，得分率 %.1f%%，共 %d 道题，答对 %d 道。"
                        + "建议回顾错题对应的知识点，针对性练习，再接再厉！",
                        totalScore, paper.getTotalScore().intValue(), percentage,
                        paper.getQuestionCount(), correctNumber);
            }
        }

        //改卷 AI 计费：仅含简答题（触发过 LLM）的试卷才计费；失败不影响交卷；纯客观题卷从入口跳过
        String billingNote = null;
        if (hasTextQuestion) {
            try {
                billingNote = billingAiGrading(examRecord.getUserId());
            } catch (Exception e) {
                log.warn("AI 判卷计费失败，跳过计费。userId={} examRecordId={}", examRecord.getUserId(), examRecordId, e);
            }
        }
        examRecord.setScore(totalScore);
        examRecord.setAnswers(billingNote == null ? summary : summary + "\n" + billingNote);
        examRecord.setStatus("已批阅");
        updateById(examRecord);

        return examRecord;
    }

    /**
     * 改卷 AI 计费：每日前 {@link #AI_GRADING_DAILY_FREE} 份免费，超出每份扣 {@link #AI_GRADING_COST} 积分；
     * 积分不足时平台赠送（免费兜底），保证 AI 判卷照常完成、交卷永不失败。
     *
     * @param userId 被扣积分的用户（考试记录归属者）
     * @return 追加到评语的计费提示文本
     */
    private String billingAiGrading(Long userId) {
        Date todayStart = Date.from(LocalDateTime.now().toLocalDate()
                .atStartOfDay(ZoneId.systemDefault()).toInstant());
        long freeCount = creditRecordMapper.selectCount(new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getUserId, userId)
                .eq(CreditRecord::getType, "exam-ai-grade")
                .eq(CreditRecord::getChangeAmount, 0)
                .ge(CreditRecord::getCreateTime, todayStart));
        boolean free = freeCount < AI_GRADING_DAILY_FREE;
        if (free) {
            insertGradingCreditRecord(userId, 0, "AI判卷免费额度");
            return "本次 AI 判卷使用免费额度，不消耗积分";
        }
        int updated = userCreditMapper.update(null, new UpdateWrapper<UserCredit>()
                .eq("user_id", userId)
                .ge("active_credits", AI_GRADING_COST)
                .setSql("active_credits = active_credits - " + AI_GRADING_COST)
                .setSql("update_time = NOW()"));
        if (updated == 0) {
            // 积分不足：平台赠送（免费兜底）
            insertGradingCreditRecord(userId, 0, "AI判卷免费(积分不足)");
            return "本次 AI 判卷因积分不足由平台赠送，加油攒积分～";
        }
        insertGradingCreditRecord(userId, -AI_GRADING_COST, "AI判卷");
        return "本次 AI 判卷已消耗 " + AI_GRADING_COST + " 积分";
    }

    private void insertGradingCreditRecord(Long userId, Integer change, String source) {
        UserCredit latest = userCreditMapper.selectOne(
                new LambdaQueryWrapper<UserCredit>().eq(UserCredit::getUserId, userId));
        int balance = latest == null || latest.getActiveCredits() == null ? 0 : latest.getActiveCredits();
        CreditRecord record = new CreditRecord();
        record.setUserId(userId);
        record.setChangeAmount(change);
        record.setType("exam-ai-grade");
        record.setSource(source);
        record.setBalance(balance);
        record.setCreateTime(new Date());
        creditRecordMapper.insert(record);
    }

    @Override
    public void customRemoveById(Integer id) {
        //重要的关联数据校验，有删除失败！
        //判断自身状态，进行中不能删除
        ExamRecord examRecord = getById(id);
        if ("进行中".equals(examRecord.getStatus())){
            throw new RuntimeException("正在考试中，无法直接删除！");
        }
        //删除自身数据，同时删除答题记录
        removeById(id);
        answerRecordService.remove(new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getExamRecordId,id));
    }

    @Override
    public List<ExamRankingVO> customGetRanking(Integer paperId, Integer limit) {
        return examRecordMapper.customQueryRanking(paperId,limit);
    }

    @Override
    public List<ExamRecord> customGetMyRecords(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .orderByDesc(ExamRecord::getStartTime));
    }

    @Override
    public int forceSettleGrading(Integer examRecordId) {
        ExamRecord examRecord = getById(examRecordId);
        if (examRecord == null) {
            return 0;
        }
        if ("已批阅".equals(examRecord.getStatus())) {
            return examRecord.getScore() == null ? 0 : examRecord.getScore();
        }
        // 按已保存的答题记录统计总分：客观题已判分，AI 失败的简答题为占位 0 分
        List<AnswerRecord> saved = answerRecordService.list(
                new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getExamRecordId, examRecordId));
        int forcedScore = saved == null ? 0 : saved.stream()
                .filter(a -> a.getScore() != null)
                .mapToInt(AnswerRecord::getScore)
                .sum();
        examRecord.setStatus("已批阅");
        examRecord.setScore(forcedScore);
        examRecord.setAnswers("AI 判卷多次重试仍失败，简答题暂按 0 分、成绩按客观题结算，建议稍后重考或联系老师复核。");
        updateById(examRecord);
        log.warn("AI 判卷强制结算 examRecordId={} forcedScore={}", examRecordId, forcedScore);
        return forcedScore;
    }


    /**
     * 标准化判断题答案，将T/F转换为TRUE/FALSE
     * @param answer 原始答案
     * @return 标准化后的答案
     */
    private String normalizeJudgeAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return "";
        }

        String normalized = answer.trim().toUpperCase();
        switch (normalized) {
            case "T":
            case "TRUE":
            case "正确":
                return "TRUE";
            case "F":
            case "FALSE":
            case "错":
                return "FALSE";
            default:
                return normalized;
        }
    }
}