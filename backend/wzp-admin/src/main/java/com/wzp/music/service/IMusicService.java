package com.wzp.music.service;

import java.util.List;
import com.wzp.music.domain.Music;

/**
 * 音乐管理Service接口
 * 
 * @author wzp
 * @date 2025-10-03
 */
public interface IMusicService 
{
    /**
     * 查询音乐管理
     * 
     * @param id 音乐管理主键
     * @return 音乐管理
     */
    public Music selectMusicById(Long id);

    /**
     * 查询音乐管理列表
     * 
     * @param music 音乐管理
     * @return 音乐管理集合
     */
    public List<Music> selectMusicList(Music music);

    /**
     * 新增音乐管理
     * 
     * @param music 音乐管理
     * @return 结果
     */
    public int insertMusic(Music music);

    /**
     * 修改音乐管理
     * 
     * @param music 音乐管理
     * @return 结果
     */
    public int updateMusic(Music music);

    /**
     * 批量删除音乐管理
     * 
     * @param ids 需要删除的音乐管理主键集合
     * @return 结果
     */
    public int deleteMusicByIds(Long[] ids);

    /**
     * 删除音乐管理信息
     * 
     * @param id 音乐管理主键
     * @return 结果
     */
    public int deleteMusicById(Long id);
}
