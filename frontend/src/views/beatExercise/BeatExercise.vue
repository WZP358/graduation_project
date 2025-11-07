<template>
    <div class="app-container beat-exercise">
        <!-- 页面标题和统计 -->
        <el-row :gutter="20" class="mb8">
            <el-col :span="12">
                <h2 class="page-title">节拍练习</h2>
                <p class="page-desc">跟随音乐节奏练习，提升节拍感知能力</p>
            </el-col>
            <el-col :span="12" style="text-align: right;">
                <el-tag size="large" type="success">今日练习: {{ todayCount }} 次</el-tag>
                <el-tag size="large" type="warning" style="margin-left: 10px;">最佳准确率: {{ bestAccuracy }}%</el-tag>
            </el-col>
        </el-row>

        <el-row :gutter="20">
            <!-- 左侧：音乐和设置 -->
            <el-col :span="6">
                <el-card shadow="hover">
                    <template #header>
                        <span>选择音乐</span>
                    </template>
                    
                    <el-form :model="settingForm" label-width="80px" size="default">
                        <el-form-item label="音乐">
                            <el-select 
                                v-model="selectedMusic" 
                                @change="handleMusicChange" 
                                :disabled="isPlaying"
                                placeholder="从音乐管理中选择"
                                style="width: 100%;"
                                filterable
                            >
                                <el-option 
                                    v-for="music in musicList" 
                                    :key="music.id"
                                    :label="music.name"
                                    :value="music.id"
                                >
                                    <span style="float: left">{{ music.name }}</span>
                                    <span style="float: right; color: #8492a6; font-size: 12px;">
                                        {{ music.beatCount }}个节拍数据
                                    </span>
                                </el-option>
                            </el-select>
                            <div v-if="!selectedMusic" style="font-size: 12px; color: #909399; margin-top: 5px;">
                                ⚠️ 请先选择音乐，系统会自动加载对应的节拍数据
                            </div>
                        </el-form-item>

                        <el-form-item label="节拍数据">
                            <el-select 
                                v-model="selectedBeatdata" 
                                :disabled="isPlaying || !selectedMusic"
                                :placeholder="selectedMusic ? '选择节拍数据作为标准答案' : '请先选择音乐'"
                                style="width: 100%;"
                                :loading="beatdataLoading"
                            >
                                <el-option 
                                    v-for="beatdata in beatdataList" 
                                    :key="beatdata.id"
                                    :label="`${beatdata.creatorName} (${beatdata.detectionMode})`"
                                    :value="beatdata.id"
                                >
                                    <div style="display: flex; justify-content: space-between; align-items: center;">
                                        <span>{{ beatdata.creatorName }}</span>
                                        <el-tag size="small" :type="beatdata.detectionMode === 'librosa' ? 'success' : 'info'">
                                            {{ beatdata.detectionMode === 'librosa' ? '自动检测' : '手动标注' }}
                                        </el-tag>
                                    </div>
                                </el-option>
                            </el-select>
                            <div v-if="selectedMusic && beatdataList.length === 0 && !beatdataLoading" style="font-size: 12px; color: #f56c6c; margin-top: 5px;">
                                ⚠️ 该音乐暂无节拍数据，请先在波形分析页面标注
                            </div>
                            <div v-if="selectedMusic && beatdataList.length > 0" style="font-size: 12px; color: #67c23a; margin-top: 5px;">
                                ✓ 已加载 {{ beatdataList.length }} 个节拍数据
                            </div>
                        </el-form-item>

                        <el-form-item label="播放速度">
                            <el-select v-model="playbackSpeed" :disabled="isPlaying" style="width: 100%;">
                                <el-option label="0.5倍速 (慢速练习)" :value="0.5"></el-option>
                                <el-option label="0.75倍速 (适应练习)" :value="0.75"></el-option>
                                <el-option label="1.0倍速 (正常速度)" :value="1.0"></el-option>
                            </el-select>
                            <div style="font-size: 12px; color: #909399; margin-top: 5px;">
                                速度越慢，节拍间隔越长，越容易跟上
                            </div>
                        </el-form-item>

                        <el-form-item label="练习模式">
                            <el-radio-group v-model="practiceMode" :disabled="isPlaying">
                                <el-radio label="follow">跟随模式</el-radio>
                                <el-radio label="blind">盲打模式</el-radio>
                            </el-radio-group>
                            <div style="font-size: 12px; color: #909399; margin-top: 5px;">
                                {{ practiceMode === 'follow' ? '显示节拍提示' : '不显示提示，测试听力' }}
                            </div>
                        </el-form-item>

                        <el-form-item>
                            <el-button 
                                v-if="!isPlaying" 
                                type="primary" 
                                icon="VideoPlay"
                                @click="startPractice"
                                :disabled="!selectedMusic || !selectedBeatdata"
                                style="width: 100%;"
                            >
                                开始练习
                            </el-button>
                            <el-button 
                                v-else 
                                type="danger" 
                                icon="VideoPause"
                                @click="stopPractice"
                                style="width: 100%;"
                            >
                                停止练习
                            </el-button>
                        </el-form-item>

                        <!-- 音乐信息 -->
                        <el-divider v-if="currentMusicInfo" />
                        <div v-if="currentMusicInfo" class="music-info">
                            <div class="info-item">
                                <span class="info-label">音乐名称：</span>
                                <span>{{ currentMusicInfo.name }}</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">标准节拍数：</span>
                                <span>{{ standardBeats.length }} 个</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">音频时长：</span>
                                <span>{{ formatDuration(audioDuration) }}</span>
                            </div>
                        </div>
                    </el-form>
                </el-card>

                <!-- 历史记录 -->
                <el-card shadow="hover" style="margin-top: 20px;" v-if="practiceHistory.length > 0">
                    <template #header>
                        <span>最近记录</span>
                    </template>
                    <el-timeline>
                        <el-timeline-item 
                            v-for="(record, index) in practiceHistory.slice(0, 5)" 
                            :key="index"
                            :timestamp="formatTimestamp(record.timestamp)"
                            placement="top"
                        >
                            <div style="font-weight: 600;">{{ record.musicName }}</div>
                            <el-tag size="small" :type="getAccuracyTagType(record.accuracy)">
                                准确率: {{ record.accuracy }}%
                            </el-tag>
                            <div style="font-size: 12px; color: #909399; margin-top: 5px;">
                                命中: {{ record.hitCount }}/{{ record.totalBeats }} | 误差: ±{{ record.avgError }}ms
                            </div>
                        </el-timeline-item>
                    </el-timeline>
                </el-card>
            </el-col>

            <!-- 中间：游戏区域 -->
            <el-col :span="12">
                <el-card shadow="hover" :body-style="{ padding: '20px', minHeight: '600px', position: 'relative' }">
                    <!-- 倒计时 -->
                    <div v-if="showCountdown" class="countdown-overlay">
                        <div class="countdown-number">{{ countdownNumber }}</div>
                        <p>音乐即将开始...</p>
                    </div>

                    <!-- 练习进行中 -->
                    <div v-if="isPlaying && !showCountdown">
                        <!-- 播放控制 -->
                        <div class="audio-controls">
                            <el-button 
                                :icon="audioPlaying ? 'VideoPause' : 'VideoPlay'" 
                                circle 
                                @click="toggleAudioPlay"
                            />
                            <div class="progress-info">
                                <span class="time-text">{{ formatTime(currentTime) }} / {{ formatTime(audioDuration) }}</span>
                                <el-slider 
                                    v-model="progressPercent" 
                                    :show-tooltip="false"
                                    @change="seekAudio"
                                    :disabled="true"
                                />
                            </div>
                        </div>

                        <!-- 实时统计 -->
                        <el-row :gutter="20" class="stat-row">
                            <el-col :span="6">
                                <el-statistic title="已打" :value="userHits.length">
                                    <template #prefix>
                                        <el-icon style="color: #409eff;"><Operation /></el-icon>
                                    </template>
                                </el-statistic>
                            </el-col>
                            <el-col :span="6">
                                <el-statistic title="命中" :value="hitCount">
                                    <template #prefix>
                                        <el-icon style="color: #67c23a;"><Select /></el-icon>
                                    </template>
                                </el-statistic>
                            </el-col>
                            <el-col :span="6">
                                <el-statistic title="准确率" :value="accuracy" suffix="%">
                                    <template #prefix>
                                        <el-icon style="color: #e6a23c;"><Aim /></el-icon>
                                    </template>
                                </el-statistic>
                            </el-col>
                            <el-col :span="6">
                                <el-statistic title="连击" :value="currentCombo">
                                    <template #prefix>
                                        <el-icon style="color: #f56c6c;"><TrophyBase /></el-icon>
                                    </template>
                                </el-statistic>
                            </el-col>
                        </el-row>

                        <!-- 可视化波形和节拍 -->
                        <div class="visualization-area">
                            <!-- 时间轴 -->
                            <div class="timeline-container">
                                <div class="timeline-track">
                                    <!-- 当前播放位置线 -->
                                    <div class="current-time-line"></div>
                                    
                                    <!-- 标准节拍点（跟随模式显示） -->
                                    <div 
                                        v-if="practiceMode === 'follow'"
                                        v-for="beat in visibleStandardBeats" 
                                        :key="'std-' + beat.index"
                                        class="standard-beat"
                                        :class="{ 
                                            'beat-passed': beat.time < currentTime,
                                            'beat-upcoming': beat.time >= currentTime && beat.time <= currentTime + 2
                                        }"
                                        :style="{ left: getBeatPosition(beat.time) + '%' }"
                                    >
                                        <div class="beat-marker"></div>
                                    </div>

                                    <!-- 用户击打点 -->
                                    <div 
                                        v-for="(hit, index) in userHits" 
                                        :key="'hit-' + index"
                                        class="user-hit"
                                        :class="'hit-' + hit.result"
                                        :style="{ left: getBeatPosition(hit.time) + '%' }"
                                        :title="`${hit.result}: ${hit.error > 0 ? '+' : ''}${hit.error}ms`"
                                    >
                                        <div class="hit-marker"></div>
                                    </div>
                                </div>
                                
                                <div class="timeline-labels">
                                    <span>{{ formatTime(Math.max(0, currentTime - 2)) }}</span>
                                    <span style="font-weight: 600; color: #409eff;">当前</span>
                                    <span>{{ formatTime(Math.min(audioDuration, currentTime + 2)) }}</span>
                                </div>
                            </div>

                            <!-- 击打提示 -->
                            <el-alert 
                                title="按空格键或点击屏幕进行击打" 
                                type="info" 
                                :closable="false"
                                center
                                style="margin-top: 15px;"
                            >
                                <template #default>
                                    <div style="display: flex; justify-content: center; gap: 20px; font-size: 12px;">
                                        <span>🟢 Perfect (±100ms)</span>
                                        <span>🟡 Good (±150ms)</span>
                                        <span>⚪ OK (±200ms)</span>
                                        <span>🔴 Miss (>200ms)</span>
                                    </div>
                                </template>
                            </el-alert>
                        </div>

                        <!-- 反馈提示 -->
                        <transition name="el-fade-in">
                            <div v-if="lastFeedback" class="feedback-tip">
                                <el-tag 
                                    :type="getFeedbackType(lastFeedback.result)" 
                                    size="large"
                                    effect="dark"
                                >
                                    {{ lastFeedback.text }}
                                    <span v-if="lastFeedback.error !== null">
                                        ({{ lastFeedback.error > 0 ? '+' : '' }}{{ lastFeedback.error }}ms)
                                    </span>
                                </el-tag>
                            </div>
                        </transition>
                    </div>

                    <!-- 等待状态 -->
                    <el-empty 
                        v-if="!isPlaying && !showResults" 
                        description="选择音乐和节拍数据后，点击开始练习"
                        :image-size="200"
                    >
                        <template #image>
                            <el-icon :size="100" color="#409eff"><Headset /></el-icon>
                        </template>
                        <el-divider />
                        <div class="tips-box">
                            <h4><el-icon><InfoFilled /></el-icon> 练习说明</h4>
                            <ul>
                                <li>🎵 跟随音乐节奏，在节拍点位置按下空格键</li>
                                <li>🎯 <strong>跟随模式</strong>：显示标准节拍位置作为提示</li>
                                <li>👁️ <strong>盲打模式</strong>：不显示提示，完全依靠听觉</li>
                                <li>📊 系统会对比你的击打和标准节拍数据</li>
                                <li>🏆 练习结束后查看详细分析报告</li>
                            </ul>
                        </div>
                    </el-empty>

                    <!-- 结果展示 -->
                    <div v-if="showResults" class="results-container">
                        <el-result 
                            :icon="getRankIcon()"
                            :title="'练习完成'"
                            :sub-title="`准确率: ${results.accuracy}%`"
                        >
                            <template #extra>
                                <el-descriptions :column="2" border>
                                    <el-descriptions-item label="音乐">
                                        {{ results.musicName }}
                                    </el-descriptions-item>
                                    <el-descriptions-item label="模式">
                                        {{ results.mode === 'follow' ? '跟随模式' : '盲打模式' }}
                                    </el-descriptions-item>
                                    <el-descriptions-item label="准确率">
                                        <el-tag :type="getAccuracyTagType(results.accuracy)" size="large">
                                            {{ results.accuracy }}%
                                        </el-tag>
                                    </el-descriptions-item>
                                    <el-descriptions-item label="得分">
                                        <strong>{{ results.score }}</strong>
                                    </el-descriptions-item>
                                    <el-descriptions-item label="命中数">
                                        {{ results.hitCount }} / {{ results.totalBeats }}
                                    </el-descriptions-item>
                                    <el-descriptions-item label="平均误差">
                                        ±{{ results.avgError }}ms
                                    </el-descriptions-item>
                                    <el-descriptions-item label="最高连击">
                                        🔥 {{ results.maxCombo }}
                                    </el-descriptions-item>
                                    <el-descriptions-item label="总击打数">
                                        {{ results.totalHits }}
                                    </el-descriptions-item>
                                </el-descriptions>

                                <el-divider />

                                <h4>击打分布</h4>
                                <el-row :gutter="10" style="margin-bottom: 20px;">
                                    <el-col :span="6">
                                        <el-statistic title="Perfect" :value="results.perfect" />
                                    </el-col>
                                    <el-col :span="6">
                                        <el-statistic title="Good" :value="results.good" />
                                    </el-col>
                                    <el-col :span="6">
                                        <el-statistic title="OK" :value="results.ok" />
                                    </el-col>
                                    <el-col :span="6">
                                        <el-statistic title="Miss" :value="results.miss" />
                                    </el-col>
                                </el-row>

                                <el-button type="primary" icon="RefreshRight" @click="restartPractice">
                                    再练一次
                                </el-button>
                                <el-button type="success" icon="TrendCharts" @click="showIntelligentAnalysis">
                                    智能分析
                                </el-button>
                                <el-button type="warning" icon="Download" @click="downloadReport">
                                    导出报告
                                </el-button>
                                <el-button icon="Back" @click="closeResults">返回</el-button>
                            </template>
                        </el-result>
                    </div>
                </el-card>
            </el-col>

            <!-- 右侧：实时分析 -->
            <el-col :span="6">
                <el-card shadow="hover">
                    <template #header>
                        <span>误差分析</span>
                    </template>
                    <div class="chart-container">
                        <div v-if="errorHistory.length > 0">
                            <svg :width="chartWidth" :height="chartHeight" class="error-chart">
                                <!-- 零线 -->
                                <line 
                                    :x1="0" 
                                    :y1="chartHeight / 2" 
                                    :x2="chartWidth" 
                                    :y2="chartHeight / 2" 
                                    stroke="#dcdfe6" 
                                    stroke-width="2"
                                />
                                <!-- 容差区域 -->
                                <rect 
                                    :x="0" 
                                    :y="chartHeight / 2 - 30" 
                                    :width="chartWidth" 
                                    :height="60" 
                                    fill="#67c23a" 
                                    opacity="0.1"
                                />
                                <!-- 误差折线 -->
                                <polyline
                                    :points="errorChartPoints"
                                    fill="none"
                                    stroke="#409eff"
                                    stroke-width="2"
                                />
                                <!-- 数据点 -->
                                <circle
                                    v-for="(point, index) in errorChartCircles"
                                    :key="index"
                                    :cx="point.x"
                                    :cy="point.y"
                                    r="3"
                                    :fill="point.color"
                                />
                            </svg>
                            <div class="chart-legend">
                                <div style="font-size: 12px; color: #909399; text-align: center;">
                                    绿色区域：容差范围内
                                </div>
                            </div>
                        </div>
                        <el-empty v-else description="开始练习后显示" :image-size="80" />
                    </div>
                </el-card>

                <!-- 统计面板 -->
                <el-card shadow="hover" style="margin-top: 20px;" v-if="isPlaying && !showCountdown">
                    <template #header>
                        <span>练习统计</span>
                    </template>
                    <div class="stats-list">
                        <div class="stat-item">
                            <span class="stat-label">已过节拍:</span>
                            <span class="stat-value">{{ passedBeatsCount }} / {{ standardBeats.length }}</span>
                        </div>
                        <div class="stat-item">
                            <span class="stat-label">遗漏:</span>
                            <span class="stat-value" style="color: #f56c6c;">{{ missedBeatsCount }}</span>
                        </div>
                        <div class="stat-item">
                            <span class="stat-label">误击:</span>
                            <span class="stat-value" style="color: #e6a23c;">{{ falseHitsCount }}</span>
                        </div>
                        <div class="stat-item">
                            <span class="stat-label">最大连击:</span>
                            <span class="stat-value" style="color: #67c23a;">{{ maxCombo }}</span>
                        </div>
                    </div>
                    
                    <el-progress 
                        :percentage="progressPercent" 
                        :stroke-width="10"
                        style="margin-top: 15px;"
                    />
                </el-card>

                <!-- 排行榜 -->
                <el-card shadow="hover" style="margin-top: 20px;" v-if="selectedBeatdata">
                    <template #header>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <span>🏆 排行榜</span>
                            <el-button type="primary" size="small" @click="loadLeaderboard" :loading="leaderboardLoading">
                                刷新
                            </el-button>
                        </div>
                    </template>
                    
                    <div v-if="leaderboard.length > 0">
                        <el-table :data="leaderboard" size="small" max-height="300">
                            <el-table-column label="排名" width="60" align="center">
                                <template #default="scope">
                                    <el-tag 
                                        v-if="scope.$index < 3" 
                                        :type="['', 'warning', 'info'][scope.$index]"
                                        effect="dark"
                                    >
                                        {{ scope.$index + 1 }}
                                    </el-tag>
                                    <span v-else>{{ scope.$index + 1 }}</span>
                                </template>
                            </el-table-column>
                            <el-table-column label="用户" prop="userName" show-overflow-tooltip min-width="80" />
                            <el-table-column label="准确率" width="80" align="center">
                                <template #default="scope">
                                    <el-tag :type="getAccuracyTagType(scope.row.accuracy)" size="small">
                                        {{ scope.row.accuracy }}%
                                    </el-tag>
                                </template>
                            </el-table-column>
                            <el-table-column label="得分" prop="score" width="70" align="center" />
                        </el-table>
                        
                        <el-divider v-if="userBestRecord" />
                        
                        <div v-if="userBestRecord" class="user-best-record">
                            <el-alert type="success" :closable="false">
                                <template #default>
                                    <div style="font-size: 12px;">
                                        <strong>我的最佳：</strong>第 {{ userRank }} 名 | 
                                        准确率 {{ userBestRecord.accuracy }}% | 
                                        得分 {{ userBestRecord.score }}
                                    </div>
                                </template>
                            </el-alert>
                        </div>
                    </div>
                    <el-empty v-else description="暂无排行榜数据" :image-size="60" />
                </el-card>

                <!-- 提示面板 -->
                <el-card shadow="hover" style="margin-top: 20px;">
                    <template #header>
                        <span>练习技巧</span>
                    </template>
                    <div class="tips-list">
                        <el-alert type="success" :closable="false" style="margin-bottom: 10px;">
                            <template #title>
                                <div style="font-size: 12px;">
                                    👂 用耳朵感受音乐节奏
                                </div>
                            </template>
                        </el-alert>
                        <el-alert type="warning" :closable="false" style="margin-bottom: 10px;">
                            <template #title>
                                <div style="font-size: 12px;">
                                    🎯 盲打模式最锻炼节奏感
                                </div>
                            </template>
                        </el-alert>
                        <el-alert type="info" :closable="false">
                            <template #title>
                                <div style="font-size: 12px;">
                                    💪 多次练习同一首歌曲
                                </div>
                            </template>
                        </el-alert>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <!-- 隐藏的音频播放器 -->
        <audio ref="audioPlayer" @timeupdate="updateTime" @ended="handleAudioEnd" @loadedmetadata="handleAudioLoaded"></audio>
        
        <!-- 智能分析对话框 -->
        <el-dialog v-model="showAnalysis" title="智能分析报告" width="600px">
            <div v-if="intelligentAnalysis" class="analysis-content">
                <el-alert :title="intelligentAnalysis.summary" type="info" :closable="false" style="margin-bottom: 20px;" />
                
                <el-divider content-position="left">
                    <el-icon><CircleCheck /></el-icon>
                    优点
                </el-divider>
                <ul class="analysis-list">
                    <li v-for="(item, index) in intelligentAnalysis.strengths" :key="'s-'+index" style="color: #67c23a;">
                        ✓ {{ item }}
                    </li>
                </ul>
                
                <el-divider content-position="left">
                    <el-icon><Warning /></el-icon>
                    需要改进
                </el-divider>
                <ul class="analysis-list">
                    <li v-for="(item, index) in intelligentAnalysis.weaknesses" :key="'w-'+index" style="color: #e6a23c;">
                        ⚠ {{ item }}
                    </li>
                </ul>
                
                <el-divider content-position="left">
                    <el-icon><Promotion /></el-icon>
                    建议
                </el-divider>
                <ul class="analysis-list">
                    <li v-for="(item, index) in intelligentAnalysis.suggestions" :key="'sg-'+index" style="color: #409eff;">
                        💡 {{ item }}
                    </li>
                </ul>
            </div>
            
            <template #footer>
                <el-button @click="showAnalysis = false">关闭</el-button>
                <el-button type="primary" @click="downloadReport">导出完整报告</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import { 
    VideoPlay, VideoPause, TrophyBase, Aim, Operation, Select,
    Headset, InfoFilled, RefreshRight, Back, TrendCharts, Download,
    CircleCheck, Warning, Promotion
} from '@element-plus/icons-vue';
import { listMusic } from '@/api/music/music_info';
import { getBeatdata, listBeatdata } from '@/api/music_anaysis/beatdata';
import { 
    saveExerciseRecord, 
    getLeaderboard, 
    getUserBestRecord, 
    getExerciseHistory,
    getIntelligentAnalysis 
} from '@/api/music_anaysis/exercise';

