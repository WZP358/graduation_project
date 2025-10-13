<template>
    <div class="result">
        <h4>速度&nbsp;&nbsp;-&nbsp;&nbsp;力度&nbsp;&nbsp;曲线</h4>
        <div class="chart-wrapper">
            <div id="lineChart" v-on:parentEvent="redraw" style="font-size: 14px; color: azure;"></div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { LineChart } from 'metrics-graphics'

const props = defineProps({
    speedArray: {
        type: Array as () => Array<[number, number]>,
        required: true
    }
})

function redraw() {
    const confidence = props.speedArray.map(([beat, speed]) => ({
        speed: speed,
        beat: beat,
    }))

    if (confidence.length === 0) {
        return
    }

    const speeds = confidence.map(entry => entry.speed)
    const minSpeed = Math.min(...speeds)
    const maxSpeed = Math.max(...speeds)
    const speedRange = maxSpeed - minSpeed
    const yPadding = speedRange * 0.1 || 10
    const yMin = Math.max(0, minSpeed - yPadding)
    const yMax = maxSpeed + yPadding

    const chartContainer = document.getElementById('lineChart')
    const containerWidth = chartContainer?.parentElement?.clientWidth || 800
    const chartWidth = Math.max(containerWidth - 40, 600)

    new LineChart({
        confidenceBand: ['l', 'u'],
        data: [
            confidence.map((entry) => ({
                ...entry,
                beat: entry.beat,
                speed: entry.speed,
                l: entry.speed - speedRange * 0.05,
                u: entry.speed + speedRange * 0.05,
            })),
        ],
        area: [],
        xAxis: {
            label: 'Beat',
        },
        yAxis: {
            label: 'Speed(BPM)',
            tickCount: 6,
            extendedTicks: true,
        },
        yScale: {
            minValue: yMin,
            maxValue: yMax,
        },
        xAccessor: 'beat',
        yAccessor: 'speed',
        width: chartWidth,
        height: 400,
        target: '#lineChart',
        tooltipFunction: (point) => `beat: ${point.beat} - speed: ${point.speed.toFixed(2)}`,
        margin: { top: 20, right: 20, bottom: 50, left: 60 },
    })
}

onMounted(() => {
})

defineExpose({ redraw })
</script>

<style>
.result {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-height: 400px;
    background-color: #fafafa;
    border-radius: 8px;
    padding: 20px;
    width: 100%;
    box-sizing: border-box;
}

h4 {
    margin-top: 0;
    margin-bottom: 20px;
    font-size: 16px;
    color: #303133;
    font-weight: 600;
}

.chart-wrapper {
    width: 100%;
    overflow-x: auto;
    overflow-y: hidden;
}

#lineChart {
    min-width: 600px;
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
</style>
