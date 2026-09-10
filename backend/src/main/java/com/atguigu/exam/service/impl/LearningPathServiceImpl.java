package com.atguigu.exam.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.exam.entity.AnswerRecord;
import com.atguigu.exam.entity.Category;
import com.atguigu.exam.entity.ExamRecord;
import com.atguigu.exam.entity.InterviewQuestion;
import com.atguigu.exam.entity.LearningPath;
import com.atguigu.exam.entity.LearningPathNode;
import com.atguigu.exam.entity.Paper;
import com.atguigu.exam.entity.PaperQuestion;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.entity.UserPaper;
import com.atguigu.exam.mapper.AnswerRecordMapper;
import com.atguigu.exam.mapper.CategoryMapper;
import com.atguigu.exam.mapper.ExamRecordMapper;
import com.atguigu.exam.mapper.InterviewQuestionMapper;
import com.atguigu.exam.mapper.LearningPathMapper;
import com.atguigu.exam.mapper.LearningPathNodeMapper;
import com.atguigu.exam.mapper.PaperMapper;
import com.atguigu.exam.mapper.QuestionMapper;
import com.atguigu.exam.mapper.UserPaperMapper;
import com.atguigu.exam.service.LearningPathService;
import com.atguigu.exam.service.PaperQuestionService;
import com.atguigu.exam.service.PaperService;
import com.atguigu.exam.service.UserPaperService;
import com.atguigu.exam.vo.LearningPathDetailVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import java.math.BigDecimal;

/**
 * AI 学习路径服务实现（异步生成）
 * <p>
 * 生成流程：generatePath 立即返回（建 GENERATING 行 + 提交后台任务）→ 后台线程执行
 * 候选集检索 → 候选集注入 prompt → LLM 编排 → refId 合法性校验（防幻觉）→ 事务落库（ACTIVE）。
 * 用户可离开页面，前端轮询 GET /active 获取结果；失败置 FAILED（旧 ACTIVE 路径保留不受影响）。
 * AI 只做编排与讲解文本，节点 refId 只能从候选集中选择，规避 LLM 幻觉。
 */
@Slf4j
@Service
public class LearningPathServiceImpl implements LearningPathService {

    private static final int MAX_PAPER_CANDIDATES = 10;
    private static final int MAX_QUESTION_CANDIDATES = 20;
    private static final int QUESTION_CONTENT_LIMIT = 150;
    /** GENERATING 行超过该时长视为僵死（如服务重启导致任务丢失），允许重新发起 */
    private static final long GENERATING_STALE_MS = 10 * 60 * 1000L;

