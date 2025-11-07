# 🎵 节拍练习功能快速上手指南

## 📋 功能说明

已将节拍练习的排行榜数据从**模拟数据**改为**真实数据库存储**，所有练习记录将永久保存。

## ✅ 已完成的更改

### 前端修改
- ❌ 删除了模拟数据存储 `leaderboardStorage`
- ❌ 删除了模拟API `mockLeaderboardAPI`
- ✅ 使用真实API接口保存和查询数据
- ✅ 支持从后端获取智能分析
- ✅ 支持从后端导出练习报告

### 后端实现
- ✅ 完整的CRUD接口
- ✅ 排行榜查询（支持按条件筛选）
- ✅ 用户最佳记录查询（自动计算排名）
- ✅ 智能分析（优点、不足、建议）
- ✅ 报告导出

## 🚀 快速开始

### 第1步：检查数据库表

**方式1：使用SQL脚本检查**

```bash
# 进入数据库目录
cd database

# 连接数据库并执行检查脚本
mysql -u root -p your_database < check_exercise_table.sql
```

**方式2：手动检查**

登录MySQL：
```bash
mysql -u root -p
```

运行检查命令：
```sql
USE your_database;
SHOW TABLES LIKE 'beat_exercise_record';
```

### 第2步：创建数据库表（如果不存在）

如果第1步显示表不存在，执行以下命令：

```bash
# 在database目录下
mysql -u root -p your_database < beat_exercise_record.sql
```

或者在MySQL客户端中直接执行：
```sql
SOURCE /path/to/beat_exercise_record.sql;
```

### 第3步：重启后端服务

```bash
cd backend
# Windows
ry.bat

# Linux/Mac
./ry.sh
```

### 第4步：重启前端服务

```bash
cd frontend
npm run dev
```

## 🧪 测试步骤

### 1. 登录系统
- 打开浏览器访问前端地址（通常是 http://localhost:80 或 http://localhost:3000）
- 使用你的账号登录

### 2. 进入节拍练习页面
- 在菜单中找到"节拍练习"
- 点击进入

### 3. 选择音乐和节拍数据
- 在左侧选择一首音乐
- 选择对应的节拍数据（标准答案）
- 选择播放速度（建议新手从0.5倍开始）
- 选择练习模式（建议新手选"跟随模式"）

### 4. 开始练习
- 点击"开始练习"按钮
- 等待3秒倒计时
- 音乐播放时，按空格键或点击屏幕击打节拍
- 跟随模式下会显示标准节拍位置作为参考

### 5. 完成练习
- 等待音乐播放完成，或点击"停止练习"
- 系统会显示练习结果
- **重要**：检查是否显示 "练习记录已保存到数据库" 的提示

### 6. 查看排行榜
- 在右侧排行榜区域查看你的排名
- 点击"刷新"按钮确认数据持久化
- **刷新页面后再查看，数据应该还在**

### 7. 查看智能分析
- 点击"智能分析"按钮
- 查看系统对你练习情况的分析
- 包含优点、不足和建议

### 8. 导出报告
- 点击"导出报告"按钮
- 下载练习报告文本文件
- 报告包含完整的练习数据和分析

## 🔍 验证数据持久化

### 方式1：通过前端验证
1. 完成一次练习后，记住你的得分和准确率
2. **刷新浏览器页面**
3. 查看排行榜，你的数据应该还在
4. **关闭浏览器重新打开**，数据依然存在

### 方式2：通过数据库验证

在MySQL中执行：
```sql
USE your_database;

-- 查看所有练习记录
SELECT 
    user_name,
    music_name,
    accuracy,
    score,
    max_combo,
    practice_mode,
    DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') as practice_time
FROM 
    beat_exercise_record
ORDER BY 
    create_time DESC
LIMIT 20;

-- 查看排行榜（按准确率和得分排序）
SELECT 
    @rank := @rank + 1 AS rank,
    user_name,
    accuracy,
    score,
    max_combo
FROM 
    beat_exercise_record,
    (SELECT @rank := 0) r
WHERE
    beatdata_id = YOUR_BEATDATA_ID  -- 替换为实际的节拍数据ID
ORDER BY 
    accuracy DESC,
    score DESC
LIMIT 10;

-- 查看某个用户的最佳记录
SELECT 
    user_name,
    music_name,
    MAX(accuracy) as best_accuracy,
    MAX(score) as best_score,
    MAX(max_combo) as best_combo
FROM 
    beat_exercise_record
WHERE
    user_name = 'YOUR_USERNAME'  -- 替换为你的用户名
GROUP BY
    user_name,
    music_name;
```

