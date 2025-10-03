package com.wzp.music.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.wzp.common.annotation.Log;
import com.wzp.common.core.controller.BaseController;
import com.wzp.common.core.domain.AjaxResult;
import com.wzp.common.enums.BusinessType;
import com.wzp.music.domain.Music;
import com.wzp.music.service.IMusicService;
import com.wzp.common.utils.poi.ExcelUtil;
import com.wzp.common.core.page.TableDataInfo;

/**
 * 音乐管理Controller
 * 
 * @author wzp
 * @date 2025-10-03
 */
@RestController
@RequestMapping("/music/music_info")
public class MusicController extends BaseController
{
    @Autowired
    private IMusicService musicService;

    /**
     * 查询音乐管理列表
     */
    @PreAuthorize("@ss.hasPermi('music:music_info:list')")
    @GetMapping("/list")
    public TableDataInfo list(Music music)
    {
        startPage();
        List<Music> list = musicService.selectMusicList(music);
        return getDataTable(list);
    }

    /**
     * 导出音乐管理列表
     */
    @PreAuthorize("@ss.hasPermi('music:music_info:export')")
    @Log(title = "音乐管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Music music)
    {
        List<Music> list = musicService.selectMusicList(music);
        ExcelUtil<Music> util = new ExcelUtil<Music>(Music.class);
        util.exportExcel(response, list, "音乐管理数据");
    }

    /**
     * 获取音乐管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('music:music_info:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(musicService.selectMusicById(id));
    }

    /**
     * 新增音乐管理
     */
    @PreAuthorize("@ss.hasPermi('music:music_info:add')")
    @Log(title = "音乐管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Music music)
    {
        return toAjax(musicService.insertMusic(music));
    }

    /**
     * 修改音乐管理
     */
    @PreAuthorize("@ss.hasPermi('music:music_info:edit')")
    @Log(title = "音乐管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Music music)
    {
        return toAjax(musicService.updateMusic(music));
    }

    /**
     * 删除音乐管理
     */
    @PreAuthorize("@ss.hasPermi('music:music_info:remove')")
    @Log(title = "音乐管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(musicService.deleteMusicByIds(ids));
    }
}
