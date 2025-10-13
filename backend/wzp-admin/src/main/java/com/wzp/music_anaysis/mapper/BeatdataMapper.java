package com.wzp.music_anaysis.mapper;

import java.util.List;
import com.wzp.music_anaysis.domain.Beatdata;

/**
 * 节拍时刻Mapper接口
 * 
 * @author wzp
 * @date 2025-10-04
 */
public interface BeatdataMapper 
{
    /**
     * 查询节拍时刻
     * 
     * @param id 节拍时刻主键
     * @return 节拍时刻
     */
    public Beatdata selectBeatdataById(Long id);

    /**
     * 查询节拍时刻列表
     * 
     * @param beatdata 节拍时刻
     * @return 节拍时刻集合
     */
    public List<Beatdata> selectBeatdataList(Beatdata beatdata);

    /**
     * 新增节拍时刻
     * 
     * @param beatdata 节拍时刻
     * @return 结果
     */
    public int insertBeatdata(Beatdata beatdata);

    /**
     * 修改节拍时刻
     * 
     * @param beatdata 节拍时刻
     * @return 结果
     */
    public int updateBeatdata(Beatdata beatdata);

    /**
     * 删除节拍时刻
     * 
     * @param id 节拍时刻主键
     * @return 结果
     */
    public int deleteBeatdataById(Long id);

    /**
     * 批量删除节拍时刻
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBeatdataByIds(Long[] ids);

    /**
     * 根据音乐名称删除节拍时刻
     * 
     * @param musicName 音乐名称
     * @return 结果
     */
    public int deleteBeatdataByMusicName(String musicName);

    /**
     * 检查音乐名称和用户是否已存在
     * 
     * @param beatdata 节拍时刻
     * @return 节拍时刻
     */
    public Beatdata checkMusicNameAndUserExists(Beatdata beatdata);
}
