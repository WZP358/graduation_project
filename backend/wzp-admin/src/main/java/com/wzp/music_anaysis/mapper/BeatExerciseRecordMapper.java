package com.wzp.music_anaysis.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.wzp.music_anaysis.domain.BeatExerciseRecord;

/**
 * 节拍练习记录Mapper接口
 * 
 * @author wzp
 * @date 2025-11-07
 */
public interface BeatExerciseRecordMapper 
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
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBeatExerciseRecordByIds(Long[] ids);

    /**
     * 根据节拍数据ID删除练习记录（级联删除）
     * 
     * @param beatdataId 节拍数据ID
     * @return 结果
     */
    public int deleteByBeatdataId(Long beatdataId);

    /**
     * 获取排行榜
     * 
     * @param beatdataId 节拍数据ID
     * @param playbackSpeed 播放速度（可选）
     * @param practiceMode 练习模式（可选）
     * @param limit 返回数量
     * @return 排行榜记录
     */
    public List<BeatExerciseRecord> selectLeaderboard(
        @Param("beatdataId") Long beatdataId,
        @Param("playbackSpeed") BigDecimal playbackSpeed,
        @Param("practiceMode") String practiceMode,
        @Param("limit") int limit
    );

    /**
     * 获取用户最佳记录
     * 
     * @param userId 用户ID
     * @param beatdataId 节拍数据ID
     * @param playbackSpeed 播放速度（可选）
     * @param practiceMode 练习模式（可选）
     * @return 最佳记录
     */
    public BeatExerciseRecord selectUserBestRecord(
        @Param("userId") Long userId,
        @Param("beatdataId") Long beatdataId,
        @Param("playbackSpeed") BigDecimal playbackSpeed,
        @Param("practiceMode") String practiceMode
    );
}

