<template>
    <div class="result">
        <div class="result-header">
            <h4>速度 - 力度 曲线图</h4>
        </div>
        <div class="chart-wrapper">
            <div class="line-chart-container">
                <div id="lineChart" style="font-size: 14px; color: azure;"></div>
                <svg v-if="lineChartDimensions && currentProgress !== null && speedArray.length > 0" 
                     class="progress-overlay"
                     :width="lineChartDimensions.width"
                     :height="lineChartDimensions.height"
                     :style="{
                         position: 'absolute',
                         left: lineChartDimensions.left + 'px',
                         top: lineChartDimensions.top + 'px',
                         pointerEvents: 'none'
                     }">
                    <!-- 红色粗线指示器 -->
                    <line :x1="progressLineX" 
                          :y1="progressLineY1" 
                          :x2="progressLineX" 
                          :y2="progressLineY2"
                          stroke="#ff4d4f" 
                          stroke-width="3"
                          opacity="0.8"/>
                </svg>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick, onUnmounted } from 'vue';
import { LineChart } from 'metrics-graphics'

const props = defineProps({
    speedArray: {
        type: Array as () => Array<[number, number]>,
        required: true
    },
    currentProgress: {
        type: Number,
        default: null
    },
    currentTime: {
        type: Number,
        default: 0
    },
    beatTimes: {
        type: Array as () => number[],
        default: () => []
    },
    audioDuration: {
        type: Number,
        default: 0
    },
    visibleTimeRange: {
        type: Object as () => { start: number, end: number },
        default: () => ({ start: 0, end: 0 })
    },
    displayWindow: {
        type: Number,
        default: 10  // 显示窗口大小（秒）
    }
})

const lineChartDimensions = ref<any>(null)

// 计算进度线的 X 坐标
const progressLineX = computed(() => {
    if (!lineChartDimensions.value || props.beatTimes.length === 0 || props.audioDuration === 0) {
        return 0
    }
    
    // 这个 margin 必须与 redraw() 函数中 LineChart 的 margin 一致
    const margin = { left: 60, right: 20 }
    const totalWidth = lineChartDimensions.value.width
    const chartWidth = totalWidth - margin.left - margin.right
    
    // 加上0.2秒补偿以同步波形图
    const currentTime = props.currentTime + 0.23
    
    // 使用波形图的可见时间范围
    const visibleStart = props.visibleTimeRange.start || 0
    const visibleEnd = props.visibleTimeRange.end || props.audioDuration
    
    // 计算进度百分比（相对于可见范围）
    let progress = 0
    if (currentTime <= visibleStart) {
        progress = 0
    } else if (currentTime >= visibleEnd) {
        progress = 1
    } else {
        progress = (currentTime - visibleStart) / (visibleEnd - visibleStart)
    }
    
    // 红线 X 位置 = margin.left + 绘图区域宽度 * 进度百分比
    const xPosition = margin.left + chartWidth * progress
    
    return xPosition
})

// 计算进度线的起始 Y 坐标（顶部）
const progressLineY1 = computed(() => {
    if (!lineChartDimensions.value) {
        return 0
    }
    
    const margin = { top: 20, bottom: 50 }
    return margin.top
})

// 计算进度线的结束 Y 坐标（底部）
const progressLineY2 = computed(() => {
    if (!lineChartDimensions.value) {
        return 0
    }
    
    const margin = { top: 20, bottom: 50 }
    const totalHeight = lineChartDimensions.value.height
    return totalHeight - margin.bottom
})

