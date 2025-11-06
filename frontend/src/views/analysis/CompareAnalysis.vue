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
                        :class="{ 'locked': dataset.locked }"
                    >
                        <div 
                            class="legend-color" 
                            :style="{ backgroundColor: dataset.color }"
                        ></div>
                        <div class="legend-info">
                            <span class="legend-text">
                                {{ dataset.creatorName || '未知用户' }} 
                                ({{ dataset.detectionMode || '未知模式' }})
                                - {{ dataset.beatCount }} 个节拍
                            </span>
                            <div class="legend-controls">
                                <el-button 
                                    :type="dataset.locked ? 'info' : 'warning'" 
                                    size="small"
                                    :icon="dataset.locked ? 'Lock' : 'Unlock'"
                                    @click="toggleLock(index)"
                                >
                                    {{ dataset.locked ? '已锁定' : '锁住' }}
                                </el-button>
                                <el-button 
                                    type="success" 
                                    size="small"
                                    icon="Check"
                                    :disabled="dataset.locked"
                                    @click="saveDataset(dataset)"
                                >
                                    保存
                                </el-button>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="tips-section">
                    <el-alert type="info" :closable="false">
                        <template #title>
                            <div style="font-size: 13px; line-height: 1.6;">
                                <div><strong>操作提示：</strong></div>
                                <div>• 按 W/A/S/D 键在当前播放位置添加节拍点</div>
                                <div>• 拖动节拍点，原标签变灰，绿色预览</div>
                                <div>• 点击绿色标签确认移动位置</div>
                                <div>• 右键绿色标签可取消移动</div>
                                <div>• 右键节拍点显示菜单（键盘移动/删除）</div>
                                <div>• 使用 ←→ 键移动选中的节拍点（0.01s/次）</div>
                                <div>• Ctrl+Z 撤销操作（需确认）</div>
                                <div>• 锁定数据集防止误操作</div>
                            </div>
                        </template>
                    </el-alert>
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
import { onMounted, ref, reactive, onUnmounted } from 'vue';
import { ElMessageBox } from 'element-plus'
import WaveForm from '@/views/analysis/waveform/waveform';
import Minimap from '@/views/analysis/waveform/plugins/minimap';
import HoverPlugin from '@/views/analysis/waveform/plugins/hover';
import RegionsPlugin from '@/views/analysis/waveform/plugins/regions';
import { getBeatdata, updateBeatdata } from '@/api/music_anaysis/beatdata';
import { listMusic } from '@/api/music/music_info';

const route = useRoute();
const router = useRouter();

const ids = route.query.ids ? route.query.ids.split(',') : [];
const musicName = route.query.musicName;

let waveformInstance = null;
let wfRegionInstance = null;
let selectedRegion = null;
let dragPreviewRegion = null; // 拖动预览标签
let originalRegion = null; // 原始标签
let isDraggingRegion = false; // 是否正在拖动
let keyPressInterval = null; // 键盘长按加速定时器
let keyPressSpeed = 100; // 键盘移动速度
let keyboardMoveStartPosition = null; // 键盘移动开始位置

const datasets = ref([]);

// 操作历史记录系统
interface HistoryAction {
    type: 'add' | 'move' | 'delete'
    regionId: string
    datasetIndex: number
    data: {
        start?: number
        end?: number
        oldStart?: number
        oldEnd?: number
        content?: string
        color?: string
    }
}

const actionHistory: HistoryAction[] = []
const MAX_HISTORY_SIZE = 50

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

// 切换锁定状态
function toggleLock(index: number) {
    datasets.value[index].locked = !datasets.value[index].locked;
    const state = datasets.value[index].locked ? '已锁定' : '已解锁';
    layer.msg(`数据集 ${datasets.value[index].creatorName} ${state}`, { icon: 1 });
    
    // 更新所有属于该数据集的region的拖动状态
    updateRegionsDragState(index, !datasets.value[index].locked);
}

// 更新指定数据集的所有region的拖动状态
function updateRegionsDragState(datasetIndex: number, draggable: boolean) {
    if (!wfRegionInstance) return;
    
    const regions = wfRegionInstance.getRegions();
    regions.forEach(region => {
        const regionDatasetIndex = parseInt(region.id.split('-')[1]);
        if (regionDatasetIndex === datasetIndex) {
            region.setOptions({ drag: draggable });
        }
    });
}