    /** 异步生成线程池：低频操作，2 个工作线程足够 */
    private static final ExecutorService GENERATOR_POOL = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "learning-path-generator");
        t.setDaemon(true);
        return t;
    });

    /**
     * 生成进度表：pathId -> 进度百分比（0-100，仅 GENERATING 期间存在）。
     * 内存态即可：生成结束/失败即移除；服务重启丢失时前端按默认值展示，僵死行由 GENERATING_STALE_MS 兜底。
     * 阶段刻度：10 已受理 → 30 候选集检索完成 → 70 AI 编排完成 → 90 校验落库中 → 结束移除
     */
    private static final ConcurrentHashMap<Long, Integer> GENERATION_PROGRESS = new ConcurrentHashMap<>();

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private AnswerRecordMapper answerRecordMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private UserPaperMapper userPaperMapper;

    @Autowired
    private InterviewQuestionMapper interviewQuestionMapper;

    @Autowired
    private LearningPathMapper learningPathMapper;

    @Autowired
    private LearningPathNodeMapper learningPathNodeMapper;

    @Autowired
    private PaperService paperService;

    @Autowired
    private PaperQuestionService paperQuestionService;

    @Autowired
    private UserPaperService userPaperService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    @Qualifier("paperChatModel")
    private ChatLanguageModel paperChatModel;

    @Override
    public LearningPathDetailVo generatePath(Long userId) {
        // 0. 已有进行中的生成任务 → 幂等返回，不重复提交
        LearningPath generating = findRowByStatus(userId, "GENERATING");
        if (generating != null) {
            // 超过 10 分钟仍在生成视为僵死（服务重启丢任务），清理后允许重新发起
            if (generating.getCreateTime() != null
                    && System.currentTimeMillis() - generating.getCreateTime().getTime() > GENERATING_STALE_MS) {
                learningPathMapper.deleteById(generating.getId());
            } else {
                return assembleDetail(generating);
            }
        }

        // 1. 清理历史失败行
        LearningPath failed = findRowByStatus(userId, "FAILED");
        if (failed != null) {
            learningPathMapper.deleteById(failed.getId());
        }

        // 2. 诊断聚合（快速查询，同步完成）：无答题数据时引导先考诊断卷
        List<LearningPathDetailVo.DiagnosisItemVo> diagnosis = buildDiagnosis(userId);
        if (diagnosis.isEmpty()) {
            throw new RuntimeException("暂无答题数据，请先完成一次考试或练习，AI 才能诊断薄弱点并规划学习路径");
        }

        // 3. 建 GENERATING 行并提交后台任务（立即返回，用户可离开页面）
        JSONArray diagnosisJson = new JSONArray();
        for (LearningPathDetailVo.DiagnosisItemVo item : diagnosis) {
            JSONObject obj = new JSONObject();
            obj.put("categoryName", item.getCategoryName());
            obj.put("answerCount", item.getAnswerCount());
            obj.put("correctRate", item.getCorrectRate());
            diagnosisJson.add(obj);
        }
        LearningPath path = new LearningPath();
        path.setUserId(userId);
        path.setStatus("GENERATING");
        path.setDiagnosisJson(diagnosisJson.toJSONString());
        path.setNodeCount(0);
        learningPathMapper.insert(path);

        final Long pathId = path.getId();
        final List<LearningPathDetailVo.DiagnosisItemVo> diagnosisFinal = diagnosis;
        GENERATION_PROGRESS.put(pathId, 10);
        GENERATOR_POOL.submit(() -> runGeneration(userId, pathId, diagnosisFinal));

        log.info("AI 学习路径后台生成已提交：userId={}, pathId={}", userId, pathId);
        return assembleDetail(path);
    }

    @Override
    public LearningPathDetailVo getActivePath(Long userId) {
        // 优先级：生成中 → 失败 → 当前生效路径（ACTIVE/COMPLETED）
        // 保证前端轮询能及时感知"生成中/失败"状态，不被旧路径遮蔽
        LearningPath path = findRowByStatus(userId, "GENERATING");
        if (path == null) {
            path = findRowByStatus(userId, "FAILED");
        }
        if (path == null) {
            path = learningPathMapper.selectOne(
                    new LambdaQueryWrapper<LearningPath>()
                            .eq(LearningPath::getUserId, userId)
                            .in(LearningPath::getStatus, "ACTIVE", "COMPLETED")
                            .orderByDesc(LearningPath::getId)
                            .last("LIMIT 1"));
        }
        if (path == null) {
            return null;
        }
        // 惰性同步：仅对生效路径（ACTIVE/COMPLETED）自动判定 PAPER 节点完成状态
        if ("ACTIVE".equals(path.getStatus()) || "COMPLETED".equals(path.getStatus())) {
            syncPaperNodes(path);
        }
        return assembleDetail(path);
    }

    /**
     * PAPER 节点完成状态惰性同步：
     * 该节点关联的试卷存在当前用户的已批阅考试记录（exam_records.score 非空）→ 节点自动置 COMPLETED。
     * 同步后若全部节点完成且路径仍为 ACTIVE → 路径自动置 COMPLETED。
     */
    private void syncPaperNodes(LearningPath path) {
        Long userId = path.getUserId();
        List<LearningPathNode> nodes = learningPathNodeMapper.selectList(
                new LambdaQueryWrapper<LearningPathNode>()
                        .eq(LearningPathNode::getPathId, path.getId()));
        if (nodes.isEmpty()) {
            return;
        }
        List<Long> pendingPaperRefs = nodes.stream()
                .filter(n -> "PAPER".equals(n.getNodeType()) && "PENDING".equals(n.getStatus())
                        && n.getRefId() != null)
                .map(LearningPathNode::getRefId)
                .collect(Collectors.toList());
        boolean changed = false;
        if (!pendingPaperRefs.isEmpty()) {
            // 该用户已批阅的试卷集合（isNotNull(score) 与学习分析页口径一致）
            Set<Integer> gradedPaperIds = examRecordMapper.selectList(
                            new LambdaQueryWrapper<ExamRecord>()
                                    .eq(ExamRecord::getUserId, userId)
                                    .isNotNull(ExamRecord::getScore)
                                    .in(ExamRecord::getExamId, pendingPaperRefs)
                                    .select(ExamRecord::getExamId))
                    .stream()
                    .map(ExamRecord::getExamId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            for (LearningPathNode node : nodes) {
                if ("PAPER".equals(node.getNodeType()) && "PENDING".equals(node.getStatus())
                        && node.getRefId() != null
                        && gradedPaperIds.contains(node.getRefId().intValue())) {
                    node.setStatus("COMPLETED");
                    learningPathNodeMapper.updateById(node);
                    changed = true;
                    log.info("PAPER 节点自动完成同步：pathId={}, nodeId={}, paperId={}",
                            path.getId(), node.getId(), node.getRefId());
                }
            }
        }
        // 全节点完成 → 路径置 COMPLETED
        boolean allCompleted = nodes.stream().allMatch(n -> "COMPLETED".equals(n.getStatus()));
        if (allCompleted && !"COMPLETED".equals(path.getStatus())) {
            LearningPath update = new LearningPath();
            update.setId(path.getId());
            update.setStatus("COMPLETED");
            learningPathMapper.updateById(update);
            path.setStatus("COMPLETED");
            log.info("学习路径全部节点完成，自动置 COMPLETED：pathId={}", path.getId());
        } else if (changed && "COMPLETED".equals(path.getStatus())) {
            // 已完成路径下出现未完成节点（理论上不发生），防御性回退
            LearningPath update = new LearningPath();
            update.setId(path.getId());
            update.setStatus("ACTIVE");
            learningPathMapper.updateById(update);
            path.setStatus("ACTIVE");
        }
    }

    @Override
    public void toggleNode(Long userId, Long nodeId) {
        LearningPathNode node = learningPathNodeMapper.selectById(nodeId);
        if (node == null) {
            throw new RuntimeException("节点不存在");
        }
        LearningPath path = learningPathMapper.selectById(node.getPathId());
        if (path == null || !userId.equals(path.getUserId())) {
            throw new RuntimeException("无权操作该节点");
        }
        if (!"ACTIVE".equals(path.getStatus()) && !"COMPLETED".equals(path.getStatus())) {
            throw new RuntimeException("当前路径状态不允许操作节点");
        }
        if ("PAPER".equals(node.getNodeType())) {
            throw new RuntimeException("试卷节点由系统自动判定完成：完成该卷考试并等批阅后自动勾选");
        }
        boolean toCompleted = !"COMPLETED".equals(node.getStatus());
        LearningPathNode update = new LearningPathNode();
        update.setId(nodeId);
        update.setStatus(toCompleted ? "COMPLETED" : "PENDING");
        learningPathNodeMapper.updateById(update);

        // 重算路径完成状态
        Long total = learningPathNodeMapper.selectCount(
                new LambdaQueryWrapper<LearningPathNode>()
                        .eq(LearningPathNode::getPathId, path.getId()));
        Long done = learningPathNodeMapper.selectCount(
                new LambdaQueryWrapper<LearningPathNode>()
                        .eq(LearningPathNode::getPathId, path.getId())
                        .eq(LearningPathNode::getStatus, "COMPLETED"));
        String newStatus = total > 0 && done.equals(total) ? "COMPLETED" : "ACTIVE";
        if (!newStatus.equals(path.getStatus())) {
            LearningPath pathUpdate = new LearningPath();
            pathUpdate.setId(path.getId());
            pathUpdate.setStatus(newStatus);
            learningPathMapper.updateById(pathUpdate);
            path.setStatus(newStatus);
        }
        log.info("学习路径节点手动切换：userId={}, nodeId={}, -> {}", userId, nodeId, update.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateDiagnosticPaper(Long userId) {
        // 题库随机抽 10 道（小表 ORDER BY RAND() 可接受），覆盖各分类
        List<Question> samples = questionMapper.selectList(
                new LambdaQueryWrapper<Question>().last("ORDER BY RAND() LIMIT 10"));
        if (samples.size() < 5) {
            throw new RuntimeException("题库题目不足，无法生成诊断卷，请先完成一次考试或练习");
        }
        // 组卷（复用 AI 卷落库规范：DRAFT + user_paper 归属，开考时 startExam 做归属校验）
        Paper paper = new Paper();
        paper.setName("综合诊断卷-" + new java.text.SimpleDateFormat("MMddHHmm").format(new Date()));
        paper.setDescription("AI 学习路径配套诊断卷：混合抽取各知识点题目，考完后 AI 将据此诊断你的薄弱知识点并规划学习路径。");
        paper.setStatus("DRAFT");
        paper.setDuration(30);
        BigDecimal totalScore = samples.stream()
                .map(q -> BigDecimal.valueOf(q.getScore() == null ? 5 : q.getScore()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        paper.setTotalScore(totalScore);
        paper.setQuestionCount(samples.size());
        paperService.save(paper);

        for (Question question : samples) {
            paperQuestionService.save(new PaperQuestion(paper.getId().intValue(), question.getId(),
                    BigDecimal.valueOf(question.getScore() == null ? 5 : question.getScore())));
        }
        UserPaper userPaper = new UserPaper();
        userPaper.setUserId(userId);
        userPaper.setPaperId(paper.getId());
        userPaper.setRelationType(UserPaperServiceImpl.RELATION_AI_GENERATED);
        userPaperService.save(userPaper);

        log.info("综合诊断卷已生成：userId={}, paperId={}, {} 道题, 总分 {}", userId, paper.getId(),
                samples.size(), totalScore);
        return paper.getId();
    }

    /**
     * 后台执行：候选集检索 → LLM 编排 → 校验 → 事务落库。
     * 任意失败将 GENERATING 行置 FAILED（携带失败原因），旧 ACTIVE 路径保留不受影响。
     */
    private void runGeneration(Long userId, Long pathId, List<LearningPathDetailVo.DiagnosisItemVo> diagnosis) {
        try {
            // 1. 候选集检索
            List<Paper> paperCandidates = findPaperCandidates(userId);
            List<InterviewQuestion> questionCandidates = findQuestionCandidates();
            GENERATION_PROGRESS.put(pathId, 30);

            // 2. LLM 编排（30-90s）
            String content = paperChatModel.chat(buildPathPrompt(diagnosis, paperCandidates, questionCandidates));
            GENERATION_PROGRESS.put(pathId, 70);
            JSONObject json = parsePathJson(content);
            if (json == null) {
                throw new RuntimeException("AI 返回的学习路径格式异常，请重试");
            }
            JSONArray phases = json.getJSONArray("phases");
            if (phases == null || phases.isEmpty()) {
                throw new RuntimeException("AI 返回的学习路径为空，请重试");
            }

            // 3. refId 合法性校验（不在候选集内的 PAPER/QUESTION 节点降级为 KNOWLEDGE）
            Set<Long> paperIdSet = paperCandidates.stream()
                    .map(Paper::getId).collect(Collectors.toSet());
            Set<Long> questionIdSet = questionCandidates.stream()
                    .map(InterviewQuestion::getId).collect(Collectors.toSet());

            // 4. 事务落库：归档旧生效路径（ACTIVE/COMPLETED 均归档）+ 激活新路径 + 插入节点
            GENERATION_PROGRESS.put(pathId, 90);
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            int nodeCount = txTemplate.execute(status -> {
                List<LearningPathNode> nodes = buildNodes(phases, paperIdSet, questionIdSet);
                if (nodes.isEmpty()) {
                    throw new RuntimeException("AI 返回的学习路径节点为空，请重试");
                }
                learningPathMapper.update(null, new LambdaUpdateWrapper<LearningPath>()
                        .eq(LearningPath::getUserId, userId)
                        .in(LearningPath::getStatus, "ACTIVE", "COMPLETED")
                        .set(LearningPath::getStatus, "OUTDATED"));
                LearningPath update = new LearningPath();
                update.setId(pathId);
                update.setStatus("ACTIVE");
                update.setSummary(json.getString("summary"));
                update.setNodeCount(nodes.size());
                learningPathMapper.updateById(update);
                for (LearningPathNode node : nodes) {
                    node.setPathId(pathId);
                    learningPathNodeMapper.insert(node);
                }
                return nodes.size();
            });
            log.info("AI 学习路径后台生成成功：userId={}, pathId={}, {} 个节点", userId, pathId, nodeCount);
        } catch (Exception e) {
            log.error("AI 学习路径后台生成失败：userId={}, pathId={}, 原因：{}", userId, pathId, e.getMessage());
            LearningPath failed = new LearningPath();
            failed.setId(pathId);
            failed.setStatus("FAILED");
            failed.setSummary("生成失败：" + (e.getMessage() == null ? "未知异常" : e.getMessage()));
            learningPathMapper.updateById(failed);
        } finally {
            // 生成结束（成功/失败）即清理进度，状态已离开 GENERATING，前端不再读取进度
            GENERATION_PROGRESS.remove(pathId);
        }
    }

    /**
     * 校验并组装节点列表（非法 refId / 未知类型降级为 KNOWLEDGE 讲解节点）
     */
    private List<LearningPathNode> buildNodes(JSONArray phases, Set<Long> paperIdSet, Set<Long> questionIdSet) {
        List<LearningPathNode> nodes = new ArrayList<>();
        int sortOrder = 0;
        int phaseIndex = 0;
        for (int i = 0; i < phases.size(); i++) {
            JSONObject phaseJson = phases.getJSONObject(i);
            if (phaseJson == null) {
                continue;
            }
            phaseIndex++;
            String phaseTitle = phaseJson.getString("phaseTitle");
            JSONArray nodeArray = phaseJson.getJSONArray("nodes");
            if (nodeArray == null || nodeArray.isEmpty()) {
                continue;
            }
            for (int j = 0; j < nodeArray.size(); j++) {
                JSONObject nodeJson = nodeArray.getJSONObject(j);
                if (nodeJson == null) {
                    continue;
                }
                LearningPathNode node = new LearningPathNode();
                node.setPhase(phaseIndex);
                node.setPhaseTitle(phaseTitle);
                node.setSortOrder(sortOrder++);
                node.setStatus("PENDING");
                node.setTitle(trimTo(nodeJson.getString("title"), 200));
                node.setDescription(nodeJson.getString("description"));

                String nodeType = nodeJson.getString("nodeType");
                Long refId = nodeJson.getLong("refId");
                if ("PAPER".equals(nodeType) && refId != null && paperIdSet.contains(refId)) {
                    node.setNodeType("PAPER");
                    node.setRefId(refId);
                } else if ("QUESTION".equals(nodeType) && refId != null && questionIdSet.contains(refId)) {
                    node.setNodeType("QUESTION");
                    node.setRefId(refId);
                } else {
                    // 非法 refId / 未知类型 → 降级为知识点讲解节点
                    node.setNodeType("KNOWLEDGE");
                    node.setRefId(null);
                }
                if ("KNOWLEDGE".equals(node.getNodeType())) {
                    node.setCategoryName(trimTo(nodeJson.getString("categoryName"), 100));
                }
                if (node.getTitle() == null || node.getTitle().isEmpty()) {
                    continue;
                }
                nodes.add(node);
            }
        }
        return nodes;
    }

    /**
     * 查询用户某状态的单条路径（最新一条）
     */
    private LearningPath findRowByStatus(Long userId, String status) {
        return learningPathMapper.selectOne(
                new LambdaQueryWrapper<LearningPath>()
                        .eq(LearningPath::getUserId, userId)
                        .eq(LearningPath::getStatus, status)
                        .orderByDesc(LearningPath::getId)
                        .last("LIMIT 1"));
    }

    /**
     * 诊断聚合：与学习分析页同口径（已出分考试 → 答题记录 → 按题目分类聚合得分率）
     * 返回按得分率升序（最薄弱在前）
     */
    private List<LearningPathDetailVo.DiagnosisItemVo> buildDiagnosis(Long userId) {
        List<ExamRecord> examRecords = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, userId)
                        .isNotNull(ExamRecord::getScore));
        if (examRecords.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> examRecordIds = examRecords.stream()
                .map(ExamRecord::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<AnswerRecord> answerRecords = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>()
                        .in(AnswerRecord::getExamRecordId, examRecordIds));
        if (answerRecords.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Question> questionMap = buildQuestionMap(answerRecords);
        Map<String, int[]> masteryMap = new LinkedHashMap<>();
        Map<Long, Category> categoryCache = new HashMap<>();
        for (AnswerRecord record : answerRecords) {
            if (record.getQuestionId() == null) {
                continue;
            }
            Question question = questionMap.get(record.getQuestionId().longValue());
            if (question == null) {
                continue;
            }
            Long categoryId = question.getCategoryId();
            if (categoryId == null) {
                categoryId = 0L;
            }
            Category category = categoryCache.get(categoryId);
            if (category == null) {
                category = categoryId == 0L ? null : categoryMapper.selectById(categoryId);
                categoryCache.put(categoryId, category);
            }
            String categoryName = category != null && category.getName() != null
                    ? normalizeName(category.getName()) : "未分类";
            int[] agg = masteryMap.computeIfAbsent(categoryName, k -> new int[3]);
            agg[0]++; // answerCount
            agg[1] += record.getScore() == null ? 0 : record.getScore(); // earned
            agg[2] += question.getScore() == null ? 0 : question.getScore(); // max
        }

        List<LearningPathDetailVo.DiagnosisItemVo> result = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : masteryMap.entrySet()) {
            int[] agg = entry.getValue();
            if (agg[2] <= 0) {
                continue;
            }
            LearningPathDetailVo.DiagnosisItemVo item = new LearningPathDetailVo.DiagnosisItemVo();
            item.setCategoryName(entry.getKey());
            item.setAnswerCount(agg[0]);
            item.setCorrectRate(Math.round(agg[1] * 100f / agg[2]));
            result.add(item);
        }
        result.sort(Comparator.comparingInt(LearningPathDetailVo.DiagnosisItemVo::getCorrectRate));
        return result;
    }

    /**
     * 试卷候选：PUBLISHED 公开卷 + 用户自己的卷（user_paper 关联，含 DRAFT AI 卷）
     */
    private List<Paper> findPaperCandidates(Long userId) {
        Map<Long, Paper> candidates = new LinkedHashMap<>();
        List<Paper> published = paperMapper.selectList(
                new LambdaQueryWrapper<Paper>().eq(Paper::getStatus, "PUBLISHED"));
        for (Paper paper : published) {
            candidates.put(paper.getId(), paper);
        }
        List<UserPaper> userPapers = userPaperMapper.selectList(
                new LambdaQueryWrapper<UserPaper>().eq(UserPaper::getUserId, userId));
        if (!userPapers.isEmpty()) {
            Set<Long> ownedIds = userPapers.stream()
                    .map(UserPaper::getPaperId).filter(Objects::nonNull).collect(Collectors.toSet());
            ownedIds.removeAll(candidates.keySet());
            if (!ownedIds.isEmpty()) {
                for (Paper paper : paperMapper.selectBatchIds(ownedIds)) {
                    candidates.put(paper.getId(), paper);
                }
            }
        }
        return candidates.values().stream()
                .limit(MAX_PAPER_CANDIDATES)
                .collect(Collectors.toList());
    }

    /**
     * 真题候选：已审核通过的企业真题（按浏览量降序取前 N）
     */
    private List<InterviewQuestion> findQuestionCandidates() {
        return interviewQuestionMapper.selectList(
                new LambdaQueryWrapper<InterviewQuestion>()
                        .eq(InterviewQuestion::getStatus, "approved")
                        .orderByDesc(InterviewQuestion::getViewCount)
                        .last("LIMIT " + MAX_QUESTION_CANDIDATES));
    }

    /**
     * 组装候选集注入 + AI 编排提示词
     */
    private String buildPathPrompt(List<LearningPathDetailVo.DiagnosisItemVo> diagnosis,
                                   List<Paper> paperCandidates,
                                   List<InterviewQuestion> questionCandidates) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名资深 IT 学习规划专家，请根据用户的答题诊断数据，为其规划一份分阶段、可执行的学习路径。\n\n");
        prompt.append("【用户知识点掌握度诊断】（按得分率升序=最薄弱在前）\n");
        for (int i = 0; i < diagnosis.size(); i++) {
            LearningPathDetailVo.DiagnosisItemVo item = diagnosis.get(i);
            prompt.append(i + 1).append(". ").append(item.getCategoryName())
                    .append("：得分率 ").append(item.getCorrectRate())
                    .append("%，答题 ").append(item.getAnswerCount()).append(" 道\n");
        }
        prompt.append("\n【试卷候选】（nodeType=PAPER 时 refId 只能从此列表选择）\n");
        if (paperCandidates.isEmpty()) {
            prompt.append("（无，禁止使用 PAPER 类型节点）\n");
        } else {
            for (Paper paper : paperCandidates) {
                prompt.append("- id=").append(paper.getId()).append(" 「").append(paper.getName())
                        .append("」").append(paper.getQuestionCount()).append("题\n");
            }
        }
        prompt.append("\n【企业真题候选】（nodeType=QUESTION 时 refId 只能从此列表选择）\n");
        if (questionCandidates.isEmpty()) {
            prompt.append("（无，禁止使用 QUESTION 类型节点）\n");
        } else {
            for (InterviewQuestion question : questionCandidates) {
                String content = question.getQuestionContent() == null ? "" : question.getQuestionContent();
                if (content.length() > QUESTION_CONTENT_LIMIT) {
                    content = content.substring(0, QUESTION_CONTENT_LIMIT) + "...";
                }
                content = content.replaceAll("\\s+", " ");
                prompt.append("- id=").append(question.getId()).append(" 「").append(content)
                        .append("」难度 ").append(question.getDifficultyLevel()).append("\n");
            }
        }
        prompt.append("\n规划要求：\n");
        prompt.append("1. 共 2-3 个阶段（phases），每阶段 2-4 个节点，最薄弱的知识点排在第一阶段\n");
        prompt.append("2. 节点类型说明：\n");
        prompt.append("   - KNOWLEDGE：知识点讲解节点。title=知识点名称，description=该知识点 200-400 字的核心讲解（面向已学过但掌握不牢的学生，突出易错点与关键结论），categoryName=所属知识点分类名（从诊断列表里选），refId 必须为 null\n");
        prompt.append("   - PAPER：试卷实战节点。refId 只能从【试卷候选】中选 id，title=试卷名，description=完成该卷的目标与注意事项\n");
        prompt.append("   - QUESTION：企业真题节点。refId 只能从【真题候选】中选 id，title=题目主题概括，description=该题的作答建议与考察意图\n");
        prompt.append("3. 每个薄弱知识点先给一个 KNOWLEDGE 讲解节点，再跟实战节点（PAPER 或 QUESTION）巩固\n");
        prompt.append("4. summary 为整体规划思路，80 字以内\n\n");
        prompt.append("请严格按照以下 JSON 格式返回，禁止包含任何其他文字：\n");
        prompt.append("{\"summary\":\"...\",\"phases\":[{\"phaseTitle\":\"第一阶段：...\",\"nodes\":[{\"nodeType\":\"KNOWLEDGE\",\"refId\":null,\"title\":\"...\",\"description\":\"...\",\"categoryName\":\"...\"},{\"nodeType\":\"PAPER\",\"refId\":2,\"title\":\"...\",\"description\":\"...\",\"categoryName\":null}]}]}\n");
        return prompt.toString();
    }

    /**
     * 解析 LLM 返回的路径 JSON（剥离 ```json 围栏）
     */
    private JSONObject parsePathJson(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        String jsonText;
        int startIndex = content.indexOf("```json");
        int endIndex = content.lastIndexOf("```");
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            jsonText = content.substring(startIndex + 7, endIndex).trim();
        } else {
            int braceStart = content.indexOf("{");
            int braceEnd = content.lastIndexOf("}");
            if (braceStart == -1 || braceEnd <= braceStart) {
                return null;
            }
            jsonText = content.substring(braceStart, braceEnd + 1);
        }
        try {
            return JSONObject.parseObject(jsonText);
        } catch (Exception e) {
            log.warn("学习路径 JSON 解析失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 组装路径详情（按阶段分组节点）
     */
    private LearningPathDetailVo assembleDetail(LearningPath path) {
        LearningPathDetailVo detail = new LearningPathDetailVo();
        detail.setId(path.getId());
        detail.setStatus(path.getStatus());
        detail.setSummary(path.getSummary());
        detail.setNodeCount(path.getNodeCount());
        detail.setCreateTime(path.getCreateTime());
        // 生成进度：仅 GENERATING 期间返回（Map 丢失如服务重启时按 10% 兜底，僵死行由超时清理兜底）
        if ("GENERATING".equals(path.getStatus())) {
            detail.setProgress(GENERATION_PROGRESS.getOrDefault(path.getId(), 10));
        }

        // 诊断快照
        List<LearningPathDetailVo.DiagnosisItemVo> diagnosis = new ArrayList<>();
        if (path.getDiagnosisJson() != null) {
            try {
                JSONArray arr = JSONArray.parseArray(path.getDiagnosisJson());
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    LearningPathDetailVo.DiagnosisItemVo item = new LearningPathDetailVo.DiagnosisItemVo();
                    item.setCategoryName(obj.getString("categoryName"));
                    item.setAnswerCount(obj.getIntValue("answerCount"));
                    item.setCorrectRate(obj.getIntValue("correctRate"));
                    diagnosis.add(item);
                }
            } catch (Exception e) {
                log.warn("诊断快照解析失败：{}", e.getMessage());
            }
        }
        detail.setDiagnosis(diagnosis);

        // 节点
        List<LearningPathNode> nodes = learningPathNodeMapper.selectList(
                new LambdaQueryWrapper<LearningPathNode>()
                        .eq(LearningPathNode::getPathId, path.getId())
                        .orderByAsc(LearningPathNode::getSortOrder));
        Map<Integer, LearningPathDetailVo.PhaseVo> phaseMap = new LinkedHashMap<>();
        int completed = 0;
        for (LearningPathNode node : nodes) {
            if ("COMPLETED".equals(node.getStatus())) {
                completed++;
            }
            LearningPathDetailVo.PhaseVo phase = phaseMap.computeIfAbsent(node.getPhase(), p -> {
                LearningPathDetailVo.PhaseVo vo = new LearningPathDetailVo.PhaseVo();
                vo.setPhase(p);
                vo.setNodes(new ArrayList<>());
                return vo;
            });
            phase.setPhaseTitle(node.getPhaseTitle());
            LearningPathDetailVo.NodeVo nodeVo = new LearningPathDetailVo.NodeVo();
            nodeVo.setId(node.getId());
            nodeVo.setNodeType(node.getNodeType());
            nodeVo.setRefId(node.getRefId());
            nodeVo.setCategoryName(node.getCategoryName());
            nodeVo.setTitle(node.getTitle());
            nodeVo.setDescription(node.getDescription());
            nodeVo.setStatus(node.getStatus());
            phase.getNodes().add(nodeVo);
        }
        detail.setCompletedCount(completed);
        detail.setPhases(new ArrayList<>(phaseMap.values()));
        return detail;
    }

    /**
     * 查询答题记录涉及的全部题目，构建题目ID -> 题目映射
     */
    private Map<Long, Question> buildQuestionMap(List<AnswerRecord> answerRecords) {
        Set<Long> questionIds = answerRecords.stream()
                .map(AnswerRecord::getQuestionId).filter(Objects::nonNull)
                .map(Long::valueOf).collect(Collectors.toSet());
        if (questionIds.isEmpty()) {
            return new HashMap<>();
        }
        return questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
    }

    /**
     * 清洗展示文本：去除 markdown 强调符并压缩空白，空值回退"未分类"
     */
    private String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "未分类";
        }
        return name.replaceAll("[*_#`~]+", " ").replaceAll("\\s+", " ").trim();
    }

    private String trimTo(String text, int limit) {
        if (text == null) {
            return null;
        }
        return text.length() > limit ? text.substring(0, limit) : text;
    }
}
