<template>
    <div class="analysis-page">
        <div class="waveform-container">
            <div id="waveform" ref="waveform"></div>
        </div>

        <div class="content-wrapper">
            <div class="chart-section">
                <Result ref="RefResult" :speedArray="speedArray"></Result>
            </div>

            <div class="control-panel">
                <h3 class="panel-title">控制面板</h3>
                
                <div class="control-group">
                    <el-button type="primary" id="playPause">播放/暂停</el-button>
                    <el-button type="warning" @click="saveBeats()">保存节拍修改</el-button>
                </div>

                <div class="zoom-control">
                    <label>缩放比例：</label>
                    <input type="range" min="10" max="400" value="120" id="zoomSlider"></input>
                </div>

                <div class="speed-control">
                    <label>播放速度：<span class="speed-value">{{ playbackSpeed }}x</span></label>
                    <input 
                        type="range" 
                        min="0.25" 
                        max="2" 
                        step="0.05" 
                        :value="playbackSpeed" 
                        @input="onSpeedChange"
                        id="speedSlider"
                    />
                    <div class="speed-presets">
                        <el-button size="small" @click="setSpeed(0.5)" :type="isSpeedActive(0.5) ? 'primary' : ''">0.5x</el-button>
                        <el-button size="small" @click="setSpeed(0.75)" :type="isSpeedActive(0.75) ? 'primary' : ''">0.75x</el-button>
                        <el-button size="small" @click="setSpeed(1)" :type="isSpeedActive(1) ? 'primary' : ''">1x</el-button>
                        <el-button size="small" @click="setSpeed(1.25)" :type="isSpeedActive(1.25) ? 'primary' : ''">1.25x</el-button>
                        <el-button size="small" @click="setSpeed(1.5)" :type="isSpeedActive(1.5) ? 'primary' : ''">1.5x</el-button>
                    </div>
                </div>
                
                <Options v-model:options="myOptions"></Options>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">  
import { useRoute } from 'vue-router';
import { layer } from '@layui/layer-vue'
import { onMounted, ref, onUnmounted, onBeforeMount, reactive, watchEffect } from 'vue';
import { ElMessageBox } from 'element-plus'
import WaveForm from '@/views/analysis/waveform/waveform';
import SpectrogramPlugin from '@/views/analysis/waveform/plugins/spectrogram';
import Minimap from '@/views/analysis/waveform/plugins/minimap';
import HoverPlugin from '@/views/analysis/waveform/plugins/hover';
import RegionsPlugin from '@/views/analysis/waveform/plugins/regions';
import Options from './components/Options.vue';
import Result from './components/Result.vue';
import { getBeatdataByMusicName, updateBeatdata, addBeatdata } from '@/api/music_anaysis/beatdata';

const RefResult = ref(null)
function callRedraw() {
    RefResult.value.redraw()
}

