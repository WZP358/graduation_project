package com.wzp.music.service.impl;

import java.io.File;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.wzp.common.utils.StringUtils;
import com.wzp.music.mapper.MusicMapper;
import com.wzp.music.domain.Music;
import com.wzp.music.service.IMusicService;
import com.wzp.music_anaysis.mapper.BeatdataMapper;

/**
 * 音乐管理Service业务层处理
 * 
 * @author wzp
 * @date 2025-10-03
 */
@Service
public class MusicServiceImpl implements IMusicService 
{
    private static final Logger log = LoggerFactory.getLogger(MusicServiceImpl.class);
    
    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private BeatdataMapper beatdataMapper;

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
        String oldFilePath = music.getFilePath();
        log.info("========== 开始插入音乐 ==========");
        log.info("接收到的 filePath: {}", oldFilePath);
        
        int result = musicMapper.insertMusic(music);
        log.info("数据库插入结果: {}, 获取到的ID: {}", result, music.getId());
        
        if (result > 0 && StringUtils.isNotEmpty(oldFilePath))
        {
            Long musicId = music.getId();
            
            String realOldPath = oldFilePath;
            if (oldFilePath.startsWith("/profile/"))
            {
                realOldPath = "E:\\code\\graduation_project\\uploadPath" + oldFilePath.substring(8);
            }
            log.info("转换后的真实路径: {}", realOldPath);
            
            File oldFile = new File(realOldPath);
            log.info("文件是否存在: {}", oldFile.exists());
            
            if (oldFile.exists())
            {
                String extension = FilenameUtils.getExtension(oldFile.getName());
                String newFileName = musicId + "." + extension;
                String newFilePath = "E:\\code\\graduation_project\\uploadPath\\upload\\" + newFileName;
                
                log.info("新文件名: {}", newFileName);
                log.info("新文件路径: {}", newFilePath);
                
                File newFile = new File(newFilePath);
                File parentDir = newFile.getParentFile();
                if (!parentDir.exists())
                {
                    log.info("创建目录: {}", parentDir.mkdirs());
                }
                
                if (oldFile.renameTo(newFile))
                {
                    log.info("renameTo 成功");
                    music.setFilePath(newFilePath);
                    musicMapper.updateMusic(music);
                }
                else
                {
                    log.warn("renameTo 失败，尝试使用 FileUtils.moveFile");
                    try
                    {
                        org.apache.commons.io.FileUtils.moveFile(oldFile, newFile);
                        log.info("FileUtils.moveFile 成功");
                        music.setFilePath(newFilePath);
                        musicMapper.updateMusic(music);
                    }
                    catch (Exception e)
                    {
                        log.error("文件移动失败", e);
                    }
                }
            }
            else
            {
                log.error("原文件不存在: {}", realOldPath);
            }
        }
        else
        {
            log.warn("跳过文件重命名: result={}, filePath={}", result, oldFilePath);
        }
        
        log.info("========== 插入音乐完成 ==========");
        return result;
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
        for (Long id : ids)
        {
            Music music = musicMapper.selectMusicById(id);
            if (music != null)
            {
                if (StringUtils.isNotEmpty(music.getFilePath()))
                {
                    File file = new File(music.getFilePath());
                    if (file.exists())
                    {
                        if (file.delete())
                        {
                            log.info("成功删除文件: {}", music.getFilePath());
                        }
                        else
                        {
                            log.warn("删除文件失败: {}", music.getFilePath());
                        }
                    }
                }
                
                if (StringUtils.isNotEmpty(music.getName()))
                {
                    int deletedCount = beatdataMapper.deleteBeatdataByMusicName(music.getName());
                    log.info("删除音乐 {} 关联的 {} 条beatdata记录", music.getName(), deletedCount);
                }
            }
        }
        return musicMapper.deleteMusicByIds(ids);
    }

    @Override
    public int deleteMusicById(Long id)
    {
        Music music = musicMapper.selectMusicById(id);
        if (music != null)
        {
            if (StringUtils.isNotEmpty(music.getFilePath()))
            {
                File file = new File(music.getFilePath());
                if (file.exists())
                {
                    if (file.delete())
                    {
                        log.info("成功删除文件: {}", music.getFilePath());
                    }
                    else
                    {
                        log.warn("删除文件失败: {}", music.getFilePath());
                    }
                }
            }
            
            if (StringUtils.isNotEmpty(music.getName()))
            {
                int deletedCount = beatdataMapper.deleteBeatdataByMusicName(music.getName());
                log.info("删除音乐 {} 关联的 {} 条beatdata记录", music.getName(), deletedCount);
            }
        }
        return musicMapper.deleteMusicById(id);
    }
}