// ========== 数据定义 ==========
const settingForm = ref({});
const selectedMusic = ref(null);
const selectedBeatdata = ref(null);
const playbackSpeed = ref(1.0);
const practiceMode = ref('follow');

const musicList = ref([]);
const beatdataList = ref([]);
const beatdataLoading = ref(false);
const currentMusicInfo = ref(null);
const standardBeats = ref([]);

const isPlaying = ref(false);
const showCountdown = ref(false);
const countdownNumber = ref(3);
const showResults = ref(false);
const audioPlaying = ref(false);

const audioPlayer = ref(null);
const currentTime = ref(0);
const audioDuration = ref(0);
const progressPercent = computed(() => {
    if (audioDuration.value === 0) return 0;
    return Math.round((currentTime.value / audioDuration.value) * 100);
});

// 游戏状态
const userHits = ref([]);
const hitCount = ref(0);
const accuracy = ref(0);
const currentCombo = ref(0);
const maxCombo = ref(0);
const errorHistory = ref([]);
const lastFeedback = ref(null);

// 统计
const todayCount = ref(0);
const bestAccuracy = ref(0);
const practiceHistory = ref([]);
const currentRecordId = ref(null);

// 固定容差配置（不随速度变化）
const toleranceConfig = {
    perfect: 100,  // ±100ms
    good: 150,     // ±150ms
    ok: 200        // ±200ms
};