// 操作历史记录系统
interface HistoryAction {
    type: 'add' | 'move' | 'delete'
    regionId: string
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
const MAX_HISTORY_SIZE = 50 // 最多保存50条历史记录

// 添加操作到历史记录
function addToHistory(action: HistoryAction) {
    actionHistory.push(action)
    if (actionHistory.length > MAX_HISTORY_SIZE) {
        actionHistory.shift() // 移除最旧的记录
    }
    console.log('记录操作:', action, '当前历史记录数:', actionHistory.length)
}

// 撤回最后一次操作
async function undoLastAction() {
    if (actionHistory.length === 0) {
        layer.msg('没有可撤回的操作', { icon: 2 })
        return
    }
    
    const lastAction = actionHistory[actionHistory.length - 1]
    let actionDesc = ''
    
    if (lastAction.type === 'add') {
        actionDesc = `添加节拍点 (${lastAction.data.start?.toFixed(2)}s)`
    } else if (lastAction.type === 'move') {
        actionDesc = `移动节拍点 (从 ${lastAction.data.oldStart?.toFixed(2)}s 到 ${lastAction.data.start?.toFixed(2)}s)`
    } else if (lastAction.type === 'delete') {
        actionDesc = `删除节拍点 (${lastAction.data.start?.toFixed(2)}s)`
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
        )
        
        // 执行撤回操作
        const action = actionHistory.pop()
        if (!action) return
        
        const regions = wfRegionInstance.getRegions()
        
        if (action.type === 'add') {
            // 撤回添加操作：删除该region
            const region = regions.find(r => r.id === action.regionId)
            if (region) {
                region.remove()
                layer.msg('已撤回添加操作', { icon: 1 })
            } else {
                layer.msg('未找到要删除的节拍点', { icon: 2 })
            }
        } else if (action.type === 'move') {
            // 撤回移动操作：恢复到原来的位置
            const region = regions.find(r => r.id === action.regionId)
            if (region && action.data.oldStart !== undefined) {
                region.setOptions({
                    start: action.data.oldStart,
                    end: action.data.oldStart + 0.01,
                    content: `${action.data.oldStart.toFixed(2)}s`
                })
                layer.msg('已撤回移动操作', { icon: 1 })
            } else {
                layer.msg('未找到要恢复的节拍点', { icon: 2 })
            }
        } else if (action.type === 'delete') {
            // 撤回删除操作：重新添加该region
            if (action.data.start !== undefined) {
                const region = wfRegionInstance.addRegion({
                    id: action.regionId,
                    start: action.data.start,
                    end: action.data.end || action.data.start + 0.01,
                    content: action.data.content || `${action.data.start.toFixed(2)}s`,
                    color: action.data.color || 'rgba(255, 0, 0, 0.6)',
                    drag: true,
                    resize: false
                })
                setupRegionEvents(region, waveformInstance)
                layer.msg('已撤回删除操作', { icon: 1 })
            }
        }
    } catch (error) {
        // 用户取消了操作
        console.log('用户取消了撤回操作')
    }
}

function saveBeats() {
    if (!wfRegionInstance) {
        layer.msg('没有节拍数据可保存', { icon: 2 })
        return
    }
    
    const regions = wfRegionInstance.getRegions()
    if (regions.length === 0) {
        layer.msg('请至少添加一个节拍点', { icon: 2 })
        return
    }
    
    const beatTimes = regions
        .map(r => parseFloat(r.start.toFixed(2)))
        .sort((a, b) => a - b)
    
    if (isCreateMode) {
        const newData = {
            musicName: musicName,
            beatTimes: JSON.stringify(beatTimes),
            detectionMode: 'manual'
        }
        
        addBeatdata(newData).then(response => {
            layer.msg('创建成功', { icon: 1 })
            console.log('节拍数据已创建:', beatTimes)
            isCreateMode = false
            currentBeatData = response.data
        }).catch(error => {
            layer.msg('创建失败: ' + error, { icon: 2 })
        })
    } else {
        if (!currentBeatData) {
            layer.msg('没有节拍数据可更新', { icon: 2 })
            return
        }
        
        const updateData = {
            ...currentBeatData,
            beatTimes: JSON.stringify(beatTimes)
        }
        
        updateBeatdata(updateData).then(response => {
            layer.msg('保存成功', { icon: 1 })
            console.log('节拍数据已更新:', beatTimes)
        }).catch(error => {
            layer.msg('保存失败: ' + error, { icon: 2 })
        })
    }
}

const route = useRoute();
let musicId = route.params.musicId;
let musicName = route.params.musicName;
let isCreateMode = route.query.mode === 'create';
let detectionMode = route.query.detectionMode || 'manual';
let bitCount
let speedArray: number[][] = []
let waveformInstance = null
let wfRegionInstance = null
let currentBeatData = null
let selectedRegion = null
let keyPressInterval = null
let keyPressSpeed = 100
let playbackSpeed = ref(1.0)
let dragPreviewRegion = null // 拖动预览标签
let originalRegion = null // 原始标签
let isDraggingRegion = false // 是否正在拖动

console.log('Analysis页面初始化', { musicId, musicName, isCreateMode, detectionMode })

