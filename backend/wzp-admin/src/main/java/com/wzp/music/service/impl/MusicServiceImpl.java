package com.wzp.music.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.wzp.music.mapper.MusicMapper;
import com.wzp.music.domain.Music;
import com.wzp.music.service.IMusicService;

/**
 * 音乐管理Service业务层处理
 * 
 * @author wzp
 * @date 2025-10-03
 */
@Service
public class MusicServiceImpl implements IMusicService 
{
    @Autowired
    private MusicMapper musicMapper;

    /**
     * 查询音乐管理
     * 
     * @param id 音乐管理主键
     * @return 音乐管理
     */
    @Override
    public Music selectMusicById(Long id)
    {
        return musicMapper.selectMusicById(id);
    }

    /**
     * 查询音乐管理列表
     * 
     * @param music 音乐管理
     * @return 音乐管理
     */
    @Override
    public List<Music> selectMusicList(Music music)
    {
        return musicMapper.selectMusicList(music);
    }

    /**
     * 新增音乐管理
     * 
     * @param music 音乐管理
     * @return 结果
     */
    @Override
    public int insertMusic(Music music)
    {
        return musicMapper.insertMusic(music);
    }

    /**
     * 修改音乐管理
     * 
     * @param music 音乐管理
     * @return 结果
     */
    @Override
    public int updateMusic(Music music)
    {
        return musicMapper.updateMusic(music);
    }

    /**
     * 批量删除音乐管理
     * 
     * @param ids 需要删除的音乐管理主键
     * @return 结果
     */
    @Override
    public int deleteMusicByIds(Long[] ids)
    {
        return musicMapper.deleteMusicByIds(ids);
    }

    /**
     * 删除音乐管理信息
     * 
     * @param id 音乐管理主键
     * @return 结果
     */
    @Override
    public int deleteMusicById(Long id)
    {
        return musicMapper.deleteMusicById(id);
    }
}
