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

-- ----------------------------------------
-- 支付宝沙箱支付订单（购买邀请码）
-- ----------------------------------------
DROP TABLE IF EXISTS pay_order;
CREATE TABLE pay_order (
  id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_no      VARCHAR(64)   NOT NULL COMMENT '商户订单号',
  user_id       BIGINT        NOT NULL COMMENT '下单用户ID',
  product_type  VARCHAR(20)   NOT NULL COMMENT '商品类型：normal/vip/enterprise',
  amount        DECIMAL(10,2) NOT NULL COMMENT '支付金额（元）',
  status        VARCHAR(20)   NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED/PAID/CLOSED',
  trade_no      VARCHAR(64)   DEFAULT NULL COMMENT '支付宝交易号',
  pay_time      DATETIME      DEFAULT NULL COMMENT '支付时间',
  notify_time   DATETIME      DEFAULT NULL COMMENT '异步回调时间',
  create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_user (user_id),
  KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '支付宝沙箱支付订单（购买邀请码）';

-- ----------------------------------------
-- AI 学习路径（结构化可执行任务清单）
-- ----------------------------------------
DROP TABLE IF EXISTS learning_path;
CREATE TABLE learning_path (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id        BIGINT       NOT NULL COMMENT '归属用户ID',
  status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/OUTDATED/COMPLETED',
  diagnosis_json TEXT         COMMENT '诊断快照JSON：[{categoryName,answerCount,earnedScore,maxScore,correctRate}]',
  summary        TEXT         COMMENT 'AI 规划总结',
  node_count     INT          DEFAULT 0 COMMENT '节点总数',
  create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  KEY idx_user_status (user_id, status)
) ENGINE = InnoDB COMMENT = 'AI 学习路径';

DROP TABLE IF EXISTS learning_path_node;
CREATE TABLE learning_path_node (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  path_id      BIGINT       NOT NULL COMMENT '路径ID',
  phase        INT          NOT NULL DEFAULT 1 COMMENT '阶段序号（从1开始）',
  phase_title  VARCHAR(100) DEFAULT NULL COMMENT '阶段标题',
  sort_order   INT          NOT NULL DEFAULT 0 COMMENT '节点在路径内的全局顺序',
  node_type    VARCHAR(20)  NOT NULL COMMENT '节点类型：KNOWLEDGE/PAPER/QUESTION',
  ref_id       BIGINT       DEFAULT NULL COMMENT '关联实体ID：PAPER=paper.id / QUESTION=interview_question.id / KNOWLEDGE=NULL',
  category_name VARCHAR(100) DEFAULT NULL COMMENT '关联知识点分类名（KNOWLEDGE 必填）',
  title        VARCHAR(200) NOT NULL COMMENT '节点标题',
  description  TEXT         COMMENT '节点描述/讲解文本/完成指引',
  status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '完成状态：PENDING/COMPLETED',
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  KEY idx_path (path_id)
) ENGINE = InnoDB COMMENT = 'AI 学习路径节点';

-- ----------------------------------------
-- 财务收支台账（后台管理 - 企业财务报表数据源）
-- ----------------------------------------
DROP TABLE IF EXISTS finance_record;
CREATE TABLE finance_record (
  id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  record_no      VARCHAR(32)   NOT NULL COMMENT '流水号',
  record_type    VARCHAR(10)   NOT NULL COMMENT '类型：INCOME（收入）/EXPENSE（支出）',
  category       VARCHAR(50)   NOT NULL COMMENT '分类',
  amount         DECIMAL(12,2) NOT NULL COMMENT '金额（正数，方向由 record_type 表达）',
  record_date    DATE          NOT NULL COMMENT '业务发生日期',
  payment_method VARCHAR(20)   DEFAULT NULL COMMENT '结算方式：ALIPAY/BANK/CASH',
  remark         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  create_time    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_record_no (record_no),
  KEY idx_date (record_date),
  KEY idx_type_category (record_type, category)
) ENGINE = InnoDB COMMENT = '财务收支台账';

INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251101001', 'INCOME', '课程订阅', 7855.58, '2025-11-01', 'BANK', '课程订阅-前端进阶班');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251102001', 'INCOME', '邀请码销售', 29.90, '2025-11-02', 'ALIPAY', '购买普通邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20251103001', 'EXPENSE', '市场推广', 1799.90, '2025-11-03', 'ALIPAY', '公众号/社群推广');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20251103002', 'EXPENSE', '人员薪酬', 14184.55, '2025-11-03', 'BANK', '月度薪酬发放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251104001', 'INCOME', '真题集销售', 29.90, '2025-11-04', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251105001', 'INCOME', '邀请码销售', 9.90, '2025-11-05', 'ALIPAY', '购买普通邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251108001', 'INCOME', '邀请码销售', 9.90, '2025-11-08', 'ALIPAY', '活动价购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251111001', 'INCOME', '课程订阅', 7187.84, '2025-11-11', 'BANK', '课程订阅-大数据实战班');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251111002', 'INCOME', '真题集销售', 199.00, '2025-11-11', 'ALIPAY', '真题集销售-阿里篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20251113001', 'EXPENSE', '市场推广', 1449.10, '2025-11-13', 'ALIPAY', '小红书内容投放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251115001', 'INCOME', '邀请码销售', 9.90, '2025-11-15', 'ALIPAY', '活动价购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251118001', 'INCOME', '邀请码销售', 29.90, '2025-11-18', 'ALIPAY', '购买普通邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251118002', 'INCOME', '邀请码销售', 29.90, '2025-11-18', 'ALIPAY', '购买VIP邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251120001', 'INCOME', '真题集销售', 99.00, '2025-11-20', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20251121001', 'EXPENSE', '市场推广', 2324.53, '2025-11-21', 'BANK', '小红书内容投放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251123001', 'INCOME', '邀请码销售', 29.90, '2025-11-23', 'ALIPAY', '活动价购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20251124001', 'EXPENSE', '服务器与云服务', 2252.83, '2025-11-24', 'BANK', '云数据库订阅费');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251124001', 'INCOME', '邀请码销售', 9.90, '2025-11-24', 'ALIPAY', '购买企业版邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20251205001', 'EXPENSE', '人员薪酬', 9754.57, '2025-12-05', 'BANK', '月度薪酬发放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20251206001', 'EXPENSE', '办公与行政', 398.69, '2025-12-06', 'BANK', '办公用品采购');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251206001', 'INCOME', '邀请码销售', 29.90, '2025-12-06', 'ALIPAY', '活动价购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251206002', 'INCOME', '邀请码销售', 29.90, '2025-12-06', 'ALIPAY', '新用户购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20251209001', 'EXPENSE', '市场推广', 3186.03, '2025-12-09', 'ALIPAY', '公众号/社群推广');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251212001', 'INCOME', '邀请码销售', 9.90, '2025-12-12', 'ALIPAY', '购买VIP邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251213001', 'INCOME', '真题集销售', 99.00, '2025-12-13', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20251215001', 'EXPENSE', '第三方服务', 1139.56, '2025-12-15', 'BANK', '客服系统订阅');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20251216001', 'EXPENSE', '服务器与云服务', 1182.73, '2025-12-16', 'BANK', '云数据库订阅费');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20251218001', 'EXPENSE', '市场推广', 4673.94, '2025-12-18', 'BANK', '小红书内容投放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20251219001', 'EXPENSE', '市场推广', 3027.86, '2025-12-19', 'BANK', '校园渠道推广');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251219001', 'INCOME', '真题集销售', 29.90, '2025-12-19', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251221001', 'INCOME', '邀请码销售', 9.90, '2025-12-21', 'ALIPAY', '购买企业版邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251221002', 'INCOME', '邀请码销售', 9.90, '2025-12-21', 'ALIPAY', '活动价购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251223001', 'INCOME', '邀请码销售', 29.90, '2025-12-23', 'ALIPAY', '购买普通邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20251225001', 'EXPENSE', '办公与行政', 841.57, '2025-12-25', 'CASH', '办公用品采购');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20251227001', 'INCOME', '课程订阅', 7106.69, '2025-12-27', 'BANK', '课程订阅-Java 就业班');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260103001', 'EXPENSE', '办公与行政', 522.28, '2026-01-03', 'BANK', '办公用品采购');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260104001', 'INCOME', '邀请码销售', 29.90, '2026-01-04', 'ALIPAY', '活动价购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260104002', 'INCOME', '课程订阅', 6045.09, '2026-01-04', 'BANK', '课程订阅-大数据实战班');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260104003', 'INCOME', '真题集销售', 49.90, '2026-01-04', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260106001', 'INCOME', '企业合作', 22478.28, '2026-01-06', 'BANK', '企业合作-极客学院');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260107001', 'INCOME', '企业合作', 25153.26, '2026-01-07', 'BANK', '企业合作-云启网络');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260108001', 'INCOME', '真题集销售', 29.90, '2026-01-08', 'ALIPAY', '真题集销售-阿里篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260114001', 'INCOME', '邀请码销售', 9.90, '2026-01-14', 'ALIPAY', '购买VIP邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260116001', 'EXPENSE', '服务器与云服务', 1031.10, '2026-01-16', 'BANK', '百炼API调用费用');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260116001', 'INCOME', '真题集销售', 29.90, '2026-01-16', 'ALIPAY', '真题集销售-阿里篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260117001', 'EXPENSE', '办公与行政', 685.45, '2026-01-17', 'BANK', '办公用品采购');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260118001', 'INCOME', '真题集销售', 199.00, '2026-01-18', 'ALIPAY', '真题集销售-字节篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260119001', 'EXPENSE', '第三方服务', 1518.63, '2026-01-19', 'BANK', '域名/SSL证书续费');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260120001', 'EXPENSE', '市场推广', 3543.90, '2026-01-20', 'BANK', '公众号/社群推广');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260120001', 'INCOME', '真题集销售', 199.00, '2026-01-20', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260122001', 'EXPENSE', '人员薪酬', 11191.94, '2026-01-22', 'BANK', '月度薪酬发放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260123001', 'EXPENSE', '第三方服务', 315.82, '2026-01-23', 'BANK', '短信服务费');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260124001', 'INCOME', '邀请码销售', 9.90, '2026-01-24', 'ALIPAY', '购买企业版邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260125001', 'EXPENSE', '市场推广', 5630.35, '2026-01-25', 'ALIPAY', '校园渠道推广');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260125001', 'INCOME', '邀请码销售', 29.90, '2026-01-25', 'ALIPAY', '购买企业版邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260127001', 'INCOME', '真题集销售', 99.00, '2026-01-27', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260128001', 'INCOME', '邀请码销售', 99.90, '2026-01-28', 'ALIPAY', '购买普通邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260202001', 'INCOME', '邀请码销售', 9.90, '2026-02-02', 'ALIPAY', '购买企业版邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260202002', 'INCOME', '真题集销售', 99.00, '2026-02-02', 'ALIPAY', '真题集销售-阿里篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260203001', 'EXPENSE', '市场推广', 6000.00, '2026-02-03', 'BANK', '春节促活投放-信息流');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260204001', 'INCOME', '真题集销售', 29.90, '2026-02-04', 'ALIPAY', '真题集销售-美团篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260204002', 'INCOME', '真题集销售', 99.00, '2026-02-04', 'ALIPAY', '真题集销售-字节篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260205001', 'INCOME', '课程订阅', 4284.42, '2026-02-05', 'BANK', '课程订阅-全栈训练营');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260208001', 'INCOME', '邀请码销售', 9.90, '2026-02-08', 'ALIPAY', '购买企业版邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260208002', 'INCOME', '课程订阅', 6456.27, '2026-02-08', 'BANK', '课程订阅-算法冲刺班');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260214001', 'EXPENSE', '市场推广', 4200.00, '2026-02-14', 'ALIPAY', '春节促活投放-社群');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260214001', 'INCOME', '真题集销售', 29.90, '2026-02-14', 'ALIPAY', '真题集销售-美团篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260215001', 'EXPENSE', '人员薪酬', 9467.90, '2026-02-15', 'BANK', '月度薪酬发放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260218001', 'EXPENSE', '服务器与云服务', 1144.86, '2026-02-18', 'BANK', '云数据库订阅费');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260224001', 'INCOME', '真题集销售', 29.90, '2026-02-24', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260301001', 'INCOME', '邀请码销售', 9.90, '2026-03-01', 'ALIPAY', '新用户购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260302001', 'EXPENSE', '服务器与云服务', 1842.05, '2026-03-02', 'BANK', '阿里云服务器续费');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260302001', 'INCOME', '企业合作', 20934.16, '2026-03-02', 'BANK', '企业合作-极客学院');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260303001', 'EXPENSE', '第三方服务', 2353.17, '2026-03-03', 'BANK', '域名/SSL证书续费');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260303001', 'INCOME', '邀请码销售', 29.90, '2026-03-03', 'ALIPAY', '购买VIP邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260303002', 'INCOME', '真题集销售', 99.00, '2026-03-03', 'ALIPAY', '真题集销售-阿里篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260305001', 'EXPENSE', '人员薪酬', 10124.92, '2026-03-05', 'BANK', '月度薪酬发放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260305001', 'INCOME', '企业合作', 29368.17, '2026-03-05', 'BANK', '企业合作-云启网络');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260308001', 'INCOME', '真题集销售', 49.90, '2026-03-08', 'ALIPAY', '真题集销售-美团篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260309001', 'EXPENSE', '第三方服务', 768.94, '2026-03-09', 'BANK', '域名/SSL证书续费');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260311001', 'INCOME', '真题集销售', 29.90, '2026-03-11', 'ALIPAY', '真题集销售-阿里篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260313001', 'INCOME', '邀请码销售', 9.90, '2026-03-13', 'ALIPAY', '购买普通邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260315001', 'EXPENSE', '办公与行政', 1039.15, '2026-03-15', 'BANK', '办公用品采购');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260316001', 'INCOME', '邀请码销售', 9.90, '2026-03-16', 'ALIPAY', '新用户购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260316002', 'INCOME', '真题集销售', 29.90, '2026-03-16', 'ALIPAY', '真题集销售-字节篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260317001', 'INCOME', '真题集销售', 29.90, '2026-03-17', 'ALIPAY', '真题集销售-阿里篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260319001', 'EXPENSE', '市场推广', 4218.34, '2026-03-19', 'BANK', '校园渠道推广');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260319001', 'INCOME', '真题集销售', 29.90, '2026-03-19', 'ALIPAY', '真题集销售-字节篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260320001', 'EXPENSE', '办公与行政', 212.11, '2026-03-20', 'CASH', '办公用品采购');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260321001', 'INCOME', '邀请码销售', 9.90, '2026-03-21', 'ALIPAY', '活动价购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260323001', 'EXPENSE', '市场推广', 1862.15, '2026-03-23', 'BANK', '公众号/社群推广');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260323001', 'INCOME', '课程订阅', 4710.50, '2026-03-23', 'BANK', '课程订阅-全栈训练营');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260326001', 'INCOME', '课程订阅', 8558.11, '2026-03-26', 'BANK', '课程订阅-算法冲刺班');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260328001', 'INCOME', '邀请码销售', 29.90, '2026-03-28', 'ALIPAY', '活动价购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260405001', 'INCOME', '邀请码销售', 99.90, '2026-04-05', 'ALIPAY', '购买企业版邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260406001', 'EXPENSE', '市场推广', 5894.95, '2026-04-06', 'BANK', '小红书内容投放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260407001', 'INCOME', '真题集销售', 29.90, '2026-04-07', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260409001', 'INCOME', '真题集销售', 49.90, '2026-04-09', 'ALIPAY', '真题集销售-阿里篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260411001', 'EXPENSE', '服务器与云服务', 879.36, '2026-04-11', 'BANK', '百炼API调用费用');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260412001', 'EXPENSE', '人员薪酬', 8288.23, '2026-04-12', 'BANK', '月度薪酬发放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260414001', 'EXPENSE', '办公与行政', 918.28, '2026-04-14', 'CASH', '办公用品采购');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260414001', 'INCOME', '真题集销售', 29.90, '2026-04-14', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260415001', 'INCOME', '邀请码销售', 9.90, '2026-04-15', 'ALIPAY', '活动价购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260418001', 'EXPENSE', '市场推广', 4469.47, '2026-04-18', 'ALIPAY', '抖音信息流推广');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260420001', 'INCOME', '邀请码销售', 29.90, '2026-04-20', 'ALIPAY', '活动价购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260421001', 'INCOME', '真题集销售', 29.90, '2026-04-21', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260422001', 'INCOME', '邀请码销售', 9.90, '2026-04-22', 'ALIPAY', '购买普通邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260423001', 'EXPENSE', '市场推广', 1191.27, '2026-04-23', 'ALIPAY', '小红书内容投放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260424001', 'INCOME', '课程订阅', 8337.28, '2026-04-24', 'BANK', '课程订阅-全栈训练营');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260426001', 'EXPENSE', '办公与行政', 600.74, '2026-04-26', 'CASH', '水电物业费');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260427001', 'INCOME', '邀请码销售', 9.90, '2026-04-27', 'ALIPAY', '活动价购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260501001', 'EXPENSE', '市场推广', 2521.98, '2026-05-01', 'ALIPAY', '校园渠道推广');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260501001', 'INCOME', '邀请码销售', 29.90, '2026-05-01', 'ALIPAY', '购买VIP邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260502001', 'INCOME', '课程订阅', 5294.25, '2026-05-02', 'BANK', '课程订阅-算法冲刺班');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260504001', 'INCOME', '邀请码销售', 9.90, '2026-05-04', 'ALIPAY', '购买企业版邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260508001', 'INCOME', '邀请码销售', 99.90, '2026-05-08', 'ALIPAY', '购买VIP邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260509001', 'EXPENSE', '服务器与云服务', 1517.24, '2026-05-09', 'BANK', '阿里云服务器续费');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260509002', 'EXPENSE', '人员薪酬', 9428.54, '2026-05-09', 'BANK', '月度薪酬发放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260511001', 'INCOME', '邀请码销售', 29.90, '2026-05-11', 'ALIPAY', '新用户购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260515001', 'INCOME', '真题集销售', 99.00, '2026-05-15', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260517001', 'EXPENSE', '市场推广', 2625.96, '2026-05-17', 'BANK', '小红书内容投放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260522001', 'INCOME', '真题集销售', 29.90, '2026-05-22', 'ALIPAY', '真题集销售-字节篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260523001', 'INCOME', '课程订阅', 3954.19, '2026-05-23', 'BANK', '课程订阅-Java 就业班');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260524001', 'INCOME', '邀请码销售', 99.90, '2026-05-24', 'ALIPAY', '购买VIP邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260526001', 'INCOME', '邀请码销售', 99.90, '2026-05-26', 'ALIPAY', '购买VIP邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260526002', 'INCOME', '真题集销售', 29.90, '2026-05-26', 'ALIPAY', '真题集销售-字节篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260528001', 'INCOME', '邀请码销售', 29.90, '2026-05-28', 'ALIPAY', '新用户购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260528002', 'INCOME', '邀请码销售', 29.90, '2026-05-28', 'ALIPAY', '购买VIP邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260602001', 'EXPENSE', '第三方服务', 624.18, '2026-06-02', 'BANK', '域名/SSL证书续费');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260603001', 'INCOME', '邀请码销售', 9.90, '2026-06-03', 'ALIPAY', '购买企业版邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260604001', 'INCOME', '课程订阅', 6979.32, '2026-06-04', 'BANK', '课程订阅-大数据实战班');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260606001', 'INCOME', '真题集销售', 49.90, '2026-06-06', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260607001', 'EXPENSE', '服务器与云服务', 1346.97, '2026-06-07', 'BANK', '云数据库订阅费');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260607002', 'EXPENSE', '市场推广', 3099.23, '2026-06-07', 'BANK', '公众号/社群推广');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260610001', 'EXPENSE', '市场推广', 4226.02, '2026-06-10', 'BANK', '抖音信息流推广');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260610001', 'INCOME', '企业合作', 23048.01, '2026-06-10', 'BANK', '企业合作-极客学院');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260611001', 'EXPENSE', '市场推广', 3954.64, '2026-06-11', 'BANK', '搜索引擎竞价推广');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260617001', 'INCOME', '邀请码销售', 9.90, '2026-06-17', 'ALIPAY', '购买普通邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260619001', 'INCOME', '邀请码销售', 9.90, '2026-06-19', 'ALIPAY', '购买VIP邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260622001', 'INCOME', '课程订阅', 6041.74, '2026-06-22', 'BANK', '课程订阅-算法冲刺班');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260622002', 'INCOME', '真题集销售', 29.90, '2026-06-22', 'ALIPAY', '真题集销售-美团篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260623001', 'INCOME', '邀请码销售', 9.90, '2026-06-23', 'ALIPAY', '新用户购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260624001', 'INCOME', '邀请码销售', 9.90, '2026-06-24', 'ALIPAY', '购买企业版邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260624002', 'INCOME', '邀请码销售', 29.90, '2026-06-24', 'ALIPAY', '购买VIP邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260625001', 'EXPENSE', '人员薪酬', 14547.39, '2026-06-25', 'BANK', '月度薪酬发放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260626001', 'EXPENSE', '第三方服务', 982.92, '2026-06-26', 'BANK', '域名/SSL证书续费');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260626001', 'INCOME', '邀请码销售', 9.90, '2026-06-26', 'ALIPAY', '活动价购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260627001', 'INCOME', '真题集销售', 49.90, '2026-06-27', 'ALIPAY', '真题集销售-阿里篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260701001', 'INCOME', '邀请码销售', 29.90, '2026-07-01', 'ALIPAY', '购买普通邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260702001', 'EXPENSE', '市场推广', 4000.00, '2026-07-02', 'BANK', '暑期招生投放-信息流');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260703001', 'EXPENSE', '办公与行政', 1360.93, '2026-07-03', 'CASH', '差旅报销');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260708001', 'INCOME', '邀请码销售', 9.90, '2026-07-08', 'ALIPAY', '购买VIP邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260708002', 'INCOME', '邀请码销售', 29.90, '2026-07-08', 'ALIPAY', '购买VIP邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260708003', 'INCOME', '课程订阅', 3300.31, '2026-07-08', 'BANK', '课程订阅-Java 就业班');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260709001', 'EXPENSE', '服务器与云服务', 1928.15, '2026-07-09', 'BANK', '云数据库订阅费');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260709001', 'INCOME', '真题集销售', 99.00, '2026-07-09', 'ALIPAY', '真题集销售-美团篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260710001', 'EXPENSE', '市场推广', 3800.00, '2026-07-10', 'ALIPAY', '暑期招生投放-校园渠道');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260711001', 'EXPENSE', '人员薪酬', 9875.95, '2026-07-11', 'BANK', '月度薪酬发放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260715001', 'INCOME', '真题集销售', 199.00, '2026-07-15', 'ALIPAY', '真题集销售-美团篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260718001', 'INCOME', '真题集销售', 99.00, '2026-07-18', 'ALIPAY', '真题集销售-美团篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260720001', 'INCOME', '真题集销售', 199.00, '2026-07-20', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260721001', 'INCOME', '邀请码销售', 9.90, '2026-07-21', 'ALIPAY', '活动价购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260723001', 'INCOME', '邀请码销售', 9.90, '2026-07-23', 'ALIPAY', '新用户购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260724001', 'INCOME', '真题集销售', 99.00, '2026-07-24', 'ALIPAY', '真题集销售-美团篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260725001', 'INCOME', '邀请码销售', 29.90, '2026-07-25', 'ALIPAY', '购买普通邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260725002', 'INCOME', '真题集销售', 49.90, '2026-07-25', 'ALIPAY', '真题集销售-字节篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260726001', 'INCOME', '课程订阅', 5737.77, '2026-07-26', 'BANK', '课程订阅-算法冲刺班');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260801001', 'INCOME', '真题集销售', 49.90, '2026-08-01', 'ALIPAY', '真题集销售-美团篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260805001', 'EXPENSE', '市场推广', 4142.30, '2026-08-05', 'BANK', '公众号/社群推广');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260811001', 'EXPENSE', '办公与行政', 475.16, '2026-08-11', 'BANK', '团建活动费用');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260812001', 'INCOME', '课程订阅', 5860.88, '2026-08-12', 'BANK', '课程订阅-大数据实战班');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260813001', 'EXPENSE', '人员薪酬', 8402.28, '2026-08-13', 'BANK', '月度薪酬发放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260814001', 'INCOME', '邀请码销售', 9.90, '2026-08-14', 'ALIPAY', '购买企业版邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260814002', 'INCOME', '邀请码销售', 9.90, '2026-08-14', 'ALIPAY', '购买普通邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260814003', 'INCOME', '邀请码销售', 9.90, '2026-08-14', 'ALIPAY', '新用户购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260816001', 'INCOME', '企业合作', 32956.23, '2026-08-16', 'BANK', '企业合作-云启网络');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260818001', 'INCOME', '企业合作', 29950.31, '2026-08-18', 'BANK', '企业合作-极客学院');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260819001', 'EXPENSE', '服务器与云服务', 2432.47, '2026-08-19', 'BANK', '阿里云服务器续费');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260819001', 'INCOME', '邀请码销售', 99.90, '2026-08-19', 'ALIPAY', '购买普通邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260820001', 'INCOME', '真题集销售', 199.00, '2026-08-20', 'ALIPAY', '真题集销售-阿里篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260822001', 'INCOME', '真题集销售', 29.90, '2026-08-22', 'ALIPAY', '真题集销售-美团篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260823001', 'INCOME', '邀请码销售', 29.90, '2026-08-23', 'ALIPAY', '购买VIP邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260825001', 'INCOME', '邀请码销售', 29.90, '2026-08-25', 'ALIPAY', '活动价购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260827001', 'EXPENSE', '第三方服务', 1954.42, '2026-08-27', 'BANK', 'CDN 流量费');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260827001', 'INCOME', '真题集销售', 99.00, '2026-08-27', 'ALIPAY', '真题集销售-美团篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260828001', 'INCOME', '课程订阅', 7519.85, '2026-08-28', 'BANK', '课程订阅-算法冲刺班');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260901001', 'INCOME', '真题集销售', 29.90, '2026-09-01', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260902001', 'INCOME', '邀请码销售', 29.90, '2026-09-02', 'ALIPAY', '购买企业版邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260903001', 'INCOME', '邀请码销售', 9.90, '2026-09-03', 'ALIPAY', '购买普通邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260904001', 'EXPENSE', '市场推广', 3922.14, '2026-09-04', 'ALIPAY', '校园渠道推广');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260904002', 'EXPENSE', '人员薪酬', 14578.65, '2026-09-04', 'BANK', '月度薪酬发放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260906001', 'INCOME', '真题集销售', 99.00, '2026-09-06', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260907001', 'INCOME', '真题集销售', 199.00, '2026-09-07', 'ALIPAY', '真题集销售-阿里篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260908001', 'INCOME', '邀请码销售', 29.90, '2026-09-08', 'ALIPAY', '购买VIP邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260911001', 'EXPENSE', '市场推广', 3431.00, '2026-09-11', 'ALIPAY', '搜索引擎竞价推广');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260912001', 'INCOME', '企业合作', 20124.87, '2026-09-12', 'BANK', '企业合作-云启网络');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260913001', 'INCOME', '真题集销售', 49.90, '2026-09-13', 'ALIPAY', '真题集销售-美团篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260915001', 'INCOME', '企业合作', 33955.14, '2026-09-15', 'BANK', '企业合作-未来互联');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260916001', 'INCOME', '邀请码销售', 29.90, '2026-09-16', 'ALIPAY', '购买普通邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260917001', 'EXPENSE', '第三方服务', 687.50, '2026-09-17', 'BANK', '第三方数据服务');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260920001', 'INCOME', '邀请码销售', 29.90, '2026-09-20', 'ALIPAY', '购买VIP邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260921001', 'EXPENSE', '办公与行政', 1449.02, '2026-09-21', 'BANK', '差旅报销');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260922001', 'EXPENSE', '市场推广', 3570.28, '2026-09-22', 'BANK', '抖音信息流推广');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260922001', 'INCOME', '课程订阅', 3756.33, '2026-09-22', 'BANK', '课程订阅-Java 就业班');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260924001', 'INCOME', '真题集销售', 49.90, '2026-09-24', 'ALIPAY', '真题集销售-阿里篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20260925001', 'EXPENSE', '服务器与云服务', 983.42, '2026-09-25', 'BANK', '对象存储/带宽费用');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20260927001', 'INCOME', '真题集销售', 49.90, '2026-09-27', 'ALIPAY', '真题集销售-字节篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20261004001', 'INCOME', '邀请码销售', 9.90, '2026-10-04', 'ALIPAY', '购买普通邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20261006001', 'EXPENSE', '服务器与云服务', 1677.59, '2026-10-06', 'BANK', '对象存储/带宽费用');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20261006001', 'INCOME', '真题集销售', 29.90, '2026-10-06', 'ALIPAY', '真题集销售-字节篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20261007001', 'INCOME', '真题集销售', 49.90, '2026-10-07', 'ALIPAY', '真题集销售-腾讯篇');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20261008001', 'INCOME', '课程订阅', 4672.57, '2026-10-08', 'BANK', '课程订阅-前端进阶班');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20261009001', 'INCOME', '邀请码销售', 29.90, '2026-10-09', 'ALIPAY', '新用户购买邀请码');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('EX20261010001', 'EXPENSE', '市场推广', 2569.76, '2026-10-10', 'BANK', '小红书内容投放');
INSERT INTO finance_record (record_no, record_type, category, amount, record_date, payment_method, remark) VALUES ('IN20261010001', 'INCOME', '邀请码销售', 9.90, '2026-10-10', 'ALIPAY', '购买企业版邀请码');
