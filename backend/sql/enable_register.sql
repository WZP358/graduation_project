-- 开启注册功能的SQL脚本
-- 将注册功能开关从false改为true

USE graduation_project;

-- 更新注册功能配置
UPDATE sys_config 
SET config_value = 'true' 
WHERE config_key = 'sys.account.registerUser';

-- 验证更新结果
SELECT config_id, config_name, config_key, config_value 
FROM sys_config 
WHERE config_key = 'sys.account.registerUser';