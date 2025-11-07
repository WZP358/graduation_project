import request from '@/utils/request'

// 保存练习记录
export function saveExerciseRecord(data) {
  return request({
    url: '/music_anaysis/exercise/record',
    method: 'post',
    data: data
  })
}

// 获取排行榜
export function getLeaderboard(params) {
  return request({
    url: '/music_anaysis/exercise/leaderboard',
    method: 'get',
    params: params
  })
}

// 获取用户最佳记录
export function getUserBestRecord(params) {
  return request({
    url: '/music_anaysis/exercise/best',
    method: 'get',
    params: params
  })
}

// 获取用户练习历史
export function getExerciseHistory(params) {
  return request({
    url: '/music_anaysis/exercise/history',
    method: 'get',
    params: params
  })
}

// 获取智能分析
export function getIntelligentAnalysis(id) {
  return request({
    url: '/music_anaysis/exercise/analyze/' + id,
    method: 'get'
  })
}

// 导出报告
export function exportReport(id) {
  return request({
    url: '/music_anaysis/exercise/export/' + id,
    method: 'get',
    responseType: 'blob'
  })
}