// 排行榜数据
const leaderboard = ref([]);
const userBestRecord = ref(null);
const userRank = ref(null);
const leaderboardLoading = ref(false);

// 智能分析
const intelligentAnalysis = ref(null);
const showAnalysis = ref(false);

// 可见的标准节拍（当前时间前后2秒）
const visibleStandardBeats = computed(() => {
    const minTime = currentTime.value - 2;
    const maxTime = currentTime.value + 2;
    return standardBeats.value.filter(beat => 
        beat.time >= minTime && beat.time <= maxTime
    );
});

// 已过去的节拍数
const passedBeatsCount = computed(() => {
    return standardBeats.value.filter(beat => beat.time < currentTime.value).length;
});

// 遗漏的节拍数
const missedBeatsCount = computed(() => {
    return standardBeats.value.filter(beat => 
        beat.time < currentTime.value && !beat.matched
    ).length;
});

// 误击数（没有匹配到标准节拍的击打）
const falseHitsCount = computed(() => {
    return userHits.value.filter(hit => hit.result === 'miss').length;
});

// 结果
const results = ref({
    musicName: '',
    mode: '',
    accuracy: 0,
    score: 0,
    hitCount: 0,
    totalBeats: 0,
    totalHits: 0,
    avgError: 0,
    maxCombo: 0,
    perfect: 0,
    good: 0,
    ok: 0,
    miss: 0
});

