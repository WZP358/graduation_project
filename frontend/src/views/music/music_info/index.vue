<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入名称"
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
          v-hasPermi="['music:music_info:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['music:music_info:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['music:music_info:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['music:music_info:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="music_infoList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
<!--      <el-table-column label="音乐ID" align="center" prop="id" />-->
      <el-table-column label="名称" align="center" prop="name" />
      <el-table-column label="作者" align="center" prop="author" />
      <el-table-column label="内容介绍" align="center" prop="description" />
      <el-table-column label="上传用户ID" align="center" prop="uploadUserId" />
      <el-table-column label="上传时间" align="center" prop="uploadTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.uploadTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="是否私有" align="center" prop="isPrivate" />
<!--      <el-table-column label="存放路径" align="center" prop="filePath" />-->
      <el-table-column label="存放路径" align="center" prop="filePath" show-overflow-tooltip />

      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['music:music_info:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['music:music_info:remove']">删除</el-button>
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

    <!-- 添加或修改音乐管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="music_infoRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="form.author" placeholder="请输入作者" />
        </el-form-item>
        <el-form-item label="内容介绍" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="是否私有" prop="isPrivate">
          <el-radio-group v-model="form.isPrivate">
            <el-radio label=true>是</el-radio>
            <el-radio label=false>否</el-radio>
          </el-radio-group>
        </el-form-item>
        <!-- 隐藏这些字段，但仍保留在el-form中 -->
        <el-form-item label="上传用户ID" prop="uploadUserId" v-show="false">
          <el-input v-model="form.uploadUserId" />
        </el-form-item>
        <el-form-item label="上传时间" prop="uploadTime" v-show="false">
          <el-date-picker v-model="form.uploadTime" type="datetime" />
        </el-form-item>

        <el-form-item label="存放路径" prop="filePath">
          <el-upload
              class="audio-uploader"
              :auto-upload="false"
              :on-change="handleAudioUpload"
              :before-upload="beforeAudioUpload"
              accept="audio/*"
              :limit="1"
              ref="uploadRef"
          >
          <el-button size="small" type="primary">选择音频文件</el-button>
          <div v-if="form.fileName" class="audio-file-info">已选择: {{ form.fileName }}</div>
          </el-upload>
        </el-form-item>


      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Music_info">
import { listMusic_info, getMusic_info, delMusic_info, addMusic_info, updateMusic_info } from "@/api/music/music_info"
import { getToken } from "@/utils/auth"
import { getUserProfile } from "@/api/system/user" // 根据实际路径调整

const { proxy } = getCurrentInstance()

const music_infoList = ref([])
const open = ref(false)
const loading = ref(true)
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
    name: null,
  },
  currentUser: {},
  uploadFile: null,
  rules: {
    name: [
      { required: true, message: "名称不能为空", trigger: "blur" }
    ],
    isPrivate: [
      { required: true, message: "是否私有不能为空", trigger: "blur" }
    ],
    filePath: [
      { required: true, message: "请选择音频文件", trigger: "change" }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询音乐管理列表 */
function getList() {
  loading.value = true
  listMusic_info(queryParams.value).then(response => {
    music_infoList.value = response.rows
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
    name: null,
    author: null,
    description: null,
    uploadUserId: null,
    uploadTime: null,
    isPrivate: false,
    filePath: null,
    fileName: null
  }
  data.uploadFile = null
  proxy.resetForm("music_infoRef")
  
  if (proxy.$refs.uploadRef) {
    proxy.$refs.uploadRef.clearFiles()
  }
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
  // 自动设置上传用户ID和上传时间
  form.value.uploadUserId = data.currentUser.userId
  form.value.uploadTime = new Date()
  open.value = true
  title.value = "添加音乐管理"
}
/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getMusic_info(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改音乐管理"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["music_infoRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateMusic_info(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        if (!data.uploadFile) {
          proxy.$modal.msgError("请选择音频文件")
          return
        }
        
        const formData = new FormData()
        formData.append('file', data.uploadFile)
        formData.append('name', form.value.name)
        formData.append('author', form.value.author || '')
        formData.append('description', form.value.description || '')
        formData.append('isPrivate', form.value.isPrivate)
        
        addMusic_info(formData).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}


/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除音乐管理编号为"' + _ids + '"的数据项？').then(function() {
    return delMusic_info(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('music/music_info/export', {
    ...queryParams.value
  }, `music_info_${new Date().getTime()}.xlsx`)
}

const handleAudioUpload = (file) => {
  data.uploadFile = file.raw
  form.value.fileName = file.name
  form.value.filePath = file.name
}

const beforeAudioUpload = (file) => {
  const isAudio = file.type.startsWith('audio/')
  const isLt100M = file.size / 1024 / 1024 < 100

  if (!isAudio) {
    proxy.$modal.msgError('只能上传音频文件!')
    return false
  }
  if (!isLt100M) {
    proxy.$modal.msgError('文件大小不能超过 100MB!')
    return false
  }

  return true
}

// 在getList之前获取当前用户信息
getUserProfile().then(response => {
  data.currentUser = response.data
}).finally(() => {
  getList()
})

getList()
</script>
