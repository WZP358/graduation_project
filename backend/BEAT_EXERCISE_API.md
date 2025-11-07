# 节拍练习记录 API 文档

## 数据库表设计

### beat_exercise_record 表
```sql
- id: 主键
- user_id: 用户ID
- user_name: 用户名称
- beatdata_id: 节拍数据ID (外键，级联删除)
- music_name: 音乐名称
- playback_speed: 播放速度 (0.5-1.0)
- practice_mode: 练习模式 (follow/blind)
- accuracy: 准确率 (0-100)
- score: 得分
- hit_count: 命中数
- total_beats: 总节拍数
- total_hits: 总击打数
- avg_error: 平均误差(ms)
- max_combo: 最高连击
- perfect_count: Perfect数量
- good_count: Good数量
- ok_count: OK数量
- miss_count: Miss数量
- practice_time: 练习时长(秒)
- create_time: 创建时间
```

## 需要创建的文件

### 1. Mapper接口
`com/wzp/music_anaysis/mapper/BeatExerciseRecordMapper.java`
```java
public interface BeatExerciseRecordMapper {
    // 查询列表
    List<BeatExerciseRecord> selectBeatExerciseRecordList(BeatExerciseRecord record);
    
    // 查询单个
    BeatExerciseRecord selectBeatExerciseRecordById(Long id);
    
    // 插入
    int insertBeatExerciseRecord(BeatExerciseRecord record);
    
    // 删除
    int deleteBeatExerciseRecordById(Long id);
    int deleteBeatExerciseRecordByIds(Long[] ids);
    
    // 根据节拍数据ID删除（级联删除）
    int deleteByBeatdataId(Long beatdataId);
    
    // 获取排行榜
    List<BeatExerciseRecord> selectLeaderboard(
        @Param("beatdataId") Long beatdataId,
        @Param("playbackSpeed") BigDecimal playbackSpeed,
        @Param("practiceMode") String practiceMode,
        @Param("limit") int limit
    );
    
    // 获取用户最佳记录
    BeatExerciseRecord selectUserBestRecord(
        @Param("userId") Long userId,
        @Param("beatdataId") Long beatdataId,
        @Param("playbackSpeed") BigDecimal playbackSpeed,
        @Param("practiceMode") String practiceMode
    );
}
```

### 2. Service接口
`com/wzp/music_anaysis/service/IBeatExerciseRecordService.java`

### 3. Service实现
`com/wzp/music_anaysis/service/impl/BeatExerciseRecordServiceImpl.java`

### 4. Controller
`com/wzp/music_anaysis/controller/BeatExerciseRecordController.java`

## API 端点

### 1. 保存练习记录
```
POST /music_anaysis/exercise/record
参数: BeatExerciseRecord对象
返回: { code: 200, msg: "保存成功", data: recordId }
```

### 2. 获取排行榜
```
GET /music_anaysis/exercise/leaderboard
参数: 
  - beatdataId: 节拍数据ID
  - playbackSpeed: 播放速度 (可选)
  - practiceMode: 练习模式 (可选)
  - limit: 返回数量 (默认100)
返回: { code: 200, data: [排行榜记录列表] }
```

### 3. 获取用户最佳记录
```
GET /music_anaysis/exercise/best
参数:
  - beatdataId: 节拍数据ID
  - playbackSpeed: 播放速度 (可选)
  - practiceMode: 练习模式 (可选)
返回: { code: 200, data: 最佳记录 }
```

### 4. 获取用户练习历史
```
GET /music_anaysis/exercise/history
参数:
  - pageNum: 页码
  - pageSize: 每页数量
返回: { rows: [记录列表], total: 总数 }
```

### 5. 导出练习报告
```
GET /music_anaysis/exercise/export/{id}
返回: PDF/Excel文件
```

### 6. 智能分析
```
GET /music_anaysis/exercise/analyze/{id}
返回: {
  analysis: "分析结果",
  suggestions: ["建议1", "建议2", ...],
  strengths: ["优点1", "优点2", ...],
  weaknesses: ["不足1", "不足2", ...]
}
```

## 在BeatdataServiceImpl中添加级联删除

需要在删除节拍数据时同时删除对应的练习记录：

```java
@Autowired
private BeatExerciseRecordMapper exerciseRecordMapper;

@Override
@Transactional
public int deleteBeatdataById(Long id) {
    // 先删除关联的练习记录
    exerciseRecordMapper.deleteByBeatdataId(id);
    // 再删除节拍数据
    return beatdataMapper.deleteBeatdataById(id);
}
```

## 前端API调用

`frontend/src/api/music_anaysis/exercise.js`
```javascript
import request from '@/utils/request'

// 保存练习记录
export function saveExerciseRecord(data) {
  return request({
    url: '/music_anaysis/exercise/record',
    method: 'post',
    data: data
  })
}

// 获取排行榜
export function getLeaderboard(params) {
  return request({
    url: '/music_anaysis/exercise/leaderboard',
    method: 'get',
    params: params
  })
}

// 获取用户最佳记录
export function getUserBestRecord(params) {
  return request({
    url: '/music_anaysis/exercise/best',
    method: 'get',
    params: params
  })
}

// 获取智能分析
export function getIntelligentAnalysis(id) {
  return request({
    url: '/music_anaysis/exercise/analyze/' + id,
    method: 'get'
  })
}

// 导出报告
export function exportReport(id) {
  return request({
    url: '/music_anaysis/exercise/export/' + id,
    method: 'get',
    responseType: 'blob'
  })
}
```

## 智能分析算法

根据练习数据智能分析：

1. **节奏感知能力**
   - 准确率 >= 90%: 优秀
   - 准确率 70-90%: 良好
   - 准确率 < 70%: 需要加强

2. **稳定性分析**
   - 平均误差 < 30ms: 稳定
   - 平均误差 30-60ms: 一般
   - 平均误差 > 60ms: 不稳定

3. **节奏倾向**
   - 误差 > 0: 倾向于抢拍(偏快)
   - 误差 < 0: 倾向于慢拍(偏慢)

4. **击打习惯**
   - Perfect比例 > 50%: 优秀
   - Good比例过高: 需要提高精准度
   - Miss比例过高: 需要多听多练

5. **建议生成**
   - 根据弱点给出针对性建议
   - 推荐合适的练习速度和模式

