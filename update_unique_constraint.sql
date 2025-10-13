ALTER TABLE beatdata DROP INDEX idx_music_name_unique; ALTER TABLE beatdata ADD UNIQUE INDEX idx_music_name_user_unique (music_name, created_by);
