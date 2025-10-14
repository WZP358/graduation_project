<template>
    <div class="analysis-page">
        <div class="waveform-container">
            <div id="waveform" ref="waveform"></div>
        </div>

        <div class="content-wrapper">
            <div class="legend-section">
                <h3 class="panel-title">数据对比图例</h3>
                <div class="legend-items">
                    <div 
                        v-for="(dataset, index) in datasets" 
                        :key="dataset.id"
                        class="legend-item"
                    >
                        <div 
                            class="legend-color" 
                            :style="{ backgroundColor: dataset.color }"
                        ></div>
                        <span class="legend-text">
                            {{ dataset.creatorName || '未知用户' }} 
                            ({{ dataset.detectionMode || '未知模式' }})
                            - {{ dataset.beatCount }} 个节拍
                        </span>
                    </div>
                </div>
            </div>

            <div class="control-panel">
                <h3 class="panel-title">控制面板</h3>
                
                <div class="control-group">
                    <el-button type="primary" id="playPause">播放/暂停</el-button>
                    <el-button type="warning" @click="goBack">返回</el-button>
                </div>

                <div class="zoom-control">
                    <label>缩放比例：</label>
                    <input type="range" min="10" max="400" value="120"></input>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router';
import { layer } from '@layui/layer-vue'
import { onMounted, ref, reactive } from 'vue';
import WaveForm from '@/views/analysis/waveform/waveform';
import Minimap from '@/views/analysis/waveform/plugins/minimap';
import HoverPlugin from '@/views/analysis/waveform/plugins/hover';
import RegionsPlugin from '@/views/analysis/waveform/plugins/regions';
import { getBeatdata } from '@/api/music_anaysis/beatdata';
import { listMusic } from '@/api/music/music_info';

const route = useRoute();
const router = useRouter();

const ids = route.query.ids ? route.query.ids.split(',') : [];
const musicName = route.query.musicName;

let waveformInstance = null;
let wfRegionInstance = null;

const datasets = ref([]);

const distinctColors = [
    'rgba(255, 0, 0, 0.6)',      // 红色
    'rgba(0, 150, 255, 0.6)',    // 蓝色
    'rgba(50, 205, 50, 0.6)',    // 绿色
    'rgba(255, 140, 0, 0.6)',    // 橙色
    'rgba(148, 0, 211, 0.6)',    // 紫色
    'rgba(255, 20, 147, 0.6)',   // 粉色
    'rgba(0, 255, 255, 0.6)',    // 青色
    'rgba(255, 215, 0, 0.6)',    // 金色
    'rgba(124, 252, 0, 0.6)',    // 黄绿色
    'rgba(255, 105, 180, 0.6)',  // 热粉色
];

function goBack() {
    router.back();
}

onMounted(async () => {
    console.log('CompareAnalysis页面初始化', { ids, musicName });
    
    if (!ids || ids.length < 2) {
        layer.msg('请至少选择2条数据进行对比', { icon: 2 });
        setTimeout(() => router.back(), 2000);
        return;
    }
    
    const container = document.getElementById('waveform');
    if (!container) {
        console.error('找不到 waveform 容器');
        return;
    }
    
    try {
        const musicResponse = await listMusic({ musicName: musicName, pageNum: 1, pageSize: 1 });
        if (!musicResponse.rows || musicResponse.rows.length === 0) {
            layer.msg('未找到音乐文件', { icon: 2 });
            return;
        }
        
        const musicInfo = musicResponse.rows[0];
        const musicId = musicInfo.id;
        const audioUrl = `http://localhost:8080/files/${musicId}`;
        console.log('音频URL:', audioUrl);
        
        const waveform = WaveForm.create({
            container: container as HTMLElement,
            waveColor: 'rgb(100, 149, 237)',
            progressColor: 'rgba(100, 149, 237, 0.5)',
            url: audioUrl,
            sampleRate: 44100,
            cursorWidth: 2,
            cursorColor: '#ff0000',
            height: 'auto',
            normalize: true,
            minPxPerSec: 120,
            hideScrollbar: false,
            barWidth: 2,
            barGap: 1,
            plugins: [
                Minimap.create({
                    height: 60,
                    insertPosition: 'beforebegin',
                }),
                HoverPlugin.create()
            ]
        });
        
        console.log('WaveForm创建成功:', waveform);
        
        const wfRegion = waveform.registerPlugin(RegionsPlugin.create());
        waveformInstance = waveform;
        wfRegionInstance = wfRegion;
        
        waveform.on('load', (url) => {
            console.log('开始加载音频:', url);
        });
        
        waveform.on('loading', (percent) => {
            console.log('加载进度:', percent);
        });
        
        waveform.on('error', (error) => {
            console.error('音频加载错误:', error);
        });
        
        waveform.once('decode', async () => {
            console.log('音频解码完成，开始加载多组节拍数据');
            
            for (let i = 0; i < ids.length; i++) {
                const id = ids[i];
                const color = distinctColors[i % distinctColors.length];
                
                try {
                    const response = await getBeatdata(id);
                    console.log(`节拍数据 ${i + 1} API响应:`, response);
                    
                    if (response.data) {
                        const beatData = response.data;
                        const beatTimes = JSON.parse(beatData.beatTimes);
                        console.log(`加载节拍数据 ${i + 1}:`, beatTimes);
                        
                        datasets.value.push({
                            id: beatData.id,
                            creatorName: beatData.creatorName,
                            detectionMode: beatData.detectionMode,
                            beatCount: beatTimes.length,
                            color: color
                        });
                        
                        beatTimes.forEach((time, index) => {
                            const timeLabel = time.toFixed(2) + 's';
                            wfRegion.addRegion({
                                start: time,
                                end: time + 0.01,
                                content: timeLabel,
                                color: color,
                                drag: false,
                                resize: false,
                                id: `beat-${i}-${index}`,
                                attributes: {
                                    'data-dataset-index': i,
                                    'data-time': timeLabel
                                }
                            });
                        });
                    }
                } catch (error) {
                    console.error(`加载节拍数据 ${id} 失败:`, error);
                }
            }
            
            console.log('所有节拍数据加载完成，数据集:', datasets.value);
        });

        const playPause = document.getElementById('playPause');
        playPause?.addEventListener('click', () => {
            waveform.playPause();
        });
        
        const slider = document.querySelector('input[type="range"]');
        slider?.addEventListener('input', (e) => {
            const zoomLevel = Number((e.target as HTMLInputElement).value);
            waveform.zoom(zoomLevel);
        });
        
    } catch (error) {
        console.error('初始化失败:', error);
        layer.msg('初始化失败: ' + error, { icon: 2 });
    }
});
</script>