// 图表
const chartWidth = 240;
const chartHeight = 120;
const errorChartPoints = computed(() => {
    if (errorHistory.value.length === 0) return '';
    const data = errorHistory.value.slice(-15);
    const xStep = chartWidth / Math.max(data.length - 1, 1);
    const maxError = 200;
    return data.map((error, index) => {
        const x = index * xStep;
        const y = chartHeight / 2 - (error / maxError) * (chartHeight / 2 - 10);
        return `${x},${y}`;
    }).join(' ');
});

const errorChartCircles = computed(() => {
    if (errorHistory.value.length === 0) return [];
    const data = errorHistory.value.slice(-15);
    const xStep = chartWidth / Math.max(data.length - 1, 1);
    const maxError = 200;
    return data.map((error, index) => {
        const x = index * xStep;
        const y = chartHeight / 2 - (error / maxError) * (chartHeight / 2 - 10);
        const absError = Math.abs(error);
        let color = '#67c23a';
        if (absError > toleranceConfig.ok) color = '#f56c6c';
        else if (absError > toleranceConfig.good) color = '#909399';
        else if (absError > toleranceConfig.perfect) color = '#e6a23c';
        return { x, y, color };
    });
});

// 定时器
let countdownIntervalId = null;

// ========== 方法 ==========