## 📊 API接口说明

### 保存练习记录
```
POST /music_anaysis/exercise/record
```
请求体：
```json
{
    "beatdataId": 1,
    "musicName": "示例音乐",
    "playbackSpeed": 1.0,
    "practiceMode": "follow",
    "accuracy": 95,
    "score": 8500,
    "hitCount": 42,
    "totalBeats": 45,
    "totalHits": 50,
    "avgError": 25,
    "maxCombo": 38,
    "perfectCount": 35,
    "goodCount": 7,
    "okCount": 3,
    "missCount": 5,
    "practiceTime": 180
}
```

### 获取排行榜
```
GET /music_anaysis/exercise/leaderboard?beatdataId=1&playbackSpeed=1.0&practiceMode=follow&limit=10
```

### 获取用户最佳记录
```
GET /music_anaysis/exercise/best?beatdataId=1&playbackSpeed=1.0&practiceMode=follow
```

### 获取智能分析
```
GET /music_anaysis/exercise/analyze/{recordId}
```

### 导出报告
```
GET /music_anaysis/exercise/export/{recordId}
```

## ❓ 常见问题

### Q1: 提示"保存记录失败"
**原因：**
- 数据库表未创建
- 外键约束失败（节拍数据ID不存在）
- 未登录或token过期

**解决：**
1. 检查数据库表是否存在
2. 确保选择的节拍数据是有效的
3. 重新登录

### Q2: 排行榜一直为空
**原因：**
- 还没有任何人练习过这个节拍数据
- 前端未正确调用API
- 后端服务未启动

**解决：**
1. 先完成一次练习
2. 检查浏览器控制台是否有错误
3. 检查后端服务是否正常运行

### Q3: 刷新页面后数据丢失
**原因：**
- 数据库表未创建，仍在使用模拟数据
- API调用失败

**解决：**
1. 确认已创建数据库表
2. 检查后端日志
3. 查看浏览器控制台的网络请求

### Q4: 外键约束错误
**错误信息：** `Cannot add or update a child row: a foreign key constraint fails`

**原因：** 选择的节拍数据ID在数据库中不存在

**解决：**
1. 进入"波形分析"页面
2. 为该音乐创建节拍数据
3. 返回节拍练习页面重新选择

### Q5: 智能分析显示失败
**原因：**
- 练习记录未保存成功
- 记录ID无效

**解决：**
1. 确保练习完成后显示了"保存成功"提示
2. 如果失败，会自动使用本地生成的分析

## 📈 数据库统计查询

### 查看所有用户的练习次数
```sql
SELECT 
    user_name,
    COUNT(*) as practice_count,
    AVG(accuracy) as avg_accuracy,
    MAX(accuracy) as best_accuracy,
    SUM(practice_time) as total_time_seconds
FROM 
    beat_exercise_record
GROUP BY 
    user_name
ORDER BY 
    avg_accuracy DESC;
```

### 查看最受欢迎的音乐
```sql
SELECT 
    music_name,
    COUNT(*) as practice_count,
    AVG(accuracy) as avg_accuracy
FROM 
    beat_exercise_record
GROUP BY 
    music_name
ORDER BY 
    practice_count DESC
LIMIT 10;
```

### 查看今天的练习记录
```sql
SELECT 
    user_name,
    music_name,
    accuracy,
    score,
    DATE_FORMAT(create_time, '%H:%i:%s') as time
FROM 
    beat_exercise_record
WHERE 
    DATE(create_time) = CURDATE()
ORDER BY 
    create_time DESC;
```

## 🎯 下一步建议

1. **多次练习同一首歌**：熟悉节奏后准确率会提高
2. **尝试不同速度**：从0.5倍逐渐提升到1.0倍
3. **挑战盲打模式**：不看提示，完全依靠听觉
4. **查看智能分析**：根据建议改进
5. **导出报告**：记录你的进步历程

## 📞 获取帮助

遇到问题时，请检查：
1. **浏览器控制台**（F12）- 查看JavaScript错误
2. **网络请求**（F12 -> Network）- 查看API调用情况
3. **后端日志** - 查看服务器错误
4. **数据库日志** - 查看SQL错误

## 🎉 完成！

现在你可以愉快地使用节拍练习功能了！所有数据都会安全地保存在数据库中。

祝练习愉快！🎵
