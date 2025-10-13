<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="68px">
      <el-form-item label="音乐名称" prop="musicName">
        <el-input
          v-model="queryParams.musicName"
          placeholder="请输入音乐名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="View"
          :disabled="selectedRows.length < 2"
          @click="handleCompare"
        >对比波形 (已选 {{ selectedRows.length }})</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          plain
          icon="Close"
          :disabled="selectedRows.length === 0"
          @click="handleClearSelection"
        >清空选择</el-button>
      </el-col>
    </el-row>

    <el-alert
      v-if="selectedRows.length > 0"
      :title="`已选择 ${selectedRows.length} 条数据：${selectedRows.map(r => r.musicName + ' (' + r.creatorName + ')').join('、')}`"
      type="info"
      :closable="false"
      style="margin-bottom: 15px;"
    />

    <el-table 
      v-loading="loading" 
      :data="beatdataList" 
      @selection-change="handleSelectionChange"
      :row-key="getRowKey"
      ref="tableRef"
    >
      <el-table-column 
        type="selection" 
        width="55" 
        align="center" 
        :reserve-selection="true"
        :selectable="checkSelectable"
      />
      <el-table-column label="音乐名称" align="center" prop="musicName" />
      <el-table-column label="节拍时刻数组" align="center" prop="beatTimes" show-overflow-tooltip />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建用户名称" align="center" prop="creatorName" />
      <el-table-column label="检测模式" align="center" prop="detectionMode" />
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup name="BeatdataCompare">
import { listBeatdata } from "@/api/music_anaysis/beatdata";
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';

const router = useRouter();
const tableRef = ref();
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const beatdataList = ref([]);
const selectedRows = ref([]);
const selectedMusicName = ref('');

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  musicName: undefined
});

function getRowKey(row) {
  return row.id;
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.musicName = undefined;
  handleQuery();
}

function getList() {
  loading.value = true;
  listBeatdata(queryParams).then(response => {
    beatdataList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

function checkSelectable(row) {
  if (selectedRows.value.length === 0) {
    return true;
  }
  return row.musicName === selectedMusicName.value;
}

function handleSelectionChange(selection) {
  if (selection.length === 0) {
    selectedRows.value = [];
    selectedMusicName.value = '';
    return;
  }
  
  const firstMusicName = selection[0].musicName;
  const validSelection = selection.filter(row => row.musicName === firstMusicName);
  
  if (validSelection.length !== selection.length) {
    ElMessage.warning('只能选择相同音乐名称的数据进行对比');
    tableRef.value.clearSelection();
    validSelection.forEach(row => {
      tableRef.value.toggleRowSelection(row, true);
    });
  }
  
  selectedRows.value = validSelection;
  selectedMusicName.value = validSelection.length > 0 ? firstMusicName : '';
}

function handleClearSelection() {
  tableRef.value.clearSelection();
  selectedRows.value = [];
  selectedMusicName.value = '';
}

function handleCompare() {
  if (selectedRows.value.length < 2) {
    ElMessage.warning('请至少选择2条数据进行对比');
    return;
  }
  
  const ids = selectedRows.value.map(row => row.id).join(',');
  const musicName = selectedMusicName.value;
  
  router.push({
    path: '/analysis/compare',
    query: {
      ids: ids,
      musicName: musicName,
      mode: 'compare'
    }
  });
}

getList();
</script>

<style scoped>
.app-container {
  padding: 20px;
}
</style>