// 加载音乐列表
async function loadMusicList() {
    try {
        const response = await listMusic({ pageNum: 1, pageSize: 100 });
        if (response.rows) {
            // 为每个音乐加载节拍数量
            for (const music of response.rows) {
                const beatdataResponse = await listBeatdata({ 
                    musicName: music.name,  // 使用 name 字段
                    pageNum: 1,
                    pageSize: 1
                });
                music.beatCount = beatdataResponse.total || 0;
            }
            musicList.value = response.rows;
        }
    } catch (error) {
        ElMessage.error('加载音乐列表失败');
        console.error(error);
    }
}

// 音乐改变
async function handleMusicChange() {
    selectedBeatdata.value = null;
    beatdataList.value = [];
    currentMusicInfo.value = musicList.value.find(m => m.id === selectedMusic.value);
    
    if (!currentMusicInfo.value) return;
    
    // 加载该音乐的所有节拍数据
    beatdataLoading.value = true;
    try {
        const response = await listBeatdata({ 
            musicName: currentMusicInfo.value.name,  // 使用 name 字段
            pageNum: 1,
            pageSize: 100
        });
        if (response.rows && response.rows.length > 0) {
            beatdataList.value = response.rows;
            selectedBeatdata.value = beatdataList.value[0].id;
            ElMessage.success(`已加载 ${response.rows.length} 个节拍数据`);
        } else {
            ElMessage.warning('该音乐暂无节拍数据，请先在波形分析页面标注');
        }
    } catch (error) {
        ElMessage.error('加载节拍数据失败: ' + error.message);
        console.error(error);
    } finally {
        beatdataLoading.value = false;
    }
}

