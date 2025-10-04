<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="音乐名称" prop="musicName">
        <el-input
          v-model="queryParams.musicName"
          placeholder="请输入音乐名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户ID" prop="createdBy">
        <el-input
          v-model="queryParams.createdBy"
          placeholder="请输入创建用户ID"
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
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['music_anaysis:beatdata:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['music_anaysis:beatdata:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['music_anaysis:beatdata:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['music_anaysis:beatdata:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="beatdataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键ID" align="center" prop="id" />
      <el-table-column label="音乐名称" align="center" prop="musicName" />
      <el-table-column label="节拍时刻数组" align="center" prop="beatTimes" show-overflow-tooltip />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建用户ID" align="center" prop="createdBy" />
      <el-table-column label="存放路径" align="center" prop="filePath" show-overflow-tooltip />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['music_anaysis:beatdata:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['music_anaysis:beatdata:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改节拍时刻对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="beatdataRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="音乐名称" prop="musicName">
          <el-input v-model="form.musicName" placeholder="请输入音乐名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm" :loading="submitting">确 定</el-button>
          <el-button @click="cancel" :disabled="submitting">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Beatdata">
import { listBeatdata, getBeatdata, delBeatdata, addBeatdata, updateBeatdata } from "@/api/music_anaysis/beatdata"

const { proxy } = getCurrentInstance()

const beatdataList = ref([])
const open = ref(false)
const loading = ref(true)
const submitting = ref(false)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    musicName: null,
    createdBy: null,
  },
  rules: {
    musicName: [
      { required: true, message: "音乐名称不能为空", trigger: "blur" }
    ],
    beatTimes: [
      { required: true, message: "节拍时刻数组(JSON格式)不能为空", trigger: "blur" }
    ],
    createdBy: [
      { required: true, message: "创建用户ID不能为空", trigger: "blur" }
    ],
    filePath: [
      { required: true, message: "文件在服务器上的存放路径不能为空", trigger: "blur" }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询节拍时刻列表 */
function getList() {
  loading.value = true
  listBeatdata(queryParams.value).then(response => {
    beatdataList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    musicName: null,
    beatTimes: null,
    createTime: null,
    updateTime: null,
    createdBy: null,
    filePath: null
  }
  proxy.resetForm("beatdataRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加节拍时刻"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getBeatdata(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改节拍时刻"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["beatdataRef"].validate(valid => {
    if (valid) {
      submitting.value = true
      const loadingInstance = proxy.$loading({
        lock: true,
        text: '正在分析音频，请稍等...',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      
      const promise = form.value.id != null ? updateBeatdata(form.value) : addBeatdata(form.value)
      
      promise.then(response => {
        proxy.$modal.msgSuccess(form.value.id != null ? "修改成功" : "新增成功")
        open.value = false
        getList()
      }).finally(() => {
        submitting.value = false
        loadingInstance.close()
      })
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除节拍时刻编号为"' + _ids + '"的数据项？').then(function() {
    return delBeatdata(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('music_anaysis/beatdata/export', {
    ...queryParams.value
  }, `beatdata_${new Date().getTime()}.xlsx`)
}

getList()
</script>
