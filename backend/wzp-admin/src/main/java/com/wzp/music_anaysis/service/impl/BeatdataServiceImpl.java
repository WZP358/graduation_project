package com.wzp.music_anaysis.service.impl;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import com.wzp.common.utils.DateUtils;
import com.wzp.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wzp.music_anaysis.mapper.BeatdataMapper;
import com.wzp.music_anaysis.domain.Beatdata;
import com.wzp.music_anaysis.service.IBeatdataService;
import com.wzp.music.mapper.MusicMapper;
import com.wzp.music.domain.Music;

/**
 * 节拍时刻Service业务层处理
 * 
 * @author wzp
 * @date 2025-10-04
 */
@Service
public class BeatdataServiceImpl implements IBeatdataService 
{
    private static final Logger logger = LoggerFactory.getLogger(BeatdataServiceImpl.class);
    
    @Autowired
    private BeatdataMapper beatdataMapper;

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired(required = false)
    private com.wzp.music_anaysis.mapper.BeatExerciseRecordMapper beatExerciseRecordMapper;

    /**
     * 查询节拍时刻
     * 
     * @param id 节拍时刻主键
     * @return 节拍时刻
     */
    @Override
    public Beatdata selectBeatdataById(Long id)
    {
        return beatdataMapper.selectBeatdataById(id);
    }

    /**
     * 查询节拍时刻列表
     * 
     * @param beatdata 节拍时刻
     * @return 节拍时刻
     */
    @Override
    public List<Beatdata> selectBeatdataList(Beatdata beatdata)
    {
        return beatdataMapper.selectBeatdataList(beatdata);
    }

    /**
     * 新增节拍时刻
     * 
     * @param beatdata 节拍时刻
     * @return 结果
     */
    @Override
    public int insertBeatdata(Beatdata beatdata)
    {
        beatdata.setCreatedBy(SecurityUtils.getUserId());
        
        Beatdata existingBeatdata = beatdataMapper.checkMusicNameAndUserExists(beatdata);
        if (existingBeatdata != null) {
            throw new RuntimeException("该音乐已存在相同识别方式的节拍数据");
        }
        
        Music queryMusic = new Music();
        queryMusic.setName(beatdata.getMusicName());
        List<Music> musicList = musicMapper.selectMusicList(queryMusic);
        
        if (musicList == null || musicList.isEmpty()) {
            throw new RuntimeException("未找到音乐: " + beatdata.getMusicName());
        }
        
        String filePath = musicList.get(0).getFilePath();
        beatdata.setFilePath(filePath);
        
        if ((beatdata.getBeatTimes() == null || beatdata.getBeatTimes().trim().isEmpty()) && 
            "librosa".equals(beatdata.getDetectionMode())) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                Map<String, String> requestMap = new HashMap<>();
                requestMap.put("audio_path", filePath);
                
                HttpEntity<Map<String, String>> request = new HttpEntity<>(requestMap, headers);
                
                String response = restTemplate.postForObject("http://localhost:5000/get_beatData", request, String.class);
                
                JsonNode jsonNode = objectMapper.readTree(response);
                String beatTimesStr = jsonNode.get("beat_times").toString();
                
                beatdata.setBeatTimes(beatTimesStr);
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch beat data: " + e.getMessage(), e);
            }
        }
        
        beatdata.setCreateTime(DateUtils.getNowDate());
        
        return beatdataMapper.insertBeatdata(beatdata);
    }

    /**
     * 修改节拍时刻
     * 
     * @param beatdata 节拍时刻
     * @return 结果
     */
    @Override
    public int updateBeatdata(Beatdata beatdata)
    {
        beatdata.setUpdateTime(DateUtils.getNowDate());
        return beatdataMapper.updateBeatdata(beatdata);
    }

    /**
     * 批量删除节拍时刻
     * 
     * @param ids 需要删除的节拍时刻主键
     * @return 结果
     */
    @Override
    public int deleteBeatdataByIds(Long[] ids)
    {
        return beatdataMapper.deleteBeatdataByIds(ids);
    }

    /**
     * 删除节拍时刻信息
     * 
     * @param id 节拍时刻主键
     * @return 结果
     */
    @Override
    public int deleteBeatdataById(Long id)
    {
        // 先删除关联的练习记录（如果存在BeatExerciseRecordMapper）
        if (beatExerciseRecordMapper != null) {
            try {
                beatExerciseRecordMapper.deleteByBeatdataId(id);
            } catch (Exception e) {
                // 如果表不存在或其他错误，忽略
                logger.warn("删除练习记录失败: " + e.getMessage());
            }
        }
        
        // 删除节拍数据（如果使用了外键级联，练习记录会自动删除）
        return beatdataMapper.deleteBeatdataById(id);
    }
}
