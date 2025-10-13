package com.wzp.music_anaysis.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.wzp.common.annotation.Excel;
import com.wzp.common.core.domain.BaseEntity;

/**
 * 节拍时刻对象 beatdata
 * 
 * @author wzp
 * @date 2025-10-04
 */
public class Beatdata extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 音乐名称 */
    @Excel(name = "音乐名称")
    private String musicName;

    /** 节拍时刻数组(JSON格式) */
    @Excel(name = "节拍时刻数组(JSON格式)")
    private String beatTimes;

    /** 创建用户ID */
    @Excel(name = "创建用户ID")
    private Long createdBy;

    /** 文件在服务器上的存放路径 */
    @Excel(name = "文件在服务器上的存放路径")
    private String filePath;

    /** 创建用户名称 */
    @Excel(name = "创建用户名称")
    private String creatorName;

    /** 识别方式 */
    private String detectionMode;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setMusicName(String musicName) 
    {
        this.musicName = musicName;
    }

    public String getMusicName() 
    {
        return musicName;
    }

    public void setBeatTimes(String beatTimes) 
    {
        this.beatTimes = beatTimes;
    }

    public String getBeatTimes() 
    {
        return beatTimes;
    }

    public void setCreatedBy(Long createdBy) 
    {
        this.createdBy = createdBy;
    }

    public Long getCreatedBy() 
    {
        return createdBy;
    }

    public void setFilePath(String filePath) 
    {
        this.filePath = filePath;
    }

    public String getFilePath() 
    {
        return filePath;
    }

    public void setCreatorName(String creatorName) 
    {
        this.creatorName = creatorName;
    }

    public String getCreatorName() 
    {
        return creatorName;
    }

    public void setDetectionMode(String detectionMode) 
    {
        this.detectionMode = detectionMode;
    }

    public String getDetectionMode() 
    {
        return detectionMode;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("musicName", getMusicName())
            .append("beatTimes", getBeatTimes())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("createdBy", getCreatedBy())
            .append("filePath", getFilePath())
            .append("creatorName", getCreatorName())
            .append("detectionMode", getDetectionMode())
            .toString();
    }
}
