package com.wzp.music_anaysis.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.wzp.common.annotation.Log;
import com.wzp.common.core.controller.BaseController;
import com.wzp.common.core.domain.AjaxResult;
import com.wzp.common.enums.BusinessType;
import com.wzp.music_anaysis.domain.BeatExerciseRecord;
import com.wzp.music_anaysis.service.IBeatExerciseRecordService;
import com.wzp.common.core.page.TableDataInfo;

/**
 * 节拍练习记录Controller
 * 
 * @author wzp
 * @date 2025-11-07
 */
@RestController
@RequestMapping("/music_anaysis/exercise")
public class BeatExerciseRecordController extends BaseController
{
    @Autowired
    private IBeatExerciseRecordService beatExerciseRecordService;

    /**
     * 保存练习记录
     */
    @Log(title = "节拍练习记录", businessType = BusinessType.INSERT)
    @PostMapping("/record")
    public AjaxResult saveRecord(@RequestBody BeatExerciseRecord record)
    {
        // 自动填充用户信息
        if (record.getUserId() == null) {
            record.setUserId(getUserId());
        }
        if (record.getUserName() == null || record.getUserName().isEmpty()) {
            record.setUserName(getUsername());
        }
        
        int result = beatExerciseRecordService.insertBeatExerciseRecord(record);
        return result > 0 ? success(record.getId()) : error("保存失败");
    }

    /**
     * 获取排行榜
     */
    @GetMapping("/leaderboard")
    public AjaxResult getLeaderboard(
        @RequestParam Long beatdataId,
        @RequestParam(required = false) BigDecimal playbackSpeed,
        @RequestParam(required = false) String practiceMode,
        @RequestParam(defaultValue = "100") int limit
    )
    {
        List<BeatExerciseRecord> leaderboard = beatExerciseRecordService.getLeaderboard(
            beatdataId, playbackSpeed, practiceMode, limit
        );
        return success(leaderboard);
    }

    /**
     * 获取用户最佳记录
     */
    @GetMapping("/best")
    public AjaxResult getUserBest(
        @RequestParam Long beatdataId,
        @RequestParam(required = false) BigDecimal playbackSpeed,
        @RequestParam(required = false) String practiceMode
    )
    {
        Long userId = getUserId();
        BeatExerciseRecord bestRecord = beatExerciseRecordService.getUserBestRecord(
            userId, beatdataId, playbackSpeed, practiceMode
        );
        
        if (bestRecord != null) {
            // 查询该记录在排行榜中的排名
            List<BeatExerciseRecord> leaderboard = beatExerciseRecordService.getLeaderboard(
                beatdataId, playbackSpeed, practiceMode, 1000
            );
            for (int i = 0; i < leaderboard.size(); i++) {
                if (leaderboard.get(i).getId().equals(bestRecord.getId())) {
                    bestRecord.setRank(i + 1);
                    break;
                }
            }
        }
        
        return success(bestRecord);
    }

    /**
     * 获取用户练习历史
     */
    @GetMapping("/history")
    public TableDataInfo getHistory(BeatExerciseRecord record)
    {
        // 只查询当前用户的记录
        record.setUserId(getUserId());
        startPage();
        List<BeatExerciseRecord> list = beatExerciseRecordService.selectBeatExerciseRecordList(record);
        return getDataTable(list);
    }

    /**
     * 智能分析
     */
    @GetMapping("/analyze/{id}")
    public AjaxResult analyze(@PathVariable Long id)
    {
        Map<String, Object> analysis = beatExerciseRecordService.analyzeRecord(id);
        return analysis != null ? success(analysis) : error("记录不存在");
    }

    /**
     * 导出报告（文本格式）
     */
    @GetMapping("/export/{id}")
    public void exportReport(@PathVariable Long id, HttpServletResponse response)
    {
        try {
            BeatExerciseRecord record = beatExerciseRecordService.selectBeatExerciseRecordById(id);
            if (record == null) {
                return;
            }
            
            Map<String, Object> analysis = beatExerciseRecordService.analyzeRecord(id);
            
            StringBuilder report = new StringBuilder();
            report.append("节拍练习报告\n");
            report.append("==================\n");
            report.append("音乐：").append(record.getMusicName()).append("\n");
            report.append("用户：").append(record.getUserName()).append("\n");
            report.append("播放速度：").append(record.getPlaybackSpeed()).append("倍速\n");
            report.append("练习模式：").append("follow".equals(record.getPracticeMode()) ? "跟随模式" : "盲打模式").append("\n");
            report.append("\n");
            
            report.append("练习成绩\n");
            report.append("--------\n");
            report.append("准确率：").append(record.getAccuracy()).append("%\n");
            report.append("得分：").append(record.getScore()).append("\n");
            report.append("命中数：").append(record.getHitCount()).append("/").append(record.getTotalBeats()).append("\n");
            report.append("最高连击：").append(record.getMaxCombo()).append("\n");
            report.append("\n");
            
            report.append("详细统计\n");
            report.append("--------\n");
            int totalHits = record.getTotalHits();
            report.append(String.format("Perfect: %d次 (%.1f%%)\n", record.getPerfectCount(), 
                totalHits > 0 ? (double)record.getPerfectCount() / totalHits * 100 : 0));
            report.append(String.format("Good: %d次 (%.1f%%)\n", record.getGoodCount(),
                totalHits > 0 ? (double)record.getGoodCount() / totalHits * 100 : 0));
            report.append(String.format("OK: %d次 (%.1f%%)\n", record.getOkCount(),
                totalHits > 0 ? (double)record.getOkCount() / totalHits * 100 : 0));
            report.append(String.format("Miss: %d次 (%.1f%%)\n", record.getMissCount(),
                totalHits > 0 ? (double)record.getMissCount() / totalHits * 100 : 0));
            report.append("平均误差：±").append(record.getAvgError()).append("ms\n");
            report.append("\n");
            
            if (analysis != null) {
                report.append("智能分析\n");
                report.append("--------\n");
                report.append(analysis.get("summary")).append("\n\n");
                
                report.append("优点：\n");
                @SuppressWarnings("unchecked")
                List<String> strengths = (List<String>) analysis.get("strengths");
                for (String s : strengths) {
                    report.append("- ").append(s).append("\n");
                }
                report.append("\n");
                
                report.append("需要改进：\n");
                @SuppressWarnings("unchecked")
                List<String> weaknesses = (List<String>) analysis.get("weaknesses");
                for (String w : weaknesses) {
                    report.append("- ").append(w).append("\n");
                }
                report.append("\n");
                
                report.append("建议：\n");
                @SuppressWarnings("unchecked")
                List<String> suggestions = (List<String>) analysis.get("suggestions");
                for (String sg : suggestions) {
                    report.append("- ").append(sg).append("\n");
                }
            }
            
            // 设置响应
            response.setContentType("text/plain;charset=utf-8");
            response.setHeader("Content-Disposition", 
                "attachment; filename=" + java.net.URLEncoder.encode(
                    "节拍练习报告_" + record.getMusicName() + ".txt", "UTF-8"
                )
            );
            response.getWriter().write(report.toString());
            response.getWriter().flush();
            
        } catch (Exception e) {
            logger.error("导出报告失败", e);
        }
    }

    /**
     * 删除节拍练习记录
     */
    @PreAuthorize("@ss.hasPermi('music_anaysis:exercise:remove')")
    @Log(title = "节拍练习记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(beatExerciseRecordService.deleteBeatExerciseRecordByIds(ids));
    }
}

