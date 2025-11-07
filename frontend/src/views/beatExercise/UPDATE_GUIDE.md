# 节拍练习功能更新指南

## 主要更新内容

### 1. 难度改为播放速度

#### 修改变量名
```javascript
// 旧代码
const difficulty = ref('normal');
const difficultyConfigs = {
    easy: { bpm: 60, tolerance: {...} },
    normal: { bpm: 80, tolerance: {...} },
    ...
};

// 新代码
const playbackSpeed = ref(1.0);
const toleranceConfig = {
    perfect: 100,
    good: 150,
    ok: 200
};
```

#### 修改UI
```vue
<!-- 旧代码 -->
<el-form-item label="难度">
    <el-select v-model="difficulty">
        <el-option label="宽松 (±200ms)" value="easy"></el-option>
        <el-option label="标准 (±100ms)" value="normal"></el-option>
        ...
    </el-select>
</el-form-item>

<!-- 新代码 -->
<el-form-item label="播放速度">
    <el-select v-model="playbackSpeed" :disabled="isPlaying">
        <el-option label="0.5倍速 (慢速练习)" :value="0.5"></el-option>
        <el-option label="0.75倍速 (适应练习)" :value="0.75"></el-option>
        <el-option label="1.0倍速 (正常速度)" :value="1.0"></el-option>
    </el-select>
    <div style="font-size: 12px; color: #909399; margin-top: 5px;">
        速度越慢，节拍间隔越长，越容易跟上
    </div>
</el-form-item>
```

#### 修改音频播放速度
```javascript
function actualStartPractice() {
    isPlaying.value = true;
    // ... 其他代码 ...
    
    // 设置音频播放速度
    audioPlayer.value.playbackRate = playbackSpeed.value;
    audioPlayer.value.play();
    audioPlaying.value = true;
}
```

### 2. 添加排行榜功能

#### 导入API
```javascript
import { 
    saveExerciseRecord, 
    getLeaderboard, 
    getUserBestRecord,
    getIntelligentAnalysis,
    exportReport
} from '@/api/music_anaysis/exercise';
```

#### 添加数据定义
```javascript
// 排行榜数据
const leaderboard = ref([]);
const userBestRecord = ref(null);
const userRank = ref(null);
const showLeaderboard = ref(false);
```

#### 添加排行榜UI
```vue
<!-- 在右侧面板添加排行榜卡片 -->
<el-card shadow="hover" style="margin-top: 20px;">
    <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
            <span>排行榜</span>
            <el-button type="primary" size="small" @click="loadLeaderboard">刷新</el-button>
        </div>
    </template>
    
    <div v-if="leaderboard.length > 0">
        <el-table :data="leaderboard" size="small" max-height="400">
            <el-table-column label="排名" width="60" align="center">
                <template #default="scope">
                    <el-tag 
                        v-if="scope.$index < 3" 
                        :type="['', 'warning', 'info'][scope.$index]"
                    >
                        {{ scope.$index + 1 }}
                    </el-tag>
                    <span v-else>{{ scope.$index + 1 }}</span>
                </template>
            </el-table-column>
            <el-table-column label="用户" prop="userName" show-overflow-tooltip />
            <el-table-column label="准确率" width="80" align="center">
                <template #default="scope">
                    <el-tag :type="getAccuracyTagType(scope.row.accuracy)" size="small">
                        {{ scope.row.accuracy }}%
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="得分" prop="score" width="80" align="center" />
            <el-table-column label="连击" prop="maxCombo" width="70" align="center" />
        </el-table>
        
        <el-divider />
        
        <div v-if="userBestRecord" class="user-best-record">
            <el-descriptions :column="2" size="small" border>
                <el-descriptions-item label="我的最佳">
                    第 {{ userRank }} 名
                </el-descriptions-item>
                <el-descriptions-item label="准确率">
                    {{ userBestRecord.accuracy }}%
                </el-descriptions-item>
                <el-descriptions-item label="得分">
                    {{ userBestRecord.score }}
                </el-descriptions-item>
                <el-descriptions-item label="连击">
                    {{ userBestRecord.maxCombo }}
                </el-descriptions-item>
            </el-descriptions>
        </div>
    </div>
    <el-empty v-else description="暂无排行榜数据" :image-size="80" />
</el-card>
```

### 3. 添加智能分析功能

#### 添加数据定义
```javascript
const intelligentAnalysis = ref(null);
const showAnalysis = ref(false);
```

#### 添加分析UI（在结果页面）
```vue
<!-- 在结果页面添加智能分析按钮 -->
<el-button type="success" icon="TrendCharts" @click="showIntelligentAnalysis">
    智能分析
</el-button>
<el-button type="warning" icon="Download" @click="downloadReport">
    导出报告
</el-button>

<!-- 智能分析对话框 -->
<el-dialog v-model="showAnalysis" title="智能分析报告" width="600px">
    <div v-if="intelligentAnalysis" class="analysis-content">
        <el-alert :title="intelligentAnalysis.summary" type="info" :closable="false" />
        
        <el-divider content-position="left">优点</el-divider>
        <ul class="analysis-list">
            <li v-for="(item, index) in intelligentAnalysis.strengths" :key="'s-'+index">
                ✓ {{ item }}
            </li>
        </ul>
        
        <el-divider content-position="left">需要改进</el-divider>
        <ul class="analysis-list">
            <li v-for="(item, index) in intelligentAnalysis.weaknesses" :key="'w-'+index">
                ⚠ {{ item }}
            </li>
        </ul>
        
        <el-divider content-position="left">建议</el-divider>
        <ul class="analysis-list">
            <li v-for="(item, index) in intelligentAnalysis.suggestions" :key="'sg-'+index">
                💡 {{ item }}
            </li>
        </ul>
    </div>
</el-dialog>
```