// 保存数据集
async function saveDataset(dataset: any) {
    if (dataset.locked) {
        layer.msg('数据集已锁定，无法保存', { icon: 2 });
        return;
    }
    
    try {
        await ElMessageBox.confirm(
            `确认保存数据集 "${dataset.creatorName}" 的修改吗？`,
            '确认保存',
            {
                confirmButtonText: '确认',
                cancelButtonText: '取消',
                type: 'warning',
            }
        );
        
        // 获取该数据集的所有region
        const regions = wfRegionInstance.getRegions();
        const datasetIndex = datasets.value.findIndex(d => d.id === dataset.id);
        const datasetRegions = regions.filter(r => {
            const regionDatasetIndex = parseInt(r.id.split('-')[1]);
            return regionDatasetIndex === datasetIndex;
        });
        
        // 提取并排序节拍时间
        const beatTimes = datasetRegions
            .map(r => parseFloat(r.start.toFixed(2)))
            .sort((a, b) => a - b);
        
        // 更新数据
        const updateData = {
            id: dataset.id,
            musicName: dataset.musicName,
            beatTimes: JSON.stringify(beatTimes),
            detectionMode: dataset.detectionMode,
            creatorName: dataset.creatorName
        };
        
        await updateBeatdata(updateData);
        layer.msg('保存成功', { icon: 1 });
        
        // 更新本地数据集的节拍数量
        dataset.beatCount = beatTimes.length;
        
    } catch (error) {
        if (error !== 'cancel') {
            layer.msg('保存失败: ' + error, { icon: 2 });
        }
    }
}

// 添加操作到历史记录
function addToHistory(action: HistoryAction) {
    actionHistory.push(action);
    if (actionHistory.length > MAX_HISTORY_SIZE) {
        actionHistory.shift();
    }
    console.log('记录操作:', action, '当前历史记录数:', actionHistory.length);
}

// 撤回最后一次操作
async function undoLastAction() {
    if (actionHistory.length === 0) {
        layer.msg('没有可撤回的操作', { icon: 2 });
        return;
    }
    
    const lastAction = actionHistory[actionHistory.length - 1];
    const dataset = datasets.value[lastAction.datasetIndex];
    
    // 检查数据集是否被锁定
    if (dataset && dataset.locked) {
        layer.msg('该数据集已锁定，无法撤回操作', { icon: 2 });
        return;
    }
    
    let actionDesc = '';
    if (lastAction.type === 'add') {
        actionDesc = `添加节拍点 (${lastAction.data.start?.toFixed(2)}s)`;
    } else if (lastAction.type === 'move') {
        actionDesc = `移动节拍点 (从 ${lastAction.data.oldStart?.toFixed(2)}s 到 ${lastAction.data.start?.toFixed(2)}s)`;
    } else if (lastAction.type === 'delete') {
        actionDesc = `删除节拍点 (${lastAction.data.start?.toFixed(2)}s)`;
    }
    
    try {
        await ElMessageBox.confirm(
            `确认要撤回操作: ${actionDesc} 吗？`,
            '撤回操作',
            {
                confirmButtonText: '确认',
                cancelButtonText: '取消',
                type: 'warning',
            }
        );
        
        const action = actionHistory.pop();
        if (!action) return;
        
        const regions = wfRegionInstance.getRegions();
        
        if (action.type === 'add') {
            const region = regions.find(r => r.id === action.regionId);
            if (region) {
                region.remove();
                layer.msg('已撤回添加操作', { icon: 1 });
            }
        } else if (action.type === 'move') {
            const region = regions.find(r => r.id === action.regionId);
            if (region && action.data.oldStart !== undefined) {
                region.setOptions({
                    start: action.data.oldStart,
                    end: action.data.oldStart + 0.01,
                    content: `${action.data.oldStart.toFixed(2)}s`
                });
                layer.msg('已撤回移动操作', { icon: 1 });
            }
        } else if (action.type === 'delete') {
            if (action.data.start !== undefined) {
                const region = wfRegionInstance.addRegion({
                    id: action.regionId,
                    start: action.data.start,
                    end: action.data.end || action.data.start + 0.01,
                    content: action.data.content || `${action.data.start.toFixed(2)}s`,
                    color: action.data.color || 'rgba(255, 0, 0, 0.6)',
                    drag: !dataset.locked,
                    resize: false
                });
                setupRegionEvents(region, waveformInstance, action.datasetIndex);
                layer.msg('已撤回删除操作', { icon: 1 });
            }
        }
    } catch (error) {
        console.log('用户取消了撤回操作');
    }
}