// 格式化时长
function formatDuration(seconds) {
    if (!seconds || isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
}

function formatTime(seconds) {
    if (!seconds || isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
}

// 获取节拍在时间轴上的位置（百分比）
function getBeatPosition(time) {
    const minTime = currentTime.value - 2;
    const maxTime = currentTime.value + 2;
    const range = maxTime - minTime;
    return ((time - minTime) / range) * 100;
}

// 开始练习
async function startPractice() {
    if (!selectedMusic.value || !selectedBeatdata.value) {
        ElMessage.warning('请先选择音乐和节拍数据');
        return;
    }

    try {
        // 加载节拍数据
        const beatdataResponse = await getBeatdata(selectedBeatdata.value);
        const beatTimes = JSON.parse(beatdataResponse.data.beatTimes);
        standardBeats.value = beatTimes.map((time, index) => ({
            time: time,
            index: index,
            matched: false
        }));

        // 加载音频
        const audioUrl = `http://localhost:8080/files/${selectedMusic.value}`;
        audioPlayer.value.src = audioUrl;
        
        // 等待音频加载
        await new Promise((resolve, reject) => {
            audioPlayer.value.onloadeddata = resolve;
            audioPlayer.value.onerror = reject;
        });

        // 显示倒计时
        showCountdown.value = true;
        countdownNumber.value = 3;
        
        countdownIntervalId = setInterval(() => {
            countdownNumber.value--;
            if (countdownNumber.value === 0) {
                clearInterval(countdownIntervalId);
                showCountdown.value = false;
                actualStartPractice();
            }
        }, 1000);
    } catch (error) {
        ElMessage.error('加载失败: ' + error.message);
        console.error(error);
    }
}

// 实际开始
function actualStartPractice() {
    isPlaying.value = true;
    userHits.value = [];
    hitCount.value = 0;
    accuracy.value = 0;
    currentCombo.value = 0;
    maxCombo.value = 0;
    errorHistory.value = [];
    lastFeedback.value = null;
    
    // 设置播放速度并播放音频
    audioPlayer.value.playbackRate = playbackSpeed.value;
    audioPlayer.value.play();
    audioPlaying.value = true;
}

// 音频时间更新
function updateTime() {
    currentTime.value = audioPlayer.value.currentTime;
}

// 音频加载完成
function handleAudioLoaded() {
    audioDuration.value = audioPlayer.value.duration;
}

// 音频播放结束
function handleAudioEnd() {
    endPractice();
}

// 切换播放
function toggleAudioPlay() {
    if (audioPlaying.value) {
        audioPlayer.value.pause();
    } else {
        audioPlayer.value.play();
    }
    audioPlaying.value = !audioPlaying.value;
}

// 跳转音频
function seekAudio(value) {
    audioPlayer.value.currentTime = (value / 100) * audioDuration.value;
}

// 处理击打
function handleHit() {
    if (!isPlaying.value || showCountdown.value) return;
    
    const hitTime = currentTime.value;
    
    // 查找最近的未匹配标准节拍
    let closestBeat = null;
    let minError = Infinity;
    
    standardBeats.value.forEach(beat => {
        if (beat.matched) return;
        const error = Math.abs(beat.time - hitTime);
        if (error < minError) {
            minError = error;
            closestBeat = beat;
        }
    });
    
    let result = 'miss';
    let errorMs = 0;
    
    if (closestBeat && minError <= toleranceConfig.ok / 1000) {
        // 在容差范围内
        errorMs = Math.round((hitTime - closestBeat.time) * 1000);
        const absError = Math.abs(errorMs);
        
        if (absError <= toleranceConfig.perfect) {
            result = 'perfect';
        } else if (absError <= toleranceConfig.good) {
            result = 'good';
        } else {
            result = 'ok';
        }
        
        closestBeat.matched = true;
        hitCount.value++;
        currentCombo.value++;
        if (currentCombo.value > maxCombo.value) {
            maxCombo.value = currentCombo.value;
        }
        
        errorHistory.value.push(errorMs);
    } else {
        // 误击
        result = 'miss';
        errorMs = null;
        currentCombo.value = 0;
    }
    
    userHits.value.push({
        time: hitTime,
        result: result,
        error: errorMs
    });
    
    updateAccuracy();
    showFeedback(result, errorMs);
}

// 更新准确率
function updateAccuracy() {
    if (standardBeats.value.length === 0) {
        accuracy.value = 0;
        return;
    }
    accuracy.value = Math.round((hitCount.value / standardBeats.value.length) * 100);
}

// 显示反馈
function showFeedback(result, error) {
    const texts = {
        perfect: 'Perfect!',
        good: 'Good',
        ok: 'OK',
        miss: 'Miss'
    };
    
    lastFeedback.value = {
        result: result,
        text: texts[result],
        error: error
    };
    
    setTimeout(() => {
        lastFeedback.value = null;
    }, 800);
}

// 停止练习
function stopPractice() {
    endPractice();
}

// 结束练习
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
        ElMessage.success('练习记录已保存到数据库');
        
        // 刷新排行榜
        loadLeaderboard();
    } catch (error) {
        console.error('保存记录失败:', error);
        ElMessage.error('保存记录失败: ' + error.message);
    }
    
    showResults.value = true;
    saveToHistory();
    todayCount.value++;
}

// 计算结果
function calculateResults() {
    const perfectCount = userHits.value.filter(h => h.result === 'perfect').length;
    const goodCount = userHits.value.filter(h => h.result === 'good').length;
    const okCount = userHits.value.filter(h => h.result === 'ok').length;
    const missCount = userHits.value.filter(h => h.result === 'miss').length;
    
    const validErrors = errorHistory.value.filter(e => e !== null);
    const avgError = validErrors.length > 0 
        ? Math.round(validErrors.reduce((a, b) => a + Math.abs(b), 0) / validErrors.length)
        : 0;
    
    const score = perfectCount * 100 + goodCount * 60 + okCount * 30;
    
    results.value = {
        musicName: currentMusicInfo.value.name,  // 使用 name 字段
        mode: practiceMode.value,
        accuracy: accuracy.value,
        score: score,
        hitCount: hitCount.value,
        totalBeats: standardBeats.value.length,
        totalHits: userHits.value.length,
        avgError: avgError,
        maxCombo: maxCombo.value,
        perfect: perfectCount,
        good: goodCount,
        ok: okCount,
        miss: missCount
    };
    
    if (accuracy.value > bestAccuracy.value) {
        bestAccuracy.value = accuracy.value;
        localStorage.setItem('beatExerciseBestAccuracy', bestAccuracy.value.toString());
    }
}

