import request from '@/utils/request'

// 查询节拍时刻列表
export function listBeatdata(query) {
  return request({
    url: '/music_anaysis/beatdata/list',
    method: 'get',
    params: query
  })
}

// 查询节拍时刻详细
export function getBeatdata(id) {
  return request({
    url: '/music_anaysis/beatdata/' + id,
    method: 'get'
  })
}

// 新增节拍时刻
export function addBeatdata(data) {
  return request({
    url: '/music_anaysis/beatdata',
    method: 'post',
    data: data
  })
}

// 修改节拍时刻
export function updateBeatdata(data) {
  return request({
    url: '/music_anaysis/beatdata',
    method: 'put',
    data: data
  })
}

// 删除节拍时刻
export function delBeatdata(id) {
  return request({
    url: '/music_anaysis/beatdata/' + id,
    method: 'delete'
  })
}

// 根据音乐名称查询节拍时刻
export function getBeatdataByMusicName(musicName) {
  return request({
    url: '/music_anaysis/beatdata/list',
    method: 'get',
    params: { musicName: musicName, pageNum: 1, pageSize: 1 }
  })
}

// 批量查询节拍时刻详细（通过ID列表）
export function getBeatdataByIds(ids) {
  return request({
    url: '/music_anaysis/beatdata/listByIds',
    method: 'get',
    params: { ids: ids }
  })
}