let myOptions = reactive({
    color: "1",
    fftSize: "1024",
    scrollStyle: "auto",
    beatLine: "default",
    muteBeat: [],
    startSection: 0,
    endSection: 100,
    beatsPerBeat: 4
})

// function getStartTimeInRegions(wfRegion: any): number[] {
//     const regions = wfRegion.getRegions();
//     let startTime = [];
//     for (let i = 0; i < regions.length; i++) {
//         startTime.push(regions[i].start)
//     }
//     return startTime;
// }

function caculateSpeed(startTime: number, endTime: number): number {
    const speed = 60.0 / (endTime - startTime)
    return speed
}

// 播放速度控制函数
function onSpeedChange(e: Event) {
    const speed = parseFloat((e.target as HTMLInputElement).value)
    setSpeed(speed)
}

function setSpeed(speed: number) {
    playbackSpeed.value = speed
    if (waveformInstance) {
        waveformInstance.setPlaybackRate(speed, true)
        layer.msg(`播放速度已设置为 ${speed}x`, { icon: 1, time: 1000 })
    }
}

// 判断当前速度是否与按钮速度匹配
function isSpeedActive(speed: number): boolean {
    // 使用小的误差范围来比较浮点数
    return Math.abs(playbackSpeed.value - speed) < 0.01
}