// 保存历史
function saveToHistory() {
    const record = {
        musicName: currentMusicInfo.value.name,  // 使用 name 字段
        accuracy: accuracy.value,
        hitCount: hitCount.value,
        totalBeats: standardBeats.value.length,
        avgError: results.value.avgError,
        timestamp: Date.now()
    };
    
    practiceHistory.value.unshift(record);
    if (practiceHistory.value.length > 20) {
        practiceHistory.value = practiceHistory.value.slice(0, 20);
    }
    localStorage.setItem('beatExerciseHistory', JSON.stringify(practiceHistory.value));
}

// 加载历史
function loadHistory() {
    const saved = localStorage.getItem('beatExerciseHistory');
    if (saved) {
        practiceHistory.value = JSON.parse(saved);
    }
    
    const today = new Date().toDateString();
    const savedToday = localStorage.getItem('beatExerciseToday');
    if (savedToday === today) {
        const count = localStorage.getItem('beatExerciseTodayCount');
        todayCount.value = count ? parseInt(count) : 0;
    } else {
        localStorage.setItem('beatExerciseToday', today);
        localStorage.setItem('beatExerciseTodayCount', '0');
    }
    
    const savedBest = localStorage.getItem('beatExerciseBestAccuracy');
    if (savedBest) {
        bestAccuracy.value = parseInt(savedBest);
    }
}

// 重新开始
function restartPractice() {
    showResults.value = false;
    startPractice();
}

// 关闭结果
function closeResults() {
    showResults.value = false;
}

// 工具函数
function getAccuracyTagType(acc) {
    if (acc >= 90) return 'success';
    if (acc >= 80) return 'warning';
    if (acc >= 70) return '';
    return 'danger';
}

function getFeedbackType(result) {
    const types = { perfect: 'success', good: 'warning', ok: 'info', miss: 'danger' };
    return types[result] || 'info';
}

function getRankIcon() {
    if (results.value.accuracy >= 90) return 'success';
    if (results.value.accuracy >= 70) return 'info';
    return 'warning';
}

function formatTimestamp(timestamp) {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now - date;
    
    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
    if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
    return date.toLocaleDateString();
}

// 加载排行榜
async function loadLeaderboard() {
    if (!selectedBeatdata.value) return;
    
    leaderboardLoading.value = true;
    try {
        // 加载排行榜
        const leaderboardResponse = await getLeaderboard({
            beatdataId: selectedBeatdata.value,
            playbackSpeed: playbackSpeed.value,
            practiceMode: practiceMode.value,
            limit: 10
        });
        
        leaderboard.value = leaderboardResponse.data || [];
        
        // 加载用户最佳记录
        const userBestResponse = await getUserBestRecord({
            beatdataId: selectedBeatdata.value,
            playbackSpeed: playbackSpeed.value,
            practiceMode: practiceMode.value
        });
        
        if (userBestResponse.data) {
            userBestRecord.value = userBestResponse.data;
            userRank.value = userBestResponse.data.rank || null;
        } else {
            userBestRecord.value = null;
            userRank.value = null;
        }
    } catch (error) {
        console.error('加载排行榜失败:', error);
        ElMessage.error('加载排行榜失败: ' + error.message);
    } finally {
        leaderboardLoading.value = false;
    }
}

// 生成智能分析
function generateIntelligentAnalysis() {
    const analysis = {
        summary: '',
        strengths: [],
        weaknesses: [],
        suggestions: []
    };
    
    const acc = results.value.accuracy;
    const avgErr = results.value.avgError;
    const perfectRate = results.value.perfect / (results.value.totalHits || 1) * 100;
    
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
    
    if (analysis.strengths.length === 0) {
        analysis.strengths.push('继续努力，每次练习都是进步');
    }
    
    if (analysis.weaknesses.length === 0) {
        analysis.weaknesses.push('暂无明显不足');
    }
    
    intelligentAnalysis.value = analysis;
}

// 显示智能分析
async function showIntelligentAnalysis() {
    if (!currentRecordId.value) {
        // 如果没有保存的记录ID，使用本地生成的分析
        generateIntelligentAnalysis();
        showAnalysis.value = true;
        return;
    }
    
    try {
        const response = await getIntelligentAnalysis(currentRecordId.value);
        if (response.data) {
            intelligentAnalysis.value = response.data;
        } else {
            generateIntelligentAnalysis();
        }
        showAnalysis.value = true;
    } catch (error) {
        console.error('获取智能分析失败:', error);
        // 如果获取失败，使用本地生成
        generateIntelligentAnalysis();
        showAnalysis.value = true;
    }
}