// 清理预览状态的辅助函数
function cleanupPreviewState() {
    if (dragPreviewRegion) {
        dragPreviewRegion.remove();
        dragPreviewRegion = null;
    }
    if (originalRegion) {
        const originalDatasetIndex = parseInt(originalRegion.id.split('-')[1]);
        const dataset = datasets.value[originalDatasetIndex];
        originalRegion.setOptions({
            color: dataset.color,
            drag: !dataset.locked
        });
        originalRegion = null;
    }
    isDraggingRegion = false;
}

// 为预览标签设置事件
function setupPreviewRegionEvents(previewRegion, originalRegion, waveform, originalStart, datasetIndex) {
    let previewStartBeforeDrag = previewRegion.start;
    
    previewRegion.on('update-start', () => {
        previewStartBeforeDrag = previewRegion.start;
    });
    
    previewRegion.on('update-end', () => {
        const newPreviewPosition = previewRegion.start;
        
        // 如果预览标签位置发生变化，只更新显示，不创建新的预览
        if (Math.abs(newPreviewPosition - previewStartBeforeDrag) > 0.001) {
            previewRegion.setOptions({
                content: `${newPreviewPosition.toFixed(2)}s (点击确认)`
            });
            layer.msg('请点击绿色预览标签确认移动', { icon: 1, time: 1500 });
        } else {
            previewRegion.setOptions({
                content: `${previewRegion.start.toFixed(2)}s (点击确认)`
            });
        }
    });
    
    previewRegion.on('click', async (e) => {
        if (e.button !== 0) return; // 只响应左键点击
        
        try {
            await ElMessageBox.confirm(
                `确认将节拍点从 ${originalStart.toFixed(2)}s 移动到 ${previewRegion.start.toFixed(2)}s 吗？`,
                '确认移动',
                {
                    confirmButtonText: '确认',
                    cancelButtonText: '取消',
                    type: 'info',
                }
            );
            
            const dataset = datasets.value[datasetIndex];
            
            // 用户确认，应用更改
            originalRegion.setOptions({
                start: previewRegion.start,
                end: previewRegion.start + 0.01,
                content: `${previewRegion.start.toFixed(2)}s`,
                color: dataset.color,
                drag: !dataset.locked // 恢复可拖动状态（根据锁定状态）
            });
            
            // 记录移动操作到历史
            if (Math.abs(previewRegion.start - originalStart) > 0.001) {
                addToHistory({
                    type: 'move',
                    regionId: originalRegion.id,
                    datasetIndex: datasetIndex,
                    data: {
                        oldStart: originalStart,
                        oldEnd: originalStart + 0.01,
                        start: previewRegion.start,
                        end: previewRegion.start + 0.01
                    }
                });
            }
            
            // 删除预览标签
            previewRegion.remove();
            dragPreviewRegion = null;
            originalRegion = null;
            isDraggingRegion = false;
            
            layer.msg('节拍点位置已更新', { icon: 1 });
        } catch (error) {
            // 用户取消，恢复原状态
            cleanupPreviewState();
            layer.msg('已取消移动', { icon: 2 });
        }
    });
    
    // 为预览标签添加右键菜单
    previewRegion.element.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        
        const menu = document.createElement('div');
        menu.className = 'context-menu';
        menu.style.cssText = `
            position: fixed;
            left: ${e.clientX}px;
            top: ${e.clientY}px;
            background: white;
            border: 1px solid #ccc;
            border-radius: 4px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.15);
            z-index: 10000;
            min-width: 120px;
        `;
        
        const cancelOption = document.createElement('div');
        cancelOption.textContent = '取消移动';
        cancelOption.style.cssText = `
            padding: 8px 16px;
            cursor: pointer;
            color: #f56c6c;
        `;
        cancelOption.onmouseover = () => cancelOption.style.background = '#f5f5f5';
        cancelOption.onmouseout = () => cancelOption.style.background = 'white';
        cancelOption.onclick = () => {
            document.body.removeChild(menu);
            cleanupPreviewState();
            layer.msg('已取消移动', { icon: 2 });
        };
        
        menu.appendChild(cancelOption);
        document.body.appendChild(menu);
        
        const closeMenu = (event) => {
            if (menu.parentNode && !menu.contains(event.target)) {
                document.body.removeChild(menu);
                document.removeEventListener('click', closeMenu);
            }
        };
        setTimeout(() => {
            document.addEventListener('click', closeMenu);
        }, 0);
    });
}

