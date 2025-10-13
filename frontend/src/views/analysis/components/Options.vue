<template>
    <lay-form :model="options" :pane="pane" :size="size">
        <lay-form-item>
            <!-- <lay-button type="primary" @click="">提交</lay-button>
            <lay-button type="default" @click="">重置</lay-button> -->
        </lay-form-item>
    </lay-form>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watchEffect } from 'vue'
import { layer } from '@layui/layer-vue'
import { defineEmits } from 'vue';
// import { watchEffect }

const size = ref("xs");
const pane = ref(false);
const emit = defineEmits(['update:options']);

const options = reactive({
    color: "1",
    fftSize: "1024",
    scrollStyle: "auto",
    beatLine: "default",
    muteBeat: [],
    startSection: 0,
    endSection: 100,
    beatsPerBeat: 4
})

watchEffect(() => {
    // console.log(options);
    emit('update:options', options);
})

const handleChange = (newOptions) => {
    Object.assign(options, newOptions);
    emit('update:options', options);
}
</script>

<style>
/* .layui-form-pane .layui-form-label {
    background-color: rgb(196, 3, 3);
} */

/* .layui-select,
.layui-form-item .layui-slider-horizontal {
    width: 130px;
} */

/* .layui-form-item .layui-input-inline {
    width: 200px !important;
} */
</style>