// 导出报告
async function downloadReport() {
    if (!currentRecordId.value) {
        ElMessage.warning('请先完成一次练习');
        return;
    }
    
    try {
        // 使用后端API导出报告
        const response = await fetch(`/dev-api/music_anaysis/exercise/export/${currentRecordId.value}`, {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            }
        });
        
        if (!response.ok) {
            throw new Error('导出失败');
        }
        
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        
        // 从响应头获取文件名
        const contentDisposition = response.headers.get('Content-Disposition');
        let filename = `节拍练习报告_${new Date().getTime()}.txt`;
        if (contentDisposition) {
            const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(contentDisposition);
            if (matches != null && matches[1]) {
                filename = decodeURIComponent(matches[1].replace(/['"]/g, ''));
            }
        }
        
        link.download = filename;
        link.click();
        window.URL.revokeObjectURL(url);
        ElMessage.success('报告导出成功');
    } catch (error) {
        console.error('导出报告失败:', error);
        ElMessage.error('导出报告失败: ' + error.message);
    }
}

// 键盘事件
function handleKeyDown(e) {
    if (e.code === 'Space' && isPlaying.value && !showCountdown.value) {
        e.preventDefault();
        handleHit();
    }
}

// 鼠标点击
function handleClick(e) {
    // 只在游戏区域点击时触发
    if (isPlaying.value && !showCountdown.value && e.target.closest('.visualization-area')) {
        handleHit();
    }
}

// 监听节拍数据变化，自动加载排行榜
watch(selectedBeatdata, (newVal) => {
    if (newVal) {
        loadLeaderboard();
    } else {
        leaderboard.value = [];
        userBestRecord.value = null;
        userRank.value = null;
    }
});

// 监听播放速度和模式变化，刷新排行榜
watch([playbackSpeed, practiceMode], () => {
    if (selectedBeatdata.value) {
        loadLeaderboard();
    }
});

// 生命周期
onMounted(() => {
    loadMusicList();
    loadHistory();
    document.addEventListener('keydown', handleKeyDown);
    document.addEventListener('click', handleClick);
});

onUnmounted(() => {
    if (countdownIntervalId) clearInterval(countdownIntervalId);
    if (audioPlayer.value) {
        audioPlayer.value.pause();
    }
    
    document.removeEventListener('keydown', handleKeyDown);
    document.removeEventListener('click', handleClick);
    
    localStorage.setItem('beatExerciseTodayCount', todayCount.value.toString());
});
</script>

<style scoped lang="scss">
.beat-exercise {
    .page-title {
        margin: 0 0 5px 0;
        font-size: 20px;
        font-weight: 600;
        color: #303133;
    }

    .page-desc {
        margin: 0;
        font-size: 13px;
        color: #909399;
    }

    .music-info {
        .info-item {
            display: flex;
            justify-content: space-between;
            padding: 8px 0;
            font-size: 13px;
            border-bottom: 1px solid #f0f0f0;
            
            &:last-child {
                border-bottom: none;
            }
            
            .info-label {
                color: #909399;
            }
        }
    }

    .audio-controls {
        display: flex;
        align-items: center;
        gap: 15px;
        margin-bottom: 20px;
        padding: 15px;
        background: #f5f7fa;
        border-radius: 4px;
        
        .progress-info {
            flex: 1;
            
            .time-text {
                display: block;
                font-size: 12px;
                color: #606266;
                margin-bottom: 5px;
            }
        }
    }

    .stat-row {
        margin-bottom: 20px;
        padding: 20px;
        background: #f5f7fa;
        border-radius: 4px;
    }

    .visualization-area {
        margin: 20px 0;
        padding: 20px;
        background: #f5f7fa;
        border-radius: 4px;
    }

    .timeline-container {
        margin-bottom: 15px;
    }

    .timeline-track {
        position: relative;
        height: 60px;
        background: #fff;
        border-radius: 4px;
        border: 2px solid #dcdfe6;
        overflow: hidden;
    }

    .current-time-line {
        position: absolute;
        left: 50%;
        top: 0;
        bottom: 0;
        width: 3px;
        background: #409eff;
        z-index: 10;
        transform: translateX(-50%);
        box-shadow: 0 0 10px rgba(64, 158, 255, 0.5);
    }

    .standard-beat, .user-hit {
        position: absolute;
        top: 50%;
        transform: translate(-50%, -50%);
        z-index: 5;
    }

    .standard-beat {
        .beat-marker {
            width: 4px;
            height: 40px;
            background: #909399;
            opacity: 0.3;
        }
        
        &.beat-upcoming .beat-marker {
            background: #67c23a;
            opacity: 0.6;
            animation: pulse 0.5s infinite;
        }
        
        &.beat-passed .beat-marker {
            opacity: 0.1;
        }
    }

    .user-hit {
        .hit-marker {
            width: 12px;
            height: 12px;
            border-radius: 50%;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
        }
        
        &.hit-perfect .hit-marker {
            background: #67c23a;
        }
        
        &.hit-good .hit-marker {
            background: #e6a23c;
        }
        
        &.hit-ok .hit-marker {
            background: #909399;
        }
        
        &.hit-miss .hit-marker {
            background: #f56c6c;
        }
    }

    @keyframes pulse {
        0%, 100% { transform: scale(1); }
        50% { transform: scale(1.2); }
    }

    .timeline-labels {
        display: flex;
        justify-content: space-between;
        margin-top: 5px;
        font-size: 12px;
        color: #909399;
    }

    .feedback-tip {
        text-align: center;
        margin-top: 20px;
    }

    .countdown-overlay {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 400px;
        
        .countdown-number {
            font-size: 100px;
            font-weight: 700;
            color: #409eff;
            animation: countdown-pulse 1s;
        }
        
        @keyframes countdown-pulse {
            0%, 100% { transform: scale(1); }
            50% { transform: scale(1.2); }
        }
    }

    .tips-box {
        text-align: left;
        max-width: 500px;
        margin: 0 auto;
        
        h4 {
            display: flex;
            align-items: center;
            gap: 5px;
            margin-bottom: 10px;
        }
        
        ul {
            list-style: none;
            padding: 0;
            
            li {
                padding: 8px 0;
                color: #606266;
                line-height: 1.6;
            }
        }
    }

    .results-container {
        padding: 20px;
    }

    .chart-container {
        text-align: center;
        
        .error-chart {
            margin-bottom: 10px;
        }
        
        .chart-legend {
            margin-top: 10px;
        }
    }

    .stats-list {
        .stat-item {
            display: flex;
            justify-content: space-between;
            padding: 10px 0;
            border-bottom: 1px solid #f0f0f0;
            
            &:last-child {
                border-bottom: none;
            }
            
            .stat-label {
                color: #909399;
                font-size: 13px;
            }
            
            .stat-value {
                font-weight: 600;
                font-size: 14px;
            }
        }
    }

    .tips-list {
        font-size: 12px;
    }
    
    .analysis-content {
        .analysis-list {
            list-style: none;
            padding: 0;
            
            li {
                padding: 8px 0;
                line-height: 1.6;
                font-size: 14px;
            }
        }
    }
    
    .user-best-record {
        margin-top: 10px;
    }
}
</style>