<style scoped>
.analysis-page {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    height: 100vh;
    background: #f5f7fa;
    padding: 20px;
    box-sizing: border-box;
    overflow: hidden;
}

.waveform-container {
    background: white;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    height: 50vh;
    min-height: 300px;
    display: flex;
    flex-direction: column;
}

#waveform {
    width: 100%;
    height: 100%;
    flex: 1;
    border-radius: 4px;
}

:deep(.wavesurfer-region) {
    z-index: 3 !important;
}

:deep(.wavesurfer-region:before) {
    content: attr(data-time);
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
    font-size: 11px;
    color: #333;
    background: rgba(255, 255, 255, 0.95);
    padding: 3px 6px;
    border-radius: 3px;
    white-space: nowrap;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
    font-weight: 500;
    z-index: 10;
    pointer-events: none;
    border: 1px solid rgba(0, 0, 0, 0.1);
}

:deep(.wavesurfer-region[data-dataset-index="0"]:before) {
    top: -25px;
}

:deep(.wavesurfer-region[data-dataset-index="1"]:before) {
    top: -50px;
}

:deep(.wavesurfer-region[data-dataset-index="2"]:before) {
    top: -75px;
}

:deep(.wavesurfer-region[data-dataset-index="3"]:before) {
    top: -100px;
}

:deep(.wavesurfer-region[data-dataset-index="4"]:before) {
    top: -125px;
}

:deep(.wavesurfer-region[data-dataset-index="5"]:before) {
    top: -150px;
}

:deep(.wavesurfer-region[data-dataset-index="6"]:before) {
    top: -175px;
}

:deep(.wavesurfer-region[data-dataset-index="7"]:before) {
    top: -200px;
}

:deep(.wavesurfer-region[data-dataset-index="8"]:before) {
    top: -225px;
}

:deep(.wavesurfer-region[data-dataset-index="9"]:before) {
    top: -250px;
}

.content-wrapper {
    display: flex;
    gap: 20px;
    flex: 1;
    min-height: 0;
}

.legend-section {
    flex: 1;
    background: white;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    min-width: 0;
    overflow-y: auto;
}

.control-panel {
    width: 280px;
    min-width: 280px;
    background: white;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    overflow-y: auto;
}

.panel-title {
    margin: 0 0 20px 0;
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    border-bottom: 2px solid #1890ff;
    padding-bottom: 10px;
}

.legend-items {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.legend-item {
    display: flex;
    align-items: center;
    padding: 10px;
    background: #f5f7fa;
    border-radius: 4px;
    transition: all 0.3s;
}

.legend-item:hover {
    background: #e8f4ff;
    transform: translateX(5px);
}

.legend-color {
    width: 30px;
    height: 30px;
    border-radius: 4px;
    margin-right: 12px;
    flex-shrink: 0;
    border: 2px solid rgba(0, 0, 0, 0.1);
}

.legend-text {
    font-size: 14px;
    color: #606266;
    line-height: 1.4;
}

.control-group {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-bottom: 20px;
}

.zoom-control {
    margin-bottom: 20px;
}

.zoom-control label {
    display: block;
    margin-bottom: 8px;
    font-size: 14px;
    color: #606266;
    font-weight: 500;
}

.zoom-control input[type="range"] {
    width: 100%;
}
</style>
