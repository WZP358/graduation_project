package com.wzp.music_anaysis.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.wzp.music_anaysis.mapper.BeatExerciseRecordMapper;
import com.wzp.music_anaysis.domain.BeatExerciseRecord;
import com.wzp.music_anaysis.service.IBeatExerciseRecordService;

/**
 * 节拍练习记录Service业务层处理
 * 
 * @author wzp
 * @date 2025-11-07
 */
@Service
public class BeatExerciseRecordServiceImpl implements IBeatExerciseRecordService 
{
    @Autowired
    private BeatExerciseRecordMapper beatExerciseRecordMapper;

    /**
     * 查询节拍练习记录
     * 
     * @param id 节拍练习记录主键
     * @return 节拍练习记录
     */
    @Override
    public BeatExerciseRecord selectBeatExerciseRecordById(Long id)
    {
        return beatExerciseRecordMapper.selectBeatExerciseRecordById(id);
    }

    /**
     * 查询节拍练习记录列表
     * 
     * @param beatExerciseRecord 节拍练习记录
     * @return 节拍练习记录
     */
    @Override
    public List<BeatExerciseRecord> selectBeatExerciseRecordList(BeatExerciseRecord beatExerciseRecord)
    {
        return beatExerciseRecordMapper.selectBeatExerciseRecordList(beatExerciseRecord);
    }

    /**
     * 新增节拍练习记录
     * 
     * @param beatExerciseRecord 节拍练习记录
     * @return 结果
     */
    @Override
    public int insertBeatExerciseRecord(BeatExerciseRecord beatExerciseRecord)
    {
        return beatExerciseRecordMapper.insertBeatExerciseRecord(beatExerciseRecord);
    }

    /**
     * 删除节拍练习记录
     * 
     * @param id 节拍练习记录主键
     * @return 结果
     */
    @Override
    public int deleteBeatExerciseRecordById(Long id)
    {
        return beatExerciseRecordMapper.deleteBeatExerciseRecordById(id);
    }

    /**
     * 批量删除节拍练习记录
     * 
     * @param ids 需要删除的节拍练习记录主键
     * @return 结果
     */
    @Override
    public int deleteBeatExerciseRecordByIds(Long[] ids)
    {
        return beatExerciseRecordMapper.deleteBeatExerciseRecordByIds(ids);
    }

    /**
     * 获取排行榜
     */
    @Override
    public List<BeatExerciseRecord> getLeaderboard(Long beatdataId, BigDecimal playbackSpeed, String practiceMode, int limit)
    {
        List<BeatExerciseRecord> records = beatExerciseRecordMapper.selectLeaderboard(beatdataId, playbackSpeed, practiceMode, limit);
        
        // 添加排名
        for (int i = 0; i < records.size(); i++) {
            records.get(i).setRank(i + 1);
        }
        
        return records;
    }

    /**
     * 获取用户最佳记录
     */
    @Override
    public BeatExerciseRecord getUserBestRecord(Long userId, Long beatdataId, BigDecimal playbackSpeed, String practiceMode)
    {
        return beatExerciseRecordMapper.selectUserBestRecord(userId, beatdataId, playbackSpeed, practiceMode);
    }

    /**
     * 智能分析练习记录
     */
    @Override
    public Map<String, Object> analyzeRecord(Long id)
    {
        BeatExerciseRecord record = beatExerciseRecordMapper.selectBeatExerciseRecordById(id);
        if (record == null) {
            return null;
        }
        
        Map<String, Object> analysis = new HashMap<>();
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        
        int accuracy = record.getAccuracy();
        int avgError = record.getAvgError();
        int totalHits = record.getTotalHits();
        double perfectRate = totalHits > 0 ? (double) record.getPerfectCount() / totalHits * 100 : 0;
        
        // 总体评价
        String summary;
        if (accuracy >= 90) {
            summary = "🎉 表现优秀！您的节奏感知能力很强！";
        } else if (accuracy >= 70) {
            summary = "👍 表现良好，继续保持！";
        } else {
            summary = "💪 还有提升空间，多加练习！";
        }
        
        // 优点分析
        if (accuracy >= 80) {
            strengths.add(String.format("准确率达到 %d%%，节奏把握准确", accuracy));
        }
        if (record.getMaxCombo() >= 20) {
            strengths.add(String.format("最高连击 %d，稳定性很好", record.getMaxCombo()));
        }
        if (perfectRate >= 50) {
            strengths.add(String.format("Perfect率 %.1f%%，精准度高", perfectRate));
        }
        if (avgError <= 30) {
            strengths.add(String.format("平均误差仅 %dms，非常稳定", avgError));
        }
        
        // 不足分析
        if (accuracy < 70) {
            weaknesses.add("准确率较低，建议降低播放速度多加练习");
        }
        if (record.getMaxCombo() < 10) {
            weaknesses.add("连击数较低，需要提高稳定性");
        }
        if (record.getMissCount() > totalHits * 0.3) {
            weaknesses.add("误击率过高，注意只在节拍位置击打");
        }
        if (avgError > 60) {
            weaknesses.add("平均误差较大，需要更专注");
        }
        
        // 建议
        BigDecimal speed = record.getPlaybackSpeed();
        if (speed != null && speed.compareTo(new BigDecimal("1.0")) == 0 && accuracy < 80) {
            suggestions.add("可以尝试降低播放速度到0.75倍或0.5倍");
        }
        if ("blind".equals(record.getPracticeMode()) && accuracy < 70) {
            suggestions.add("建议先使用跟随模式熟悉节拍");
        }
        if (perfectRate < 30) {
            suggestions.add("多关注节拍点的精确位置，提高Perfect率");
        }
        suggestions.add("多次练习同一首歌曲，熟能生巧");
        
        if (strengths.isEmpty()) {
            strengths.add("继续努力，每次练习都是进步");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("暂无明显不足");
        }
        
        analysis.put("summary", summary);
        analysis.put("strengths", strengths);
        analysis.put("weaknesses", weaknesses);
        analysis.put("suggestions", suggestions);
        
        return analysis;
    }
}

