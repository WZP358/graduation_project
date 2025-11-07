-- 检查节拍练习记录表是否存在
-- 运行此脚本来验证表结构

-- 1. 检查表是否存在
SELECT 
    TABLE_NAME,
    TABLE_COMMENT,
    CREATE_TIME
FROM 
    information_schema.TABLES
WHERE 
    TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'beat_exercise_record';

-- 如果上面的查询返回空，说明表不存在，需要运行 beat_exercise_record.sql

-- 2. 如果表存在，查看表结构
DESC beat_exercise_record;

-- 3. 查看所有索引
SHOW INDEX FROM beat_exercise_record;

-- 4. 查看表中的记录数
SELECT COUNT(*) as record_count FROM beat_exercise_record;

-- 5. 查看最近的10条记录
SELECT 
    id,
    user_name,
    music_name,
    accuracy,
    score,
    max_combo,
    create_time
FROM 
    beat_exercise_record
ORDER BY 
    create_time DESC
LIMIT 10;

-- 6. 验证外键约束
SELECT 
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM 
    information_schema.KEY_COLUMN_USAGE
WHERE 
    TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'beat_exercise_record'
    AND REFERENCED_TABLE_NAME IS NOT NULL;

