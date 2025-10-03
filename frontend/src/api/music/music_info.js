import request from '@/utils/request'

// 查询音乐管理列表
export function listMusic_info(query) {
  return request({
    url: '/music/music_info/list',
    method: 'get',
    params: query
  })
}

// 查询音乐管理详细
export function getMusic_info(id) {
  return request({
    url: '/music/music_info/' + id,
    method: 'get'
  })
}

// 新增音乐管理
export function addMusic_info(data) {
  return request({
    url: '/music/music_info',
    method: 'post',
    data: data
  })
}

// 修改音乐管理
export function updateMusic_info(data) {
  return request({
    url: '/music/music_info',
    method: 'put',
    data: data
  })
}

// 删除音乐管理
export function delMusic_info(id) {
  return request({
    url: '/music/music_info/' + id,
    method: 'delete'
  })
}