### 4. 保存练习记录到数据库

#### 修改endPractice函数
```javascript
async function endPractice() {
    isPlaying.value = false;
    audioPlaying.value = false;
    audioPlayer.value.pause();
    audioPlayer.value.currentTime = 0;
    
    calculateResults();
    
    // 保存到数据库
    try {
        const recordData = {
            beatdataId: selectedBeatdata.value,
            musicName: currentMusicInfo.value.name,
            playbackSpeed: playbackSpeed.value,
            practiceMode: practiceMode.value,
            accuracy: results.value.accuracy,
            score: results.value.score,
            hitCount: results.value.hitCount,
            totalBeats: results.value.totalBeats,
            totalHits: results.value.totalHits,
            avgError: results.value.avgError,
            maxCombo: results.value.maxCombo,
            perfectCount: results.value.perfect,
            goodCount: results.value.good,
            okCount: results.value.ok,
            missCount: results.value.miss,
            practiceTime: Math.floor(audioDuration.value)
        };
        
        const response = await saveExerciseRecord(recordData);
        currentRecordId.value = response.data;
        ElMessage.success('练习记录已保存');
        
        // 刷新排行榜
        loadLeaderboard();
    } catch (error) {
        console.error('保存记录失败:', error);
    }
    
    showResults.value = true;
    saveToHistory();
    todayCount.value++;
}
```

### 5. 实现智能分析

#### 客户端分析
```javascript
function generateLocalAnalysis() {
    const analysis = {
        summary: '',
        strengths: [],
        weaknesses: [],
        suggestions: []
    };
    
    const acc = results.value.accuracy;
    const avgErr = results.value.avgError;
    const perfectRate = results.value.perfect / results.value.totalHits * 100;
    
    // 总体评价
    if (acc >= 90) {
        analysis.summary = '🎉 表现优秀！您的节奏感知能力很强！';
    } else if (acc >= 70) {
        analysis.summary = '👍 表现良好，继续保持！';
    } else {
        analysis.summary = '💪 还有提升空间，多加练习！';
    }
    
    // 优点分析
    if (acc >= 80) {
        analysis.strengths.push(`准确率达到 ${acc}%，节奏把握准确`);
    }
    if (results.value.maxCombo >= 20) {
        analysis.strengths.push(`最高连击 ${results.value.maxCombo}，稳定性很好`);
    }
    if (perfectRate >= 50) {
        analysis.strengths.push(`Perfect率 ${perfectRate.toFixed(1)}%，精准度高`);
    }
    if (avgErr <= 30) {
        analysis.strengths.push(`平均误差仅 ${avgErr}ms，非常稳定`);
    }
    
    // 不足分析
    if (acc < 70) {
        analysis.weaknesses.push('准确率较低，建议降低播放速度多加练习');
    }
    if (results.value.maxCombo < 10) {
        analysis.weaknesses.push('连击数较低，需要提高稳定性');
    }
    if (results.value.miss > results.value.totalHits * 0.3) {
        analysis.weaknesses.push('误击率过高，注意只在节拍位置击打');
    }
    if (avgErr > 60) {
        analysis.weaknesses.push('平均误差较大，需要更专注');
    }
    
    // 建议
    if (playbackSpeed.value === 1.0 && acc < 80) {
        analysis.suggestions.push('可以尝试降低播放速度到0.75倍或0.5倍');
    }
    if (practiceMode.value === 'blind' && acc < 70) {
        analysis.suggestions.push('建议先使用跟随模式熟悉节拍');
    }
    if (perfectRate < 30) {
        analysis.suggestions.push('多关注节拍点的精确位置，提高Perfect率');
    }
    analysis.suggestions.push('多次练习同一首歌曲，熟能生巧');
    
    return analysis;
}
```

### 6. 实现导出报告

```javascript
async function downloadReport() {
    if (!currentRecordId.value) {
        ElMessage.warning('请先完成一次练习');
        return;
    }
    
    try {
        const response = await exportReport(currentRecordId.value);
        const blob = new Blob([response], { type: 'application/pdf' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `节拍练习报告_${currentMusicInfo.value.name}_${new Date().getTime()}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        ElMessage.success('报告导出成功');
    } catch (error) {
        ElMessage.error('导出失败: ' + error.message);
    }
}
```

## 容差配置说明

固定容差配置（与速度无关）：
```javascript
const toleranceConfig = {
    perfect: 100,  // ±100ms
    good: 150,     // ±150ms
    ok: 200        // ±200ms
};
```

原理：
- 即使播放速度降低，判定标准保持不变
- 这样可以真实反映用户的节奏感知能力
- 不会因为速度慢而"放水"

## 修改清单

- [ ] 将difficulty改为playbackSpeed
- [ ] 移除difficultyConfigs，使用固定toleranceConfig
- [ ] 添加音频播放速度设置
- [ ] 导入exercise API
- [ ] 添加排行榜数据和UI
- [ ] 添加智能分析功能
- [ ] 修改endPractice保存到数据库
- [ ] 添加导出报告功能
- [ ] 更新所有相关的计算逻辑
- [ ] 测试所有功能

