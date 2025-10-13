package com.wzp.music.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.wzp.common.annotation.Excel;
import com.wzp.common.core.domain.BaseEntity;

/**
 * 音乐管理对象 music
 * 
 * @author wzp
 * @date 2025-10-03
 */
public class Music extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 音乐ID */
    private Long id;

    /** 名称 */
    @Excel(name = "名称")
    private String name;

    /** 作者 */
    @Excel(name = "作者")
    private String author;

    /** 内容介绍 */
    @Excel(name = "内容介绍")
    private String description;

    /** 上传用户ID */
    @Excel(name = "上传用户ID")
    private Long uploadUserId;

    /** 上传时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "上传时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date uploadTime;



    /** 存放路径 */
    @Excel(name = "存放路径")
    private String filePath;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }

    public void setAuthor(String author) 
    {
        this.author = author;
    }

    public String getAuthor() 
    {
        return author;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setUploadUserId(Long uploadUserId) 
    {
        this.uploadUserId = uploadUserId;
    }

    public Long getUploadUserId() 
    {
        return uploadUserId;
    }

    public void setUploadTime(Date uploadTime) 
    {
        this.uploadTime = uploadTime;
    }

    public Date getUploadTime() 
    {
        return uploadTime;
    }



    public void setFilePath(String filePath) 
    {
        this.filePath = filePath;
    }

    public String getFilePath() 
    {
        return filePath;
    }

    @Override
    public String toString() {
        System.err.println(getFilePath());
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("author", getAuthor())
            .append("description", getDescription())
            .append("uploadUserId", getUploadUserId())
            .append("uploadTime", getUploadTime())
            .append("filePath", getFilePath())
            .toString();
    }
}
