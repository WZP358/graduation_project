-- 添加节拍练习菜单到数据库
-- 此SQL将节拍练习添加为系统菜单，使其在布局中显示（像 music_info 一样）

-- 注意：执行前请检查 menu_id 是否已被使用
-- 您可以先执行: SELECT MAX(menu_id) FROM sys_menu; 来查看当前最大的 menu_id

-- 假设当前最大 menu_id 小于 2000，我们使用 2000 作为节拍练习菜单的 ID
-- 如果有冲突，请调整 menu_id

-- 1. 先查找是否已存在节拍练习菜单
SELECT menu_id, menu_name, path FROM sys_menu WHERE path = 'beatExercise' OR menu_name = '节拍练习';

-- 如果上面的查询返回空，说明菜单不存在，继续执行下面的插入语句
-- 如果已存在，则跳过插入，或者先删除旧的：
-- DELETE FROM sys_menu WHERE menu_id = 2000;

-- 2. 插入节拍练习菜单（作为一级菜单）
INSERT INTO sys_menu VALUES(
    2000,                           -- menu_id: 菜单ID，请确保不与现有ID冲突
    '节拍练习',                      -- menu_name: 菜单名称
    0,                              -- parent_id: 父菜单ID，0表示一级菜单
    5,                              -- order_num: 显示顺序
    'beatExercise',                 -- path: 路由地址
    'beatExercise/BeatExercise',    -- component: 组件路径
    '',                             -- query: 路由参数
    '',                             -- 
    1,                              -- is_frame: 是否外链 (0是 1否)
    0,                              -- is_cache: 是否缓存 (0缓存 1不缓存)
    'C',                            -- menu_type: 菜单类型 (M目录 C菜单 F按钮)
    '0',                            -- visible: 菜单状态 (0显示 1隐藏)
    '0',                            -- status: 菜单状态 (0正常 1停用)
    '',                             -- perms: 权限标识
    'guide',                        -- icon: 菜单图标
    'admin',                        -- create_by: 创建者
    sysdate(),                      -- create_time: 创建时间
    '',                             -- update_by: 更新者
    null,                           -- update_time: 更新时间
    '节拍练习菜单'                   -- remark: 备注
);

-- 3. 验证菜单是否添加成功
SELECT menu_id, menu_name, path, component, visible, status 
FROM sys_menu 
WHERE menu_id = 2000;

-- 4. 如果需要将菜单作为某个父菜单的子菜单（例如，如果有"音乐管理"父菜单）
-- 首先查找音乐管理的菜单ID：
-- SELECT menu_id, menu_name FROM sys_menu WHERE menu_name LIKE '%音乐%';

-- 然后更新 parent_id（假设音乐管理的 menu_id 是 2100）：
-- UPDATE sys_menu SET parent_id = 2100, order_num = 3 WHERE menu_id = 2000;

-- 5. 刷新权限缓存（可选，有些系统需要）
-- 如果系统支持，执行清除缓存的存储过程或通知后端刷新权限

COMMIT;

-- 说明：
-- 1. menu_type 类型说明：
--    M: 目录 (有子菜单的父菜单)
--    C: 菜单 (具体的页面)
--    F: 按钮 (页面上的操作按钮)
--
-- 2. component 路径说明：
--    格式: 'beatExercise/BeatExercise'
--    对应前端文件: frontend/src/views/beatExercise/BeatExercise.vue
--    系统会自动添加 @/views/ 前缀
--
-- 3. path 路径说明：
--    这是浏览器地址栏显示的路径
--    完整URL: http://localhost:8082/beatExercise
--
-- 4. icon 图标说明：
--    使用系统内置图标名称
--    可选图标: guide, music, video, chart, list, etc.
--    图标文件位置: frontend/src/assets/icons/svg/

-- 执行完成后需要：
-- 1. 重新登录系统，让菜单权限生效
-- 2. 在侧边栏应该能看到"节拍练习"菜单
-- 3. 点击菜单后页面应该在布局中显示（有侧边栏和顶部导航）