// 设置Region事件
function setupRegionEvents(region: any, waveform: any, datasetIndex: number) {
    let regionStartBeforeDrag = region.start; // 保存拖动前的位置
    
    region.on('update-start', () => {
        regionStartBeforeDrag = region.start; // 记录拖动开始时的位置
    });
    
    region.on('update-end', () => {
        const newPosition = region.start;
        
        // 检查数据集是否被锁定
        const dataset = datasets.value[datasetIndex];
        if (dataset && dataset.locked) {
            region.setOptions({
                start: regionStartBeforeDrag,
                end: regionStartBeforeDrag + 0.01,
                content: `${regionStartBeforeDrag.toFixed(2)}s`
            });
            layer.msg('该数据集已锁定，无法移动', { icon: 2 });
            return;
        }
        
        // 检查是否真的移动了位置
        if (Math.abs(newPosition - regionStartBeforeDrag) < 0.001) {
            region.setOptions({
                content: `${region.start.toFixed(2)}s`
            });
            return;
        }
        
        // 如果有之前的预览状态，先清理
        if (isDraggingRegion) {
            cleanupPreviewState();
        }
        
        // 位置发生了变化，进入确认模式
        isDraggingRegion = true;
        
        // 将region恢复到原位置并变为灰色
        region.setOptions({
            start: regionStartBeforeDrag,
            end: regionStartBeforeDrag + 0.01,
            content: `${regionStartBeforeDrag.toFixed(2)}s`,
            color: 'rgba(128, 128, 128, 0.3)',
            drag: false // 禁止继续拖动原标签
        });
        
        // 创建预览标签（绿色）在新位置
        dragPreviewRegion = wfRegionInstance.addRegion({
            start: newPosition,
            end: newPosition + 0.01,
            content: `${newPosition.toFixed(2)}s (点击确认)`,
            color: 'rgba(0, 200, 0, 0.6)',
            drag: true, // 预览标签可以继续拖动
            resize: false
        });
        
        // 保存原始region引用
        originalRegion = region;
        
        // 为预览标签设置点击事件
        setupPreviewRegionEvents(dragPreviewRegion, region, waveform, regionStartBeforeDrag, datasetIndex);
        
        layer.msg('请点击绿色预览标签确认移动', { icon: 1, time: 2000 });
    });
    
    region.on('click', (e) => {
        if (e.button === 2) {
            e.preventDefault();
        }
        selectedRegion = region;
    });
    
    region.element.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        
        // 如果该region正在预览模式（变灰色），提示用户先处理预览
        if (region === originalRegion && isDraggingRegion) {
            layer.msg('请先确认或取消当前的移动操作', { icon: 2 });
            return;
        }
        
        const dataset = datasets.value[datasetIndex];
        if (dataset && dataset.locked) {
            layer.msg('该数据集已锁定，无法操作', { icon: 2 });
            return;
        }
        
        selectedRegion = region;
        
        const menu = document.createElement('div');
        menu.className = 'context-menu';
        menu.style.cssText = `
            position: fixed;
            left: ${e.clientX}px;
            top: ${e.clientY}px;
            background: white;
            border: 1px solid #ccc;
            border-radius: 4px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.15);
            z-index: 10000;
            min-width: 120px;
        `;
        
        const moveOption = document.createElement('div');
        moveOption.textContent = '键盘移动 (←→)';
        moveOption.style.cssText = `
            padding: 8px 16px;
            cursor: pointer;
            border-bottom: 1px solid #eee;
        `;
        moveOption.onmouseover = () => moveOption.style.background = '#f5f5f5';
        moveOption.onmouseout = () => moveOption.style.background = 'white';
        moveOption.onclick = () => {
            document.body.removeChild(menu);
            layer.msg('请使用键盘左右键移动节拍点，每次移动0.01s', { icon: 1 });
        };
        
        const deleteOption = document.createElement('div');
        deleteOption.textContent = '删除';
        deleteOption.style.cssText = `
            padding: 8px 16px;
            cursor: pointer;
            color: #f56c6c;
        `;
        deleteOption.onmouseover = () => deleteOption.style.background = '#f5f5f5';
        deleteOption.onmouseout = () => deleteOption.style.background = 'white';
        deleteOption.onclick = () => {
            document.body.removeChild(menu);
            if (confirm(`删除节拍点 ${region.start.toFixed(2)}s?`)) {
                addToHistory({
                    type: 'delete',
                    regionId: region.id,
                    datasetIndex: datasetIndex,
                    data: {
                        start: region.start,
                        end: region.end,
                        content: `${region.start.toFixed(2)}s`,
                        color: region.color
                    }
                });
                
                region.remove();
                selectedRegion = null;
                layer.msg('节拍点已删除，请记得保存', { icon: 1 });
            }
        };
        
        menu.appendChild(moveOption);
        menu.appendChild(deleteOption);
        document.body.appendChild(menu);
        
        const closeMenu = (event) => {
            if (menu.parentNode && !menu.contains(event.target)) {
                document.body.removeChild(menu);
                document.removeEventListener('click', closeMenu);
            }
        };
        setTimeout(() => {
            document.addEventListener('click', closeMenu);
        }, 0);
    });
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
                            musicName: beatData.musicName,
                            creatorName: beatData.creatorName,
                            detectionMode: beatData.detectionMode,
                            beatCount: beatTimes.length,
                            color: color,
                            locked: false  // 默认未锁定
                        });
                        
                        beatTimes.forEach((time, index) => {
                            const timeLabel = time.toFixed(2) + 's';
                            const region = wfRegion.addRegion({
                                start: time,
                                end: time + 0.01,
                                content: timeLabel,
                                color: color,
                                drag: true,  // 默认可拖动
                                resize: false,
                                id: `beat-${i}-${index}`,
                                attributes: {
                                    'data-dataset-index': i,
                                    'data-time': timeLabel
                                }
                            });
                            
                            // 为每个region设置事件
                            setupRegionEvents(region, waveform, i);
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
        
        // 添加键盘监听
        const keydownHandler = (e: KeyboardEvent) => {
            // Ctrl+Z 撤回功能
            if (e.ctrlKey && e.key === 'z') {
                e.preventDefault();
                undoLastAction();
                return;
            }
            
            // 左右键移动选中的节拍点
            if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') {
                if (!selectedRegion) {
                    layer.msg('请先右键选择一个节拍点', { icon: 2 });
                    return;
                }
                
                e.preventDefault();
                
                // 检查选中的节拍点所属的数据集是否被锁定
                const regionDatasetIndex = parseInt(selectedRegion.id.split('-')[1]);
                const dataset = datasets.value[regionDatasetIndex];
                if (dataset && dataset.locked) {
                    layer.msg('该数据集已锁定，无法移动', { icon: 2 });
                    return;
                }
                
                // 只在第一次按下时记录初始位置
                if (keyboardMoveStartPosition === null) {
                    keyboardMoveStartPosition = selectedRegion.start;
                }
                
                const moveStep = 0.01;
                const direction = e.key === 'ArrowLeft' ? -1 : 1;
                const newStart = Math.max(0, selectedRegion.start + direction * moveStep);
                const duration = waveform.getDuration();
                
                if (newStart <= duration) {
                    selectedRegion.setOptions({
                        start: newStart,
                        end: newStart + 0.01,
                        content: `${newStart.toFixed(2)}s`
                    });
                    
                    if (!keyPressInterval) {
                        keyPressSpeed = 100;
                        keyPressInterval = setInterval(() => {
                            if (keyPressSpeed > 20) {
                                keyPressSpeed -= 10;
                            }
                        }, 200);
                    }
                }
            } 
            // W/A/S/D 键在当前播放位置添加节拍点
            else if (e.key === 'w' || e.key === 'a' || e.key === 's' || e.key === 'd') {
                // 找到第一个未锁定的数据集
                const unlockedDatasetIndex = datasets.value.findIndex(d => !d.locked);
                if (unlockedDatasetIndex === -1) {
                    layer.msg('所有数据集都已锁定，无法添加节拍点', { icon: 2 });
                    return;
                }
                
                const currentTime = waveform.getCurrentTime();
                const dataset = datasets.value[unlockedDatasetIndex];
                
                // 生成新的region ID
                const existingRegions = wfRegion.getRegions().filter(r => {
                    const regionDatasetIndex = parseInt(r.id.split('-')[1]);
                    return regionDatasetIndex === unlockedDatasetIndex;
                });
                const newIndex = existingRegions.length;
                
                const region = wfRegion.addRegion({
                    start: currentTime,
                    end: currentTime + 0.01,
                    content: `${currentTime.toFixed(2)}s`,
                    color: dataset.color,
                    drag: true,
                    resize: false,
                    id: `beat-${unlockedDatasetIndex}-${newIndex}`,
                    attributes: {
                        'data-dataset-index': unlockedDatasetIndex,
                        'data-time': `${currentTime.toFixed(2)}s`
                    }
                });
                
                setupRegionEvents(region, waveform, unlockedDatasetIndex);
                
                addToHistory({
                    type: 'add',
                    regionId: region.id,
                    datasetIndex: unlockedDatasetIndex,
                    data: {
                        start: currentTime,
                        end: currentTime + 0.01,
                        content: `${currentTime.toFixed(2)}s`,
                        color: dataset.color
                    }
                });
                
                layer.msg(`已添加节拍点到 ${dataset.creatorName}`, { icon: 1, time: 1000 });
            }
        };
        
        const keyupHandler = (e: KeyboardEvent) => {
            if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') {
                if (keyPressInterval) {
                    clearInterval(keyPressInterval);
                    keyPressInterval = null;
                    keyPressSpeed = 100;
                }
                
                // 键盘移动结束时，记录一次移动操作到历史
                if (keyboardMoveStartPosition !== null && selectedRegion) {
                    const finalPosition = selectedRegion.start;
                    if (Math.abs(finalPosition - keyboardMoveStartPosition) > 0.001) {
                        const regionDatasetIndex = parseInt(selectedRegion.id.split('-')[1]);
                        addToHistory({
                            type: 'move',
                            regionId: selectedRegion.id,
                            datasetIndex: regionDatasetIndex,
                            data: {
                                oldStart: keyboardMoveStartPosition,
                                oldEnd: keyboardMoveStartPosition + 0.01,
                                start: finalPosition,
                                end: finalPosition + 0.01
                            }
                        });
                    }
                    keyboardMoveStartPosition = null;
                }
            }
        };
        
        document.addEventListener('keydown', keydownHandler);
        document.addEventListener('keyup', keyupHandler);
        
        // 组件卸载时清理监听器
        onUnmounted(() => {
            document.removeEventListener('keydown', keydownHandler);
            document.removeEventListener('keyup', keyupHandler);
            if (keyPressInterval) {
                clearInterval(keyPressInterval);
            }
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
    align-items: flex-start;
    padding: 15px;
    background: #f5f7fa;
    border-radius: 8px;
    transition: all 0.3s;
    border: 2px solid transparent;
}

.legend-item:hover {
    background: #e8f4ff;
    border-color: #409eff;
}

.legend-item.locked {
    background: #f0f0f0;
    opacity: 0.8;
}

.legend-item.locked:hover {
    background: #e0e0e0;
    border-color: #909399;
}

.legend-color {
    width: 30px;
    height: 30px;
    border-radius: 4px;
    margin-right: 12px;
    flex-shrink: 0;
    border: 2px solid rgba(0, 0, 0, 0.1);
}

.legend-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.legend-text {
    font-size: 14px;
    color: #606266;
    line-height: 1.6;
    font-weight: 500;
}

.legend-controls {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.tips-section {
    margin-top: 20px;
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