function redraw() {
    if (props.speedArray.length === 0 || props.beatTimes.length === 0) {
        return
    }

    // 将数据转换为时间-速度格式 - 显示全部数据
    const confidence = props.speedArray.map(([beat, speed], index) => ({
        time: props.beatTimes[index],  // 使用实际的节拍时间
        speed: speed,
        beat: beat,  // 保留beat编号用于tooltip
    }))

    const speeds = confidence.map(entry => entry.speed)
    const minSpeed = Math.min(...speeds)
    const maxSpeed = Math.max(...speeds)
    const speedRange = maxSpeed - minSpeed
    const yPadding = speedRange * 0.1 || 10
    const yMin = Math.max(0, minSpeed - yPadding)
    const yMax = maxSpeed + yPadding

    // 获取容器的实际尺寸
    const chartContainer = document.getElementById('lineChart')
    const parentContainer = chartContainer?.parentElement
    const containerWidth = parentContainer?.clientWidth || 800
    const containerHeight = parentContainer?.clientHeight || 500
    
    const chartWidth = Math.max(containerWidth - 40, 400)
    const chartHeight = Math.max(containerHeight - 80, 300)

    new LineChart({
        confidenceBand: ['l', 'u'],
        data: [
            confidence.map((entry) => ({
                ...entry,
                time: entry.time,
                speed: entry.speed,
                l: entry.speed - speedRange * 0.05,
                u: entry.speed + speedRange * 0.05,
            })),
        ],
        area: [],
        xAxis: {
            label: 'Time (s)',
            extendedTicks: true,
            tickCount: 10,
        },
        yAxis: {
            label: 'Speed(BPM)',
            tickCount: 10,
            extendedTicks: true,
        },
        xScale: {
            minValue: props.visibleTimeRange.start || 0,  // X轴显示波形图的可见范围
            maxValue: props.visibleTimeRange.end || props.audioDuration || 100,
        },
        yScale: {
            minValue: yMin,
            maxValue: yMax,
        },
        xAccessor: 'time',  // 使用时间作为X轴
        yAccessor: 'speed',
        width: chartWidth,
        height: chartHeight,
        target: '#lineChart',
        tooltipFunction: (point) => `Time: ${point.time.toFixed(2)}s - Beat: ${point.beat} - Speed: ${point.speed.toFixed(2)} BPM`,
        margin: { top: 20, right: 20, bottom: 50, left: 60 },
    })
    
    // 更新图表尺寸信息用于进度线定位
    nextTick(() => {
        const container = document.querySelector('.line-chart-container')
        if (container) {
            lineChartDimensions.value = {
                width: chartWidth,
                height: chartHeight,
                left: 0,
                top: 0
            }
        }
        
        // 手动添加网格线确保显示
        const svg = document.querySelector('#lineChart svg')
        if (svg) {
            // 移除旧的网格线
            const oldGrid = svg.querySelector('.custom-grid-lines')
            if (oldGrid) {
                oldGrid.remove()
            }
            
            // 创建网格线组
            const gridGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g')
            gridGroup.setAttribute('class', 'custom-grid-lines')
            
            const margin = { top: 20, right: 20, bottom: 50, left: 60 }
            const plotWidth = chartWidth - margin.left - margin.right
            const plotHeight = chartHeight - margin.top - margin.bottom
            
            // 只添加横向网格线（Y轴）
            const yGridCount = 10
            for (let i = 0; i <= yGridCount; i++) {
                const y = margin.top + (plotHeight / yGridCount) * i
                const line = document.createElementNS('http://www.w3.org/2000/svg', 'line')
                line.setAttribute('x1', margin.left.toString())
                line.setAttribute('y1', y.toString())
                line.setAttribute('x2', (margin.left + plotWidth).toString())
                line.setAttribute('y2', y.toString())
                line.setAttribute('stroke', '#cccccc')
                line.setAttribute('stroke-width', '1')
                line.setAttribute('stroke-dasharray', '3,3')
                gridGroup.appendChild(line)
            }
            
            // 将网格线插入到SVG的最前面（底层）
            svg.insertBefore(gridGroup, svg.firstChild)
        }
    })
}

