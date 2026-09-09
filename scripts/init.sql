-- ============================================================
-- 智能学习平台（edu-ai-path-master）数据库初始化脚本
-- 数据库：exam_system_0625
-- 说明：
--   1. 表结构与后端 day23 实体类一一对应（MyBatis-Plus 驼峰转下划线）
--   2. 聊天记录表 chat_messages 存储在 MongoDB（chat_memory_db），不在本脚本
--   3. 默认管理员：admin / 123456（BCrypt 加密，角色 ADMIN）
-- 使用方法：mysql -uroot -p < scripts/init.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS exam_system_0625
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE exam_system_0625;

-- ----------------------------------------
-- 用户表
-- ----------------------------------------
DROP TABLE IF EXISTS users;
CREATE TABLE users (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username    VARCHAR(50)  NOT NULL COMMENT '用户名，用于登录',
  password    VARCHAR(100) NOT NULL COMMENT '用户密码（BCrypt 加密）',
  real_name   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
  role        VARCHAR(20)  DEFAULT 'STUDENT' COMMENT '角色：ADMIN/TEACHER/STUDENT',
  status      VARCHAR(20)  DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted  TINYINT      DEFAULT 0 COMMENT '逻辑删除：0-正常，1-删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT = '用户表';

-- ----------------------------------------
-- 题目分类表
-- ----------------------------------------
DROP TABLE IF EXISTS categories;
CREATE TABLE categories (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  name        VARCHAR(100) NOT NULL COMMENT '分类名称',
  parent_id   BIGINT       DEFAULT 0 COMMENT '父分类ID，顶级为0',
  sort        INT          DEFAULT 0 COMMENT '排序序号',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted  TINYINT      DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '题目分类表';

-- ----------------------------------------
-- 题目表
-- ----------------------------------------
DROP TABLE IF EXISTS questions;
CREATE TABLE questions (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  title       TEXT         NOT NULL COMMENT '题干内容',
  type        VARCHAR(20)  NOT NULL COMMENT '题型：CHOICE/JUDGE/TEXT',
  multi       TINYINT(1)   DEFAULT 0 COMMENT '是否多选题',
  category_id BIGINT       DEFAULT NULL COMMENT '分类ID',
  difficulty  VARCHAR(20)  DEFAULT 'MEDIUM' COMMENT '难度：EASY/MEDIUM/HARD',
  score       INT          DEFAULT 5 COMMENT '默认分值',
  analysis    TEXT         DEFAULT NULL COMMENT '题目解析',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted  TINYINT      DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_category (category_id),
  KEY idx_type (type)
) ENGINE = InnoDB COMMENT = '题目表';

-- ----------------------------------------
-- 题目选项表（选择题）
-- ----------------------------------------
DROP TABLE IF EXISTS question_choices;
CREATE TABLE question_choices (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  question_id BIGINT      NOT NULL COMMENT '题目ID',
  content     VARCHAR(500) NOT NULL COMMENT '选项内容',
  is_correct  TINYINT(1)  DEFAULT 0 COMMENT '是否正确答案',
  sort        INT         DEFAULT 0 COMMENT '选项排序',
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted  TINYINT     DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_question (question_id)
) ENGINE = InnoDB COMMENT = '题目选项表';

-- ----------------------------------------
-- 题目答案表
-- ----------------------------------------
DROP TABLE IF EXISTS question_answers;
CREATE TABLE question_answers (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  question_id BIGINT       NOT NULL COMMENT '题目ID',
  answer      VARCHAR(1000) DEFAULT NULL COMMENT '标准答案',
  keywords    VARCHAR(500) DEFAULT NULL COMMENT '评分关键词（简答题，逗号分隔）',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted  TINYINT      DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_question (question_id)
) ENGINE = InnoDB COMMENT = '题目答案表';

-- ----------------------------------------
-- 试卷表
-- ----------------------------------------
DROP TABLE IF EXISTS paper;
CREATE TABLE paper (
  id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  name           VARCHAR(200)  NOT NULL COMMENT '试卷名称',
  description    TEXT          DEFAULT NULL COMMENT '试卷描述',
  status         VARCHAR(20)   DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/STOPPED',
  total_score    DECIMAL(10,2) DEFAULT 0 COMMENT '总分',
  question_count INT           DEFAULT 0 COMMENT '题目数量',
  duration       INT           DEFAULT 120 COMMENT '考试时长（分钟）',
  create_time    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted     TINYINT       DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '试卷表';

-- ----------------------------------------
-- 试卷-题目关联表
-- ----------------------------------------
DROP TABLE IF EXISTS paper_question;
CREATE TABLE paper_question (
  id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  paper_id    BIGINT        NOT NULL COMMENT '试卷ID',
  question_id BIGINT        NOT NULL COMMENT '题目ID',
  score       DECIMAL(10,2) DEFAULT NULL COMMENT '该题分值',
  create_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted  TINYINT       DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_paper (paper_id),
  KEY idx_question (question_id)
) ENGINE = InnoDB COMMENT = '试卷题目关联表';

-- ----------------------------------------
-- 考试（考试批次）表
-- ----------------------------------------
DROP TABLE IF EXISTS exams;
CREATE TABLE exams (
  id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  name           VARCHAR(200) NOT NULL COMMENT '考试名称',
  description    TEXT        DEFAULT NULL COMMENT '考试描述',
  duration       INT         DEFAULT 120 COMMENT '考试时长（分钟）',
  pass_score     INT         DEFAULT 60 COMMENT '及格分数',
  total_score    INT         DEFAULT 100 COMMENT '总分',
  question_count INT         DEFAULT 0 COMMENT '题目数量',
  status         VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/CLOSED',
  create_time    DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted     TINYINT     DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '考试表';

-- ----------------------------------------
-- 考试记录表
-- ----------------------------------------
DROP TABLE IF EXISTS exam_records;
CREATE TABLE exam_records (
  id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  exam_id         BIGINT      NOT NULL COMMENT '试卷ID',
  user_id         BIGINT      DEFAULT NULL COMMENT '关联用户ID（登录用户考试时记录，NULL 表示游客按姓名登记）',
  student_name    VARCHAR(50) DEFAULT NULL COMMENT '考生姓名',
  score           INT         DEFAULT NULL COMMENT '得分',
  answers         TEXT        DEFAULT NULL COMMENT '答题记录（JSON）',
  start_time      DATETIME    DEFAULT NULL COMMENT '开始时间',
  end_time        DATETIME    DEFAULT NULL COMMENT '结束时间',
  status          VARCHAR(20) DEFAULT '进行中' COMMENT '状态：进行中/已完成/已批阅',
  window_switches INT         DEFAULT 0 COMMENT '窗口切换次数',
  create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted      TINYINT     DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_exam (exam_id)
) ENGINE = InnoDB COMMENT = '考试记录表';

-- ----------------------------------------
-- 答题记录表
-- ----------------------------------------
DROP TABLE IF EXISTS answer_record;
CREATE TABLE answer_record (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  exam_record_id BIGINT       NOT NULL COMMENT '考试记录ID',
  question_id    BIGINT       NOT NULL COMMENT '题目ID',
  user_answer    VARCHAR(2000) DEFAULT NULL COMMENT '考生答案',
  score          INT          DEFAULT NULL COMMENT '该题得分',
  is_correct     TINYINT      DEFAULT NULL COMMENT '是否正确：0-错误，1-正确，2-部分正确',
  ai_correction  TEXT         DEFAULT NULL COMMENT 'AI 批改意见',
  create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_exam_record (exam_record_id)
) ENGINE = InnoDB COMMENT = '答题记录表';

-- ----------------------------------------
-- 轮播图表
-- ----------------------------------------
DROP TABLE IF EXISTS banners;
CREATE TABLE banners (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  title       VARCHAR(200) DEFAULT NULL COMMENT '轮播图标题',
  description VARCHAR(500) DEFAULT NULL COMMENT '轮播图描述',
  image_url   VARCHAR(500) DEFAULT NULL COMMENT '图片URL',
  link_url    VARCHAR(500) DEFAULT NULL COMMENT '跳转链接',
  sort_order  INT          DEFAULT 0 COMMENT '排序顺序',
  is_active   TINYINT(1)   DEFAULT 1 COMMENT '是否启用',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted  TINYINT      DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '轮播图表';

-- ----------------------------------------
-- 公告表
-- ----------------------------------------
DROP TABLE IF EXISTS notices;
CREATE TABLE notices (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  title       VARCHAR(200) NOT NULL COMMENT '公告标题',
  content     TEXT         DEFAULT NULL COMMENT '公告内容',
  type        VARCHAR(20)  DEFAULT 'NOTICE' COMMENT '类型：SYSTEM/FEATURE/NOTICE',
  priority    INT          DEFAULT 0 COMMENT '优先级：0-普通，1-重要，2-紧急',
  is_active   TINYINT(1)   DEFAULT 1 COMMENT '是否启用',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted  TINYINT      DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '公告表';

-- ----------------------------------------
-- 视频表
-- ----------------------------------------
DROP TABLE IF EXISTS videos;
CREATE TABLE videos (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  title           VARCHAR(200) NOT NULL COMMENT '视频标题',
  description     TEXT         DEFAULT NULL COMMENT '视频描述',
  category_id     BIGINT       DEFAULT NULL COMMENT '分类ID',
  file_url        VARCHAR(500) DEFAULT NULL COMMENT '视频文件URL',
  cover_url       VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL',
  duration        INT          DEFAULT 0 COMMENT '视频时长（秒）',
  file_size       BIGINT       DEFAULT 0 COMMENT '文件大小（字节）',
  uploader_name   VARCHAR(50)  DEFAULT NULL COMMENT '上传者名称',
  uploader_type   TINYINT      DEFAULT 2 COMMENT '上传者类型：1-用户投稿，2-管理员上传',
  user_id         BIGINT       DEFAULT NULL COMMENT '上传用户ID',
  admin_id        BIGINT       DEFAULT NULL COMMENT '上传管理员ID',
  status          TINYINT      DEFAULT 0 COMMENT '状态：0-待审核，1-已发布，2-已拒绝，3-已下架',
  audit_admin_id  BIGINT       DEFAULT NULL COMMENT '审核管理员ID',
  audit_time      DATETIME     DEFAULT NULL COMMENT '审核时间',
  audit_reason    VARCHAR(500) DEFAULT NULL COMMENT '审核原因',
  view_count      BIGINT       DEFAULT 0 COMMENT '观看次数',
  like_count      BIGINT       DEFAULT 0 COMMENT '点赞次数',
  tags            VARCHAR(500) DEFAULT NULL COMMENT '标签，逗号分隔',
  created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_category (category_id),
  KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '视频表';

-- ----------------------------------------
-- 视频分类表
-- ----------------------------------------
DROP TABLE IF EXISTS video_categories;
CREATE TABLE video_categories (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  name        VARCHAR(100) NOT NULL COMMENT '分类名称',
  description VARCHAR(500) DEFAULT NULL COMMENT '分类描述',
  parent_id   BIGINT       DEFAULT 0 COMMENT '父级分类ID，0为顶级',
  sort_order  INT          DEFAULT 0 COMMENT '排序权重',
  status      TINYINT      DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '视频分类表';

-- ----------------------------------------
-- 视频点赞表
-- ----------------------------------------
DROP TABLE IF EXISTS video_likes;
CREATE TABLE video_likes (
  id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  video_id   BIGINT      NOT NULL COMMENT '视频ID',
  user_ip    VARCHAR(50) DEFAULT NULL COMMENT '用户IP',
  user_agent VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
  created_at DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (id),
  KEY idx_video (video_id)
) ENGINE = InnoDB COMMENT = '视频点赞表';

-- ----------------------------------------
-- 视频观看记录表
-- ----------------------------------------
DROP TABLE IF EXISTS video_views;
CREATE TABLE video_views (
  id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  video_id      BIGINT      NOT NULL COMMENT '视频ID',
  user_ip       VARCHAR(50) DEFAULT NULL COMMENT '用户IP',
  user_agent    VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
  view_duration INT         DEFAULT 0 COMMENT '观看时长（秒）',
  created_at    DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '观看时间',
  PRIMARY KEY (id),
  KEY idx_video (video_id)
) ENGINE = InnoDB COMMENT = '视频观看记录表';

-- ============================================================
-- 种子数据
-- ============================================================

-- 默认管理员：admin / 123456
INSERT INTO users (username, password, real_name, role, status) VALUES
('admin', '$2a$10$l/c1YRUJ.35tyfhmI3zmfulU.NjXXs1b2a3lFT24KeAVUeFowjKDu', '管理员', 'ADMIN', 'ACTIVE');

-- 测试用户：teacher / 123456、student / 123456
INSERT INTO users (username, password, real_name, role, status) VALUES
('teacher', '$2a$10$l/c1YRUJ.35tyfhmI3zmfulU.NjXXs1b2a3lFT24KeAVUeFowjKDu', '教师', 'TEACHER', 'ACTIVE'),
('student', '$2a$10$l/c1YRUJ.35tyfhmI3zmfulU.NjXXs1b2a3lFT24KeAVUeFowjKDu', '学生', 'STUDENT', 'ACTIVE');

-- 题目分类
INSERT INTO categories (name, parent_id, sort) VALUES
('Java', 0, 1),
('Java基础', 1, 2),
('Spring', 0, 3),
('Spring Boot', 3, 4),
('MySQL', 0, 5),
('前端', 0, 6),
('集合框架', 1, 7),
('多线程', 1, 8),
('JVM', 1, 9),
('MyBatis', 3, 10),
('数据结构与算法', 0, 11);

-- 示例题目（选择题 Java 基础）
INSERT INTO questions (title, type, multi, category_id, difficulty, score, analysis) VALUES
('以下哪个不是 Java 的基本数据类型？', 'CHOICE', 0, 2, 'EASY', 5, 'String 是引用类型，不属于基本数据类型'),
('下列哪个关键字用于定义类？', 'CHOICE', 0, 2, 'EASY', 5, 'class 用于定义类');

INSERT INTO question_choices (question_id, content, is_correct, sort) VALUES
(1, 'int', 0, 1),
(1, 'double', 0, 2),
(1, 'boolean', 0, 3),
(1, 'String', 1, 4),
(2, 'class', 1, 1),
(2, 'public', 0, 2),
(2, 'static', 0, 3),
(2, 'void', 0, 4);

INSERT INTO question_answers (question_id, answer, keywords) VALUES
(1, 'D', NULL),
(2, 'A', NULL);

-- 示例试卷
INSERT INTO paper (name, description, status, total_score, question_count, duration) VALUES
('Java 基础测试', '考察 Java 基础语法', 'PUBLISHED', 10, 2, 30);

INSERT INTO paper_question (paper_id, question_id, score) VALUES
(1, 1, 5),
(1, 2, 5);

-- 首页轮播图
INSERT INTO banners (title, description, image_url, link_url, sort_order, is_active) VALUES
('智能学习平台', 'AI 赋能，个性化学习路径', 'https://picsum.photos/seed/banner1/1200/400', '/home', 1, 1),
('AI 智能客服', '学习路上，小虎随时为你答疑', 'https://picsum.photos/seed/banner2/1200/400', '/chat', 2, 1);

-- 系统公告
INSERT INTO notices (title, content, type, priority, is_active) VALUES
('欢迎使用智能学习平台', '平台集成在线考试、AI 组卷、企业真题、AI 智能客服等功能，祝您学习愉快！', 'SYSTEM', 1, 1);

-- 视频分类与示例视频
INSERT INTO video_categories (name, description, parent_id, sort_order, status) VALUES
('Java', 'Java 编程语言教程', 0, 1, 1),
('Java基础', 'Java 基础语法讲解', 1, 1, 1),
('Spring', 'Spring 框架教程', 0, 2, 1);

INSERT INTO videos (title, description, category_id, file_url, cover_url, duration, file_size, uploader_name, uploader_type, status, view_count, like_count, tags) VALUES
('Java 基础语法入门', '从零开始学习 Java 语法', 2, 'http://localhost:9000/videos/original/java-basic.mp4', 'https://picsum.photos/seed/video1/640/360', 1800, 104857600, '管理员', 2, 1, 120, 23, 'Java,基础,入门');

-- ============================================================
-- 商业化模块表（企业真题 / 模拟面试 / 邀请码 / 积分 / 用户上传）
-- ============================================================

-- 企业表
DROP TABLE IF EXISTS interview_company;
CREATE TABLE interview_company (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  name            VARCHAR(100) NOT NULL COMMENT '企业名称',
  logo            VARCHAR(255) DEFAULT NULL COMMENT '企业logo URL',
  description     TEXT         DEFAULT NULL COMMENT '企业描述',
  is_premium      TINYINT      DEFAULT 0 COMMENT '是否付费可见：0-否，1-是',
  total_questions INT          DEFAULT 0 COMMENT '题目总数',
  create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '企业表';

-- 企业真题表
DROP TABLE IF EXISTS interview_question;
CREATE TABLE interview_question (
  id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  company_id       BIGINT       DEFAULT NULL COMMENT '企业ID',
  category_id      BIGINT       DEFAULT NULL COMMENT '题集分类ID',
  direction        VARCHAR(30)  NOT NULL COMMENT '技术方向：java/frontend/bigdata/algorithm/devops/testing',
  difficulty_level VARCHAR(20)  DEFAULT 'medium' COMMENT '难度：easy/medium/hard',
  interview_year   INT          DEFAULT NULL COMMENT '面试年份',
  question_content TEXT         NOT NULL COMMENT '题目内容',
  reference_answer TEXT         DEFAULT NULL COMMENT '参考答案',
  view_count       INT          DEFAULT 0 COMMENT '浏览次数',
  favorite_count   INT          DEFAULT 0 COMMENT '收藏次数',
  status           VARCHAR(20)  DEFAULT 'approved' COMMENT '状态：approved/disabled',
  create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted       TINYINT      DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_direction (direction),
  KEY idx_company (company_id)
) ENGINE = InnoDB COMMENT = '企业真题表';

-- 题集分类表（管理端企业-分类下拉）
DROP TABLE IF EXISTS interview_question_category;
CREATE TABLE interview_question_category (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  company_id     BIGINT       DEFAULT NULL COMMENT '企业ID',
  name           VARCHAR(100) NOT NULL COMMENT '题集名称',
  direction      VARCHAR(30)  DEFAULT 'java' COMMENT '技术方向：java/frontend/bigdata/algorithm/devops/testing',
  difficulty     VARCHAR(20)  DEFAULT 'medium' COMMENT '难度：easy/medium/hard',
  year           INT          DEFAULT NULL COMMENT '面试年份',
  round          VARCHAR(20)  DEFAULT 'first' COMMENT '轮次：first/second/third',
  question_count INT          DEFAULT 0 COMMENT '题目数量',
  sort           INT          DEFAULT 0 COMMENT '排序',
  create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_company (company_id)
) ENGINE = InnoDB COMMENT = '题集分类表';

-- 真题收藏表
DROP TABLE IF EXISTS interview_favorite;
CREATE TABLE interview_favorite (
  id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT   NOT NULL COMMENT '用户ID',
  question_id BIGINT   NOT NULL COMMENT '题目ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_question (user_id, question_id)
) ENGINE = InnoDB COMMENT = '真题收藏表';

-- 真题评价表
DROP TABLE IF EXISTS interview_evaluation;
CREATE TABLE interview_evaluation (
  id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  question_id BIGINT   NOT NULL COMMENT '题目ID',
  user_id     BIGINT   NOT NULL COMMENT '用户ID',
  rating      INT      DEFAULT 5 COMMENT '评分 1-5',
  comment     VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_question (question_id)
) ENGINE = InnoDB COMMENT = '真题评价表';

-- 模拟面试记录表
DROP TABLE IF EXISTS mock_interview;
CREATE TABLE mock_interview (
  id                      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id                 BIGINT       NOT NULL COMMENT '用户ID',
  direction               VARCHAR(30)  DEFAULT 'java' COMMENT '技术方向',
  question_count          INT          DEFAULT 5 COMMENT '题目数量',
  difficulty              VARCHAR(20)  DEFAULT 'medium' COMMENT '难度：easy/medium/hard',
  company_type            VARCHAR(20)  DEFAULT 'large' COMMENT '公司类型：large/medium/startup',
  duration                INT          DEFAULT 30 COMMENT '面试时长(分钟)',
  status                  VARCHAR(20)  DEFAULT 'in_progress' COMMENT '状态：in_progress/completed',
  total_score             INT          DEFAULT 0 COMMENT '总分',
  max_score               INT          DEFAULT 0 COMMENT '满分',
  average_score           DECIMAL(6,2) DEFAULT 0 COMMENT '平均分',
  completed_questions     INT          DEFAULT 0 COMMENT '完成题数',
  start_time              DATETIME     DEFAULT NULL COMMENT '开始时间',
  end_time                DATETIME     DEFAULT NULL COMMENT '结束时间',
  interviewer_summary     TEXT         DEFAULT NULL COMMENT '面试官总结',
  improvement_suggestions TEXT         DEFAULT NULL COMMENT '改进建议(JSON数组)',
  create_time             DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time             DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted              TINYINT      DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_user (user_id)
) ENGINE = InnoDB COMMENT = '模拟面试记录表';

-- 模拟面试答题记录表
DROP TABLE IF EXISTS mock_interview_answer;
CREATE TABLE mock_interview_answer (
  id                 BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  interview_id       BIGINT   NOT NULL COMMENT '面试记录ID',
  question_id        BIGINT   NOT NULL COMMENT '题目ID',
  question_content   TEXT     DEFAULT NULL COMMENT '题目内容快照',
  direction          VARCHAR(30) DEFAULT 'java' COMMENT '技术方向快照',
  difficulty_level   VARCHAR(20) DEFAULT 'medium' COMMENT '难度快照',
  user_answer        TEXT     DEFAULT NULL COMMENT '用户答案',
  voice_file_url     VARCHAR(255) DEFAULT NULL COMMENT '语音回答URL',
  answer_time        INT      DEFAULT 0 COMMENT '答题用时(秒)',
  score              INT      DEFAULT 0 COMMENT '得分',
  max_score          INT      DEFAULT 100 COMMENT '满分',
  ai_evaluation      TEXT     DEFAULT NULL COMMENT 'AI评价',
  technical_accuracy INT      DEFAULT 0 COMMENT '技术准确性 1-5',
  clarity            INT      DEFAULT 0 COMMENT '表达清晰度 1-5',
  logic              INT      DEFAULT 0 COMMENT '逻辑性 1-5',
  create_time        DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_interview (interview_id)
) ENGINE = InnoDB COMMENT = '模拟面试答题记录表';

-- 邀请码表
DROP TABLE IF EXISTS invite_code;
CREATE TABLE invite_code (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  code        VARCHAR(40)  NOT NULL COMMENT '邀请码',
  type        VARCHAR(20)  DEFAULT 'normal' COMMENT '类型：normal/vip/enterprise',
  status      VARCHAR(20)  DEFAULT 'unused' COMMENT '状态：unused/used/expired',
  actived_by  BIGINT       DEFAULT NULL COMMENT '激活用户ID',
  actived_at  DATETIME     DEFAULT NULL COMMENT '激活时间',
  expire_time DATETIME     DEFAULT NULL COMMENT '过期时间',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE = InnoDB COMMENT = '邀请码表';

-- 用户积分表
DROP TABLE IF EXISTS user_credit;
CREATE TABLE user_credit (
  id             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id        BIGINT   NOT NULL COMMENT '用户ID',
  total_credits  INT      DEFAULT 0 COMMENT '累计积分',
  active_credits INT      DEFAULT 0 COMMENT '可用积分',
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user (user_id)
) ENGINE = InnoDB COMMENT = '用户积分表';

-- 积分流水表
DROP TABLE IF EXISTS credit_record;
CREATE TABLE credit_record (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT       NOT NULL COMMENT '用户ID',
  change_amount INT          DEFAULT 0 COMMENT '积分变动（正负）',
  type          VARCHAR(30)  DEFAULT 'other' COMMENT '类型：invite/practice/knowledge/other',
  source        VARCHAR(255) DEFAULT NULL COMMENT '来源描述',
  balance       INT          DEFAULT 0 COMMENT '变动后余额',
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_user (user_id)
) ENGINE = InnoDB COMMENT = '积分流水表';

-- 用户上传真题审核表
DROP TABLE IF EXISTS user_contribution;
CREATE TABLE user_contribution (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT       DEFAULT NULL COMMENT '上传用户ID',
  content       TEXT         NOT NULL COMMENT '题目描述',
  image_urls    VARCHAR(1000) DEFAULT NULL COMMENT '图片URL(JSON数组)',
  contact       VARCHAR(100) DEFAULT NULL COMMENT '联系方式',
  status        TINYINT      DEFAULT 0 COMMENT '状态：0待审核/1已采纳/2未采纳',
  admin_remark  VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  created_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  reviewed_time DATETIME     DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '用户上传真题审核表';

-- ============================================================
-- 商业化模块种子数据
-- ============================================================

-- 企业
INSERT INTO interview_company (name, logo, description, is_premium, total_questions) VALUES
('阿里巴巴', 'https://picsum.photos/seed/alibaba/200/200', '全球领先的电子商务与云计算企业，Java 技术栈深厚', 0, 4),
('字节跳动', 'https://picsum.photos/seed/bytedance/200/200', '算法驱动的互联网公司，重视基础与算法', 0, 4),
('腾讯', 'https://picsum.photos/seed/tencent/200/200', '综合性互联网服务公司，覆盖社交、游戏、云计算', 0, 2),
('美团', 'https://picsum.photos/seed/meituan/200/200', '生活服务电子商务平台，分布式架构实践丰富', 1, 2);

-- 题集分类
INSERT INTO interview_question_category (company_id, name, direction, difficulty, year, round, question_count, sort) VALUES
(1, 'Java 基础必考', 'java', 'medium', 2024, 'first', 2, 1),
(1, 'JVM 与并发', 'java', 'hard', 2024, 'second', 2, 2),
(2, '算法与数据结构', 'algorithm', 'medium', 2024, 'first', 2, 1),
(2, '前端基础', 'frontend', 'easy', 2025, 'first', 2, 2),
(3, 'Java 高级', 'java', 'hard', 2025, 'second', 2, 1),
(4, '分布式架构', 'java', 'hard', 2025, 'third', 2, 1);

-- 企业真题（java/frontend/algorithm 全覆盖）
INSERT INTO interview_question (company_id, category_id, direction, difficulty_level, interview_year, question_content, reference_answer, view_count, favorite_count, status) VALUES
(1, 1, 'java', 'medium', 2024, '请阐述 HashMap 的底层实现原理及 JDK1.8 的优化点。', '数组+链表/红黑树，1.8 引入红黑树优化哈希冲突，尾插法避免死循环', 156, 32, 'approved'),
(1, 1, 'java', 'medium', 2024, '什么是线程安全？请举例说明如何保证线程安全。', '多个线程同时访问共享数据时不产生错误结果。可通过 synchronized、Lock、原子类、ThreadLocal 等方式', 120, 21, 'approved'),
(1, 2, 'java', 'hard', 2024, '请简述 JVM 内存模型，并说明年轻代与老年代的对象转移过程。', 'JVM 内存分线程私有区（虚拟机栈、本地方法栈、程序计数器）与共享区（堆、方法区）。对象先进入 Eden，Minor GC 后存活对象进入 Survivor，多次 GC 后晋升老年代', 98, 17, 'approved'),
(1, 2, 'java', 'hard', 2024, '聊聊 synchronized 与 ReentrantLock 的区别。', 'synchronized 是 JVM 关键字自动释放，ReentrantLock 是 API 需手动释放且支持公平锁、可中断、条件变量', 145, 28, 'approved'),
(3, 5, 'java', 'hard', 2025, 'Spring 事务的传播行为有哪些？REQUIRED 传播级别的含义？', 'REQUIRED（默认，有事务则加入）、REQUIRES_NEW（新建事务）、NESTED、SUPPORTS 等。REQUIRED 表示如果当前存在事务则加入当前事务，否则新建事务', 88, 15, 'approved'),
(3, 5, 'java', 'hard', 2025, 'ConcurrentHashMap 在 JDK1.8 中是如何保证线程安全的？', '放弃分段锁，改 CAS + synchronized 锁头节点，锁粒度更细，并发度更高', 102, 22, 'approved'),
(4, 6, 'java', 'hard', 2025, '如何设计一个分布式缓存系统？谈谈缓存穿透、击穿、雪崩的解决方案。', '缓存穿透加布隆过滤器，击穿加互斥锁/热点数据永不过期，雪崩用随机超时/多级缓存/限流降级', 76, 11, 'approved'),
(4, 6, 'java', 'hard', 2025, '说说你对 CAP 理论的理解，以及在分布式系统中的应用。', '一致性、可用性、分区容错性三选二。分布式系统必选 P，再在 C 与 A 间权衡', 85, 14, 'approved'),
(2, 3, 'algorithm', 'medium', 2024, '请实现一个 LRU 缓存（时间复杂度要求 O(1)）。', '哈希表 + 双向链表，get/put 均为 O(1)，命中移动至链表头，容量满时淘汰链表尾', 134, 41, 'approved'),
(2, 3, 'algorithm', 'medium', 2024, '如何判断一个链表是否有环？并找出环的入口。', '快慢指针相遇判定有环，之后一指针回到头节点，两指针同速前进相遇点即为环入口（Floyd 算法）', 167, 39, 'approved'),
(2, 4, 'frontend', 'easy', 2025, '谈谈浏览器从输入 URL 到页面展示的完整过程。', 'DNS 解析→TCP 连接→发送 HTTP 请求→服务器响应→浏览器解析渲染（HTML/CSS/JS）→DOM/布局/绘制', 210, 25, 'approved'),
(2, 4, 'frontend', 'easy', 2025, 'Vue 的响应式原理是什么？', 'Vue2 用 Object.defineProperty 劫持数据，Vue3 用 Proxy。依赖收集 + 派发更新，数据变化触发视图更新', 180, 30, 'approved');

-- 邀请码（含一个已激活用于演示激活状态，一个 VIP，一个企业）
INSERT INTO invite_code (code, type, status, actived_by, actived_at, expire_time) VALUES
('EDU2024DEMO', 'normal', 'used', 3, NOW(), '2026-12-31 23:59:59'),
('EDU2024VIP0', 'vip', 'unused', NULL, NULL, '2026-12-31 23:59:59'),
('EDU2024ENTR', 'enterprise', 'unused', NULL, NULL, '2026-12-31 23:59:59');

-- 给 student(3) 初始化积分
INSERT INTO user_credit (user_id, total_credits, active_credits) VALUES
(3, 100, 100);

INSERT INTO credit_record (user_id, change_amount, type, source, balance) VALUES
(3, 100, 'invite', '激活邀请码 EDU2024DEMO 赠送', 100);

-- ----------------------------------------
-- 用户-试卷关联表（AI 生成试卷仅本人可见）
-- ----------------------------------------
DROP TABLE IF EXISTS user_paper;
CREATE TABLE user_paper (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT       NOT NULL COMMENT '归属用户ID',
  paper_id    BIGINT       NOT NULL COMMENT '试卷ID',
  relation_type VARCHAR(20) DEFAULT 'AI_GENERATED' COMMENT '关联类型：AI_GENERATED（AI生成）、ASSIGNED（管理员分配）',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  is_deleted  TINYINT      DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_user (user_id),
  KEY idx_paper (paper_id),
  UNIQUE KEY uk_user_paper (user_id, paper_id)
) ENGINE = InnoDB COMMENT = '用户试卷关联表（AI 生成试卷归属）';