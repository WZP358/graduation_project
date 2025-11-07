package com.wzp.music_anaysis.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.wzp.common.core.domain.BaseEntity;

/**
 * 节拍练习记录对象 beat_exercise_record
 * 
 * @author wzp
 * @date 2025-11-07
 */
public class BeatExerciseRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户名称 */
    private String userName;

    /** 节拍数据ID */
    private Long beatdataId;

    /** 音乐名称 */
    private String musicName;

    /** 播放速度 */
    private BigDecimal playbackSpeed;

    /** 练习模式 */
    private String practiceMode;

    /** 准确率 */
    private Integer accuracy;

    /** 得分 */
    private Integer score;

    /** 命中数 */
    private Integer hitCount;

    /** 总节拍数 */
    private Integer totalBeats;

    /** 总击打数 */
    private Integer totalHits;

    /** 平均误差(ms) */
    private Integer avgError;

    /** 最高连击 */
    private Integer maxCombo;

    /** Perfect数量 */
    private Integer perfectCount;

    /** Good数量 */
    private Integer goodCount;

    /** OK数量 */
    private Integer okCount;

    /** Miss数量 */
    private Integer missCount;

    /** 练习时长(秒) */
    private Integer practiceTime;

    /** 排名 (非数据库字段，用于查询结果) */
    private Integer rank;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setUserName(String userName) 
    {
        this.userName = userName;
    }

    public String getUserName() 
    {
        return userName;
    }

    public void setBeatdataId(Long beatdataId) 
    {
        this.beatdataId = beatdataId;
    }

    public Long getBeatdataId() 
    {
        return beatdataId;
    }

    public void setMusicName(String musicName) 
    {
        this.musicName = musicName;
    }

    public String getMusicName() 
    {
        return musicName;
    }

    public void setPlaybackSpeed(BigDecimal playbackSpeed) 
    {
        this.playbackSpeed = playbackSpeed;
    }

    public BigDecimal getPlaybackSpeed() 
    {
        return playbackSpeed;
    }

    public void setPracticeMode(String practiceMode) 
    {
        this.practiceMode = practiceMode;
    }

    public String getPracticeMode() 
    {
        return practiceMode;
    }

    public void setAccuracy(Integer accuracy) 
    {
        this.accuracy = accuracy;
    }

    public Integer getAccuracy() 
    {
        return accuracy;
    }

    public void setScore(Integer score) 
    {
        this.score = score;
    }

    public Integer getScore() 
    {
        return score;
    }

    public void setHitCount(Integer hitCount) 
    {
        this.hitCount = hitCount;
    }

    public Integer getHitCount() 
    {
        return hitCount;
    }

    public void setTotalBeats(Integer totalBeats) 
    {
        this.totalBeats = totalBeats;
    }

    public Integer getTotalBeats() 
    {
        return totalBeats;
    }

    public void setTotalHits(Integer totalHits) 
    {
        this.totalHits = totalHits;
    }

    public Integer getTotalHits() 
    {
        return totalHits;
    }

    public void setAvgError(Integer avgError) 
    {
        this.avgError = avgError;
    }

    public Integer getAvgError() 
    {
        return avgError;
    }

    public void setMaxCombo(Integer maxCombo) 
    {
        this.maxCombo = maxCombo;
    }

    public Integer getMaxCombo() 
    {
        return maxCombo;
    }

    public void setPerfectCount(Integer perfectCount) 
    {
        this.perfectCount = perfectCount;
    }

    public Integer getPerfectCount() 
    {
        return perfectCount;
    }

    public void setGoodCount(Integer goodCount) 
    {
        this.goodCount = goodCount;
    }

    public Integer getGoodCount() 
    {
        return goodCount;
    }

    public void setOkCount(Integer okCount) 
    {
        this.okCount = okCount;
    }

    public Integer getOkCount() 
    {
        return okCount;
    }

    public void setMissCount(Integer missCount) 
    {
        this.missCount = missCount;
    }

    public Integer getMissCount() 
    {
        return missCount;
    }

    public void setPracticeTime(Integer practiceTime) 
    {
        this.practiceTime = practiceTime;
    }

    public Integer getPracticeTime() 
    {
        return practiceTime;
    }

    public void setRank(Integer rank) 
    {
        this.rank = rank;
    }

    public Integer getRank() 
    {
        return rank;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("userName", getUserName())
            .append("beatdataId", getBeatdataId())
            .append("musicName", getMusicName())
            .append("playbackSpeed", getPlaybackSpeed())
            .append("practiceMode", getPracticeMode())
            .append("accuracy", getAccuracy())
            .append("score", getScore())
            .append("hitCount", getHitCount())
            .append("totalBeats", getTotalBeats())
            .append("totalHits", getTotalHits())
            .append("avgError", getAvgError())
            .append("maxCombo", getMaxCombo())
            .append("perfectCount", getPerfectCount())
            .append("goodCount", getGoodCount())
            .append("okCount", getOkCount())
            .append("missCount", getMissCount())
            .append("practiceTime", getPracticeTime())
            .append("createTime", getCreateTime())
            .toString();
    }
}

