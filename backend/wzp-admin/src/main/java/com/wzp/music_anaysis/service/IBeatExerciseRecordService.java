package com.wzp.music_anaysis.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.wzp.music_anaysis.domain.BeatExerciseRecord;

/**
 * 节拍练习记录Service接口
 * 
 * @author wzp
 * @date 2025-11-07
 */
public interface IBeatExerciseRecordService 
{
    /**
     * 查询节拍练习记录
     * 
     * @param id 节拍练习记录主键
     * @return 节拍练习记录
     */
    public BeatExerciseRecord selectBeatExerciseRecordById(Long id);

    /**
     * 查询节拍练习记录列表
     * 
     * @param beatExerciseRecord 节拍练习记录
     * @return 节拍练习记录集合
     */
    public List<BeatExerciseRecord> selectBeatExerciseRecordList(BeatExerciseRecord beatExerciseRecord);

    /**
     * 新增节拍练习记录
     * 
     * @param beatExerciseRecord 节拍练习记录
     * @return 结果
     */
    public int insertBeatExerciseRecord(BeatExerciseRecord beatExerciseRecord);

    /**
     * 删除节拍练习记录
     * 
     * @param id 节拍练习记录主键
     * @return 结果
     */
    public int deleteBeatExerciseRecordById(Long id);

    /**
     * 批量删除节拍练习记录
     * 
     * @param ids 需要删除的节拍练习记录主键集合
     * @return 结果
     */
    public int deleteBeatExerciseRecordByIds(Long[] ids);

    /**
     * 获取排行榜
     * 
     * @param beatdataId 节拍数据ID
     * @param playbackSpeed 播放速度
     * @param practiceMode 练习模式
     * @param limit 返回数量
     * @return 排行榜
     */
    public List<BeatExerciseRecord> getLeaderboard(Long beatdataId, BigDecimal playbackSpeed, String practiceMode, int limit);

    /**
     * 获取用户最佳记录
     * 
     * @param userId 用户ID
     * @param beatdataId 节拍数据ID
     * @param playbackSpeed 播放速度
     * @param practiceMode 练习模式
     * @return 最佳记录
     */
    public BeatExerciseRecord getUserBestRecord(Long userId, Long beatdataId, BigDecimal playbackSpeed, String practiceMode);

    /**
     * 智能分析练习记录
     * 
     * @param id 记录ID
     * @return 分析结果
     */
    public Map<String, Object> analyzeRecord(Long id);
}