onMounted(async () => {
    console.log('onMounted 开始执行')
    
    const container = document.getElementById('waveform')
    console.log('waveform容器:', container)
    
    if (!container) {
        console.error('找不到 waveform 容器')
        return
    }
    
    const audioUrl = `http://localhost:8080/files/${musicId}`
    console.log('音频URL:', audioUrl)
    
    let waveform
    try {
        waveform = WaveForm.create({
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
        })
        console.log('WaveForm创建成功:', waveform)
    } catch (error) {
        console.error('WaveForm创建失败:', error)
        return
    }
    const wfRegion = waveform.registerPlugin(RegionsPlugin.create())
    waveformInstance = waveform
    wfRegionInstance = wfRegion

    // 监听加载事件
    waveform.on('load', (url) => {
        console.log('开始加载音频:', url)
    })
    
    waveform.on('loading', (percent) => {
        console.log('加载进度:', percent)
    })
    
    waveform.on('error', (error) => {
        console.error('音频加载错误:', error)
    })
    
    waveform.on('dblclick', (relativeX) => {
        const duration = waveform.getDuration()
        const clickTime = relativeX * duration
        
        const region = wfRegion.addRegion({
            start: clickTime,
            end: clickTime + 0.01,
            content: `${clickTime.toFixed(2)}s`,
            color: 'rgba(255, 0, 0, 0.6)',
            drag: true,
            resize: false
        })
        
        setupRegionEvents(region, waveform)
        
        // 记录添加操作到历史
        addToHistory({
            type: 'add',
            regionId: region.id,
            data: {
                start: clickTime,
                end: clickTime + 0.01,
                content: `${clickTime.toFixed(2)}s`,
                color: 'rgba(255, 0, 0, 0.6)'
            }
        })
        
        layer.msg('已添加节拍点，请点击"保存节拍修改"保存', { icon: 1 })
    })

    waveform.once('decode', async () => {
        console.log('音频解码完成')
        
        if (!isCreateMode) {
            console.log('开始加载节拍数据')
            console.log('音乐名称:', musicName)
            console.log('识别方式:', detectionMode)
            try {
                const response = await getBeatdataByMusicName(musicName, detectionMode);
                console.log('节拍数据API响应:', response);
                
                if (response.rows && response.rows.length > 0) {
                    currentBeatData = response.rows[0]
                    const beatTimes = JSON.parse(currentBeatData.beatTimes);
                    console.log('加载节拍数据:', beatTimes);
                    console.log('节拍数量:', beatTimes.length);
                    
                    beatTimes.forEach((time, index) => {
                        const region = wfRegion.addRegion({
                            start: time,
                            end: time + 0.01,
                            content: `${time.toFixed(2)}s`,
                            color: 'rgba(255, 0, 0, 0.6)',
                            drag: true,
                            resize: false
                        });
                        
                        setupRegionEvents(region, waveform)
                    });
                    
                    for (let i = 1; i < beatTimes.length; i++) {
                        const interval = beatTimes[i] - beatTimes[i - 1];
                        const bpm = 60 / interval;
                        speedArray.push([i, bpm]);
                    }
                    console.log('速度曲线数据:', speedArray);
                    
                    if (speedArray.length > 0 && RefResult.value) {
                        setTimeout(() => {
                            RefResult.value.redraw();
                        }, 100);
                    }
                } else {
                    console.warn('未找到节拍数据，响应:', response);
                }
            } catch (error) {
                console.error('加载节拍数据失败:', error);
            }
        } else {
            console.log('创建模式，不加载现有节拍数据')
            layer.msg('请通过双击波形或按w/a/s/d键添加节拍点', { icon: 1, time: 3000 })
        }

        const playPause = document.getElementById('playPause')
        playPause?.addEventListener('click', () => {
            waveform.playPause()
        })
        const slider = document.querySelector('input[type="range"]')
        slider?.addEventListener('input', (e) => {
            const zoomLevel = Number((e.target as HTMLInputElement).value)
            waveform.zoom(zoomLevel)
        })
        const getRegions = document.getElementById('getRegions')
        getRegions?.addEventListener('click', () => {
            console.log(wfRegion)
        })
        const clearRegions = document.getElementById('clearRegions')
        clearRegions?.addEventListener('click', () => {
            wfRegion.clearRegions()
            lastBitPosition = 0
            bitCount = 0
        })
    })

    // 清理预览状态的辅助函数
    function cleanupPreviewState() {
        if (dragPreviewRegion) {
            dragPreviewRegion.remove()
            dragPreviewRegion = null
        }
        if (originalRegion) {
            originalRegion.setOptions({
                color: 'rgba(255, 0, 0, 0.6)',
                drag: true
            })
            originalRegion = null
        }
        isDraggingRegion = false
    }
    
    function setupPreviewRegionEvents(previewRegion, originalRegion, waveform, originalStart) {
        previewRegion.on('update-end', () => {
            // 更新预览标签的内容显示
            previewRegion.setOptions({
                content: `${previewRegion.start.toFixed(2)}s (点击确认)`
            })
        })
        
        previewRegion.on('click', async (e) => {
            if (e.button !== 0) return // 只响应左键点击
            
            try {
                await ElMessageBox.confirm(
                    `确认将节拍点从 ${originalStart.toFixed(2)}s 移动到 ${previewRegion.start.toFixed(2)}s 吗？`,
                    '确认移动',
                    {
                        confirmButtonText: '确认',
                        cancelButtonText: '取消',
                        type: 'info',
                    }
                )
                
                // 用户确认，应用更改
                originalRegion.setOptions({
                    start: previewRegion.start,
                    end: previewRegion.start + 0.01,
                    content: `${previewRegion.start.toFixed(2)}s`,
                    color: 'rgba(255, 0, 0, 0.6)',
                    drag: true // 恢复可拖动
                })
                
                // 记录移动操作到历史
                if (Math.abs(previewRegion.start - originalStart) > 0.001) {
                    addToHistory({
                        type: 'move',
                        regionId: originalRegion.id,
                        data: {
                            oldStart: originalStart,
                            oldEnd: originalStart + 0.01,
                            start: previewRegion.start,
                            end: previewRegion.start + 0.01
                        }
                    })
                }
                
                // 删除预览标签
                previewRegion.remove()
                dragPreviewRegion = null
                originalRegion = null
                isDraggingRegion = false
                
                layer.msg('节拍点位置已更新', { icon: 1 })
            } catch (error) {
                // 用户取消，恢复原状态
                cleanupPreviewState()
                layer.msg('已取消移动', { icon: 2 })
            }
        })
        
        // 为预览标签添加右键菜单
        previewRegion.element.addEventListener('contextmenu', (e) => {
            e.preventDefault()
            
            const menu = document.createElement('div')
            menu.className = 'context-menu'
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
            `
            
            const cancelOption = document.createElement('div')
            cancelOption.textContent = '取消移动'
            cancelOption.style.cssText = `
                padding: 8px 16px;
                cursor: pointer;
                color: #f56c6c;
            `
            cancelOption.onmouseover = () => cancelOption.style.background = '#f5f5f5'
            cancelOption.onmouseout = () => cancelOption.style.background = 'white'
            cancelOption.onclick = () => {
                document.body.removeChild(menu)
                cleanupPreviewState()
                layer.msg('已取消移动', { icon: 2 })
            }
            
            menu.appendChild(cancelOption)
            document.body.appendChild(menu)
            
            const closeMenu = (event) => {
                if (menu.parentNode && !menu.contains(event.target)) {
                    document.body.removeChild(menu)
                    document.removeEventListener('click', closeMenu)
                }
            }
            setTimeout(() => {
                document.addEventListener('click', closeMenu)
            }, 0)
        })
    }
    
    function setupRegionEvents(region, waveform) {
        let regionStartBeforeDrag = region.start // 保存拖动前的位置
        
        region.on('update-start', () => {
            regionStartBeforeDrag = region.start // 记录拖动开始时的位置
        })
        
        region.on('update-end', () => {
            const newPosition = region.start
            
            // 检查是否真的移动了位置
            if (Math.abs(newPosition - regionStartBeforeDrag) < 0.001) {
                region.setOptions({
                    content: `${region.start.toFixed(2)}s`
                })
                return
            }
            
            // 如果有之前的预览状态，先清理
            if (isDraggingRegion) {
                cleanupPreviewState()
            }
            
            // 位置发生了变化，进入确认模式
            isDraggingRegion = true
            
            // 将region恢复到原位置并变为灰色
            region.setOptions({
                start: regionStartBeforeDrag,
                end: regionStartBeforeDrag + 0.01,
                content: `${regionStartBeforeDrag.toFixed(2)}s`,
                color: 'rgba(128, 128, 128, 0.3)',
                drag: false // 禁止继续拖动原标签
            })
            
            // 创建预览标签（绿色）在新位置
            dragPreviewRegion = wfRegionInstance.addRegion({
                start: newPosition,
                end: newPosition + 0.01,
                content: `${newPosition.toFixed(2)}s (点击确认)`,
                color: 'rgba(0, 200, 0, 0.6)',
                drag: false, // 预览标签不可拖动
                resize: false
            })
            
            // 保存原始region引用
            originalRegion = region
            
            // 为预览标签设置点击事件
            setupPreviewRegionEvents(dragPreviewRegion, region, waveform, regionStartBeforeDrag)
            
            layer.msg('请点击绿色预览标签确认移动', { icon: 1, time: 2000 })
        })
        
        region.on('click', (e) => {
            if (e.button === 2) {
                e.preventDefault()
            }
            selectedRegion = region
        })
        
        region.element.addEventListener('contextmenu', (e) => {
            e.preventDefault()
            
            // 如果该region正在预览模式（变灰色），提示用户先处理预览
            if (region === originalRegion && isDraggingRegion) {
                layer.msg('请先确认或取消当前的移动操作', { icon: 2 })
                return
            }
            
            selectedRegion = region
            
            const menu = document.createElement('div')
            menu.className = 'context-menu'
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
            `
            
            const moveOption = document.createElement('div')
            moveOption.textContent = '键盘移动 (←→)'
            moveOption.style.cssText = `
                padding: 8px 16px;
                cursor: pointer;
                border-bottom: 1px solid #eee;
            `
            moveOption.onmouseover = () => moveOption.style.background = '#f5f5f5'
            moveOption.onmouseout = () => moveOption.style.background = 'white'
            moveOption.onclick = () => {
                document.body.removeChild(menu)
                layer.msg('请使用键盘左右键移动节拍点，每次移动0.01s', { icon: 1 })
            }
            
            const deleteOption = document.createElement('div')
            deleteOption.textContent = '删除'
            deleteOption.style.cssText = `
                padding: 8px 16px;
                cursor: pointer;
                color: #f56c6c;
            `
            deleteOption.onmouseover = () => deleteOption.style.background = '#f5f5f5'
            deleteOption.onmouseout = () => deleteOption.style.background = 'white'
            deleteOption.onclick = () => {
                document.body.removeChild(menu)
                if (confirm(`删除节拍点 ${region.start.toFixed(2)}s?`)) {
                    // 记录删除操作到历史
                    addToHistory({
                        type: 'delete',
                        regionId: region.id,
                        data: {
                            start: region.start,
                            end: region.end,
                            content: `${region.start.toFixed(2)}s`,
                            color: region.color
                        }
                    })
                    
                    region.remove()
                    selectedRegion = null
                    layer.msg('节拍点已删除，请点击"保存节拍修改"保存', { icon: 1 })
                }
            }
            
            menu.appendChild(moveOption)
            menu.appendChild(deleteOption)
            document.body.appendChild(menu)
            
            const closeMenu = (event) => {
                if (menu.parentNode && !menu.contains(event.target)) {
                    document.body.removeChild(menu)
                    document.removeEventListener('click', closeMenu)
                }
            }
            setTimeout(() => {
                document.addEventListener('click', closeMenu)
            }, 0)
        })
    }

    let bitCount = 0
    let lastBitPosition = 0
    let keyboardMoveStartPosition = null // 保存键盘移动开始时的位置
    
    const keydownHandler = (e: KeyboardEvent) => {
        // Ctrl+Z 撤回功能
        if (e.ctrlKey && e.key === 'z') {
            e.preventDefault()
            undoLastAction()
            return
        }
        
        if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') {
            if (!selectedRegion) {
                layer.msg('请先右键选择一个节拍点', { icon: 2 })
                return
            }
            
            e.preventDefault()
            
            // 只在第一次按下时记录初始位置
            if (keyboardMoveStartPosition === null) {
                keyboardMoveStartPosition = selectedRegion.start
            }
            
            const moveStep = 0.01
            const direction = e.key === 'ArrowLeft' ? -1 : 1
            const newStart = Math.max(0, selectedRegion.start + direction * moveStep)
            const duration = waveform.getDuration()
            
            if (newStart <= duration) {
                selectedRegion.setOptions({
                    start: newStart,
                    end: newStart + 0.01,
                    content: `${newStart.toFixed(2)}s`
                })
                
                if (!keyPressInterval) {
                    keyPressSpeed = 100
                    keyPressInterval = setInterval(() => {
                        if (keyPressSpeed > 20) {
                            keyPressSpeed -= 10
                        }
                    }, 200)
                }
            }
        } else if (e.key === 'w' || e.key === 'a' || e.key === 's' || e.key === 'd') {
            const currentTime = waveform.getCurrentTime();
            
            const region = wfRegion.addRegion({
                start: currentTime,
                end: currentTime + 0.01,
                content: `${currentTime.toFixed(2)}s`,
                color: 'rgba(255, 0, 0, 0.6)',
                drag: true,
                resize: false
            })
            
            setupRegionEvents(region, waveform)
            
            // 记录添加操作到历史
            addToHistory({
                type: 'add',
                regionId: region.id,
                data: {
                    start: currentTime,
                    end: currentTime + 0.01,
                    content: `${currentTime.toFixed(2)}s`,
                    color: 'rgba(255, 0, 0, 0.6)'
                }
            })
            
            layer.msg('已添加节拍点，请点击"保存节拍修改"保存', { icon: 1, time: 1000 })
        }
    }
    
    const keyupHandler = (e: KeyboardEvent) => {
        if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') {
            if (keyPressInterval) {
                clearInterval(keyPressInterval)
                keyPressInterval = null
                keyPressSpeed = 100
            }
            
            // 键盘移动结束时，记录一次移动操作到历史
            if (keyboardMoveStartPosition !== null && selectedRegion) {
                const finalPosition = selectedRegion.start
                if (Math.abs(finalPosition - keyboardMoveStartPosition) > 0.001) {
                    addToHistory({
                        type: 'move',
                        regionId: selectedRegion.id,
                        data: {
                            oldStart: keyboardMoveStartPosition,
                            oldEnd: keyboardMoveStartPosition + 0.01,
                            start: finalPosition,
                            end: finalPosition + 0.01
                        }
                    })
                }
                keyboardMoveStartPosition = null
            }
        }
    }
    
    document.addEventListener('keydown', keydownHandler)
    document.addEventListener('keyup', keyupHandler)

    onUnmounted(() => {
        document.removeEventListener('keydown', keydownHandler)
        document.removeEventListener('keyup', keyupHandler)
        if (keyPressInterval) {
            clearInterval(keyPressInterval)
        }
    })
})
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
    height: 35vh;
    min-height: 250px;
    display: flex;
    flex-direction: column;
}

#waveform {
    width: 100%;
    height: 100%;
    flex: 1;
    border-radius: 4px;
}

/* Region 标记样式 */
:deep(.wavesurfer-region) {
    z-index: 3 !important;
}

:deep(.wavesurfer-region-content) {
    position: absolute !important;
    top: 50% !important;
    left: 50% !important;
    transform: translate(-50%, -50%) !important;
    background: rgba(255, 255, 255, 0.95) !important;
    color: #ff0000 !important;
    padding: 3px 7px !important;
    border-radius: 4px !important;
    font-size: 11px !important;
    font-weight: 600 !important;
    white-space: nowrap !important;
    pointer-events: none !important;
    z-index: 9999 !important;
    box-shadow: 0 2px 6px rgba(0,0,0,0.4) !important;
    border: 1px solid rgba(255,0,0,0.3) !important;
}

:deep(.wavesurfer-marker) {
    background: rgba(255, 0, 0, 0.5);
    width: 2px;
}

.content-wrapper {
    display: flex;
    gap: 20px;
    flex: 1;
    min-height: 0;
}

.chart-section {
    flex: 1;
    background: white;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    min-width: 0;
    display: flex;
    flex-direction: column;
}

.control-panel {

    width: 30vw;
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

.speed-control {
    margin-bottom: 20px;
    padding: 15px;
    background: #f8f9fa;
    border-radius: 8px;
    border: 1px solid #e4e7ed;
}

.speed-control label {
    display: block;
    margin-bottom: 10px;
    font-size: 14px;
    color: #606266;
    font-weight: 500;
}

.speed-value {
    color: #1890ff;
    font-weight: 700;
    font-size: 16px;
    margin-left: 8px;
}

.speed-control input[type="range"] {
    width: 100%;
    height: 6px;
    border-radius: 3px;
    background: linear-gradient(to right, #409EFF 0%, #67C23A 100%);
    outline: none;
    margin-bottom: 12px;
}

.speed-control input[type="range"]::-webkit-slider-thumb {
    -webkit-appearance: none;
    appearance: none;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: #409EFF;
    cursor: pointer;
    box-shadow: 0 2px 6px rgba(64, 158, 255, 0.5);
    transition: all 0.3s ease;
}

.speed-control input[type="range"]::-webkit-slider-thumb:hover {
    transform: scale(1.2);
    box-shadow: 0 3px 8px rgba(64, 158, 255, 0.7);
}

.speed-control input[type="range"]::-moz-range-thumb {
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: #409EFF;
    cursor: pointer;
    border: none;
    box-shadow: 0 2px 6px rgba(64, 158, 255, 0.5);
    transition: all 0.3s ease;
}

.speed-control input[type="range"]::-moz-range-thumb:hover {
    transform: scale(1.2);
    box-shadow: 0 3px 8px rgba(64, 158, 255, 0.7);
}

.speed-presets {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.speed-presets .el-button {
    flex: 1;
    min-width: 50px;
}




</style>
