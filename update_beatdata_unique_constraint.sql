-- 删除原有的唯一约束
ALTER TABLE beatdata DROP INDEX idx_music_name_unique;

-- 创建新的复合唯一约束（音乐名称 + 创建用户ID）
ALTER TABLE beatdata ADD UNIQUE INDEX idx_music_name_user_unique (music_name, created_by);
