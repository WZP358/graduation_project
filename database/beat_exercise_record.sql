-- 节拍练习记录表
CREATE TABLE IF NOT EXISTS `beat_exercise_record` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `user_name` VARCHAR(50) NOT NULL COMMENT '用户名称',
    `beatdata_id` BIGINT(20) NOT NULL COMMENT '节拍数据ID',
    `music_name` VARCHAR(255) NOT NULL COMMENT '音乐名称',
    `playback_speed` DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '播放速度(0.5-1.0)',
    `practice_mode` VARCHAR(20) NOT NULL COMMENT '练习模式(follow/blind)',
    `accuracy` INT(3) NOT NULL COMMENT '准确率(0-100)',
    `score` INT(10) NOT NULL COMMENT '得分',
    `hit_count` INT(10) NOT NULL COMMENT '命中数',
    `total_beats` INT(10) NOT NULL COMMENT '总节拍数',
    `total_hits` INT(10) NOT NULL COMMENT '总击打数',
    `avg_error` INT(10) DEFAULT NULL COMMENT '平均误差(ms)',
    `max_combo` INT(10) NOT NULL COMMENT '最高连击',
    `perfect_count` INT(10) DEFAULT 0 COMMENT 'Perfect数量',
    `good_count` INT(10) DEFAULT 0 COMMENT 'Good数量',
    `ok_count` INT(10) DEFAULT 0 COMMENT 'OK数量',
    `miss_count` INT(10) DEFAULT 0 COMMENT 'Miss数量',
    `practice_time` INT(10) DEFAULT NULL COMMENT '练习时长(秒)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_beatdata_id` (`beatdata_id`),
    KEY `idx_accuracy` (`accuracy` DESC),
    KEY `idx_score` (`score` DESC),
    CONSTRAINT `fk_beat_exercise_record_beatdata` 
        FOREIGN KEY (`beatdata_id`) 
        REFERENCES `beatdata` (`id`) 
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节拍练习记录表';

-- 创建索引以优化排行榜查询
CREATE INDEX `idx_leaderboard` ON `beat_exercise_record` 
    (`beatdata_id`, `playback_speed`, `practice_mode`, `accuracy` DESC, `score` DESC);

