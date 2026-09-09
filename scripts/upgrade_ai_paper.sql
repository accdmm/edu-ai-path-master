-- =============================================================
-- 运行库升级脚本：AI 生成试卷（用户私有卷）+ 考试记录绑定用户
-- 适用：已存在旧库，无需重跑 init.sql。幂等：均已做存在性判断
-- 运行（PowerShell 下不能用 < 重定向）：
--   mysql -uroot -proot --default-character-set=utf8mb4 -e "source C:/Users/34147/Downloads/edu-ai-path-master/scripts/upgrade_ai_paper.sql"
-- =============================================================
USE exam_system_0625;

-- 1. exam_records 增加 user_id 列（不存在才添加）
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = 'exam_system_0625' AND TABLE_NAME = 'exam_records' AND COLUMN_NAME = 'user_id') = 0,
  'ALTER TABLE exam_records ADD COLUMN user_id BIGINT DEFAULT NULL COMMENT ''关联用户ID'' AFTER exam_id',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 创建 user_paper 关联表（不存在才创建）
CREATE TABLE IF NOT EXISTS user_paper (
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

-- 3. categories 补齐 AI 出题常用分类（缺失才插入）
INSERT IGNORE INTO categories (name, parent_id, sort) VALUES
('集合框架', 1, 7),
('多线程', 1, 8),
('JVM', 1, 9),
('MyBatis', 3, 10),
('数据结构与算法', 0, 11);

SELECT 'upgrade done' AS result;