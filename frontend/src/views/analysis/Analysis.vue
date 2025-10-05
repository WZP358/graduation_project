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
                    <el-button type="success" @click="callRedraw()">绘制速度曲线</el-button>
                    <el-button type="warning" @click="saveBeats()">保存节拍修改</el-button>
                </div>

                <div class="zoom-control">
                    <label>缩放比例：</label>
                    <input type="range" min="10" max="400" value="120"></input>
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
import WaveForm from '@/views/analysis/waveform/waveform';
import SpectrogramPlugin from '@/views/analysis/waveform/plugins/spectrogram';
import Minimap from '@/views/analysis/waveform/plugins/minimap';
import HoverPlugin from '@/views/analysis/waveform/plugins/hover';
import RegionsPlugin from '@/views/analysis/waveform/plugins/regions';
import Options from './components/Options.vue';
import Result from './components/Result.vue';
import { getBeatdataByMusicName, updateBeatdata } from '@/api/music_anaysis/beatdata';

const RefResult = ref(null)
function callRedraw() {
    RefResult.value.redraw()
}

function saveBeats() {
    if (!wfRegionInstance || !currentBeatData) {
        layer.msg('没有节拍数据可保存', { icon: 2 })
        return
    }
    
    const regions = wfRegionInstance.getRegions()
    const beatTimes = regions
        .map(r => r.start)
        .sort((a, b) => a - b)
    
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

const route = useRoute();
let musicId = route.params.musicId;
let musicName = route.params.musicName;
let bitCount
let speedArray: number[][] = []
let waveformInstance = null
let wfRegionInstance = null
let currentBeatData = null

console.log('Analysis页面初始化', { musicId, musicName })

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
        
        region.on('click', (e) => {
            if (e.button === 2) {
                e.preventDefault()
            }
        })
        
        region.element.addEventListener('contextmenu', (e) => {
            e.preventDefault()
            if (confirm(`删除节拍点 ${clickTime.toFixed(2)}s?`)) {
                region.remove()
                layer.msg('节拍点已删除，请点击"保存节拍修改"保存', { icon: 1 })
            }
        })
        
        layer.msg('已添加节拍点，请点击"保存节拍修改"保存', { icon: 1 })
    })

    waveform.once('decode', async () => {
        console.log('音频解码完成，开始加载节拍数据')
        console.log('音乐名称:', musicName)
        try {
            const response = await getBeatdataByMusicName(musicName);
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
                    
                    region.on('click', (e) => {
                        if (e.button === 2) {
                            e.preventDefault()
                        }
                    })
                    
                    region.element.addEventListener('contextmenu', (e) => {
                        e.preventDefault()
                        if (confirm(`删除节拍点 ${time.toFixed(2)}s?`)) {
                            region.remove()
                            layer.msg('节拍点已删除，请点击"保存节拍修改"保存', { icon: 1 })
                        }
                    })
                });
                
                // 自动计算速度曲线数据
                for (let i = 1; i < beatTimes.length; i++) {
                    const interval = beatTimes[i] - beatTimes[i - 1];
                    const bpm = 60 / interval;
                    speedArray.push([i, bpm]);
                }
                console.log('速度曲线数据:', speedArray);
                
                // 自动绘制速度曲线
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

    let bitCount = 0
    let lastBitPosition = 0
    const keydownHandler = (e: KeyboardEvent) => {
        if (e.key === 'a' || e.key === 's' || e.key === 'd' || e.key === 'f') {
            const currentTime = waveform.getCurrentTime();
            const speed = caculateSpeed(lastBitPosition, currentTime)
            // console.log(speedArray)
            lastBitPosition = currentTime
            if (bitCount < myOptions.beatsPerBeat) {
                bitCount = myOptions.beatsPerBeat
            }
            if (bitCount != myOptions.beatsPerBeat) {
                speedArray.push([bitCount - myOptions.beatsPerBeat, speed])
            }
            // console.log('bitCount', bitCount)
            const bit = Math.floor(bitCount / myOptions.beatsPerBeat)
            const beat = Math.floor(bitCount % myOptions.beatsPerBeat) + 1
            // console.log(bit, beat)
            wfRegion.addRegion({
                start: currentTime,
                content: bit.toString() + '-' + beat.toString() + '\n' + waveform.getCurrentTime().toPrecision(3) + '\n' + speed.toPrecision(3),
                color: 'rgba(240, 212, 0, 0.85)',
                drag: false,
            });
            bitCount++;
        }
    }
    document.addEventListener('keydown', keydownHandler)



    // Cleanup the event listener when the component is unmounted
    onUnmounted(() => {
        document.removeEventListener('keydown', keydownHandler)
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




</style>