// 监听进度变化，添加虚线网格到SVG
watch(() => props.speedArray, () => {
    nextTick(() => {
        const svg = document.querySelector('#lineChart svg')
        if (svg) {
            // 添加虚线网格
            let gridGroup = svg.querySelector('.custom-grid')
            if (!gridGroup) {
                gridGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g')
                gridGroup.setAttribute('class', 'custom-grid')
                svg.insertBefore(gridGroup, svg.firstChild)
            }
        }
    })
}, { immediate: true })

onMounted(() => {
    // 初始化时绘制图表
    if (props.speedArray.length > 0) {
        setTimeout(() => {
            redraw()
        }, 200)
    }
    
    // 监听窗口大小变化，重新绘制图表
    const resizeObserver = new ResizeObserver(() => {
        if (props.speedArray.length > 0) {
            setTimeout(() => {
                redraw()
            }, 100)
        }
    })
    
    const chartWrapper = document.querySelector('.chart-wrapper')
    if (chartWrapper) {
        resizeObserver.observe(chartWrapper)
    }
})

// 监听数据变化，自动重绘
watch(() => props.speedArray, (newArray) => {
    if (newArray.length > 0) {
        setTimeout(() => {
            redraw()
        }, 200)
    }
}, { deep: true })

// 监听可见时间范围变化，重绘图表
watch(() => props.visibleTimeRange, (newRange) => {
    if (props.speedArray.length > 0) {
        setTimeout(() => {
            redraw()
        }, 100)
    }
}, { deep: true })

// 不监听播放时间重绘，图表只在初始化时绘制一次
// 播放时只有红圈移动，实现最流畅的效果

defineExpose({ redraw })
</script>

<style>
.result {
    display: flex;
    flex-direction: column;
    height: 100%;
    background-color: #fafafa;
    border-radius: 8px;
    padding: 20px;
    width: 100%;
    box-sizing: border-box;
}

.result-header {
    display: flex;
    justify-content: center;
    align-items: center;
    margin-bottom: 20px;
    width: 100%;
}

h4 {
    margin: 0;
    font-size: 16px;
    color: #303133;
    font-weight: 600;
}

.chart-wrapper {
    flex: 1;
    width: 100%;
    overflow: auto;
    position: relative;
    display: flex;
    flex-direction: column;
}

.line-chart-container {
    position: relative;
    width: 100%;
    flex: 1;
    display: flex;
    flex-direction: column;
}

#lineChart {
    width: 100%;
    height: 100%;
}

.mg-area {
    z-index: -1;
    fill: rgba(24, 144, 255, 0.1);
    stroke-width: 0.5px;
    stroke: rgba(24, 144, 255, 0.3);
}

.mg-line {
    z-index: 10;
    fill: none;
    stroke: #1890ff;
    stroke-width: 2px;
}

.mg-content g text {
    fill: #606266;
}

.mg-content g g text,
.mg-content g text {
    font-size: 14px;
}

/* 添加虚线网格样式 - 多种选择器确保生效 */
:deep(svg .mg-extended-y-ticks line),
:deep(svg .mg-extended-x-ticks line),
:deep(#lineChart svg .mg-extended-y-ticks line),
:deep(#lineChart svg .mg-extended-x-ticks line),
:deep(.mg-extended-y-ticks line),
:deep(.mg-extended-x-ticks line) {
    stroke: #666666 !important;
    stroke-dasharray: 3, 3 !important;
    stroke-width: 2px !important;
    opacity: 1 !important;
}

/* 曲线图元素平滑过渡 */
:deep(#lineChart svg) {
    transition: none;
}

:deep(#lineChart .mg-line) {
    transition: none;
}

:deep(#lineChart .mg-line-container) {
    transition: none;
}

.progress-overlay {
    z-index: 10;
}

.progress-overlay line {
    filter: drop-shadow(0 0 4px rgba(255, 77, 79, 0.6));
    /* 不使用transition，让红线跟随computed值即时更新 */
}
</style>
