package com.wzp.music_anaysis.controller;

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
import com.wzp.music_anaysis.domain.Beatdata;
import com.wzp.music_anaysis.service.IBeatdataService;
import com.wzp.common.utils.poi.ExcelUtil;
import com.wzp.common.core.page.TableDataInfo;

/**
 * 节拍时刻Controller
 * 
 * @author wzp
 * @date 2025-10-04
 */
@RestController
@RequestMapping("/music_anaysis/beatdata")
public class BeatdataController extends BaseController
{
    @Autowired
    private IBeatdataService beatdataService;

    /**
     * 查询节拍时刻列表
     */
    @PreAuthorize("@ss.hasPermi('music_anaysis:beatdata:list')")
    @GetMapping("/list")
    public TableDataInfo list(Beatdata beatdata)
    {
        startPage();
        List<Beatdata> list = beatdataService.selectBeatdataList(beatdata);
        return getDataTable(list);
    }

    /**
     * 导出节拍时刻列表
     */
    @PreAuthorize("@ss.hasPermi('music_anaysis:beatdata:export')")
    @Log(title = "节拍时刻", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Beatdata beatdata)
    {
        List<Beatdata> list = beatdataService.selectBeatdataList(beatdata);
        ExcelUtil<Beatdata> util = new ExcelUtil<Beatdata>(Beatdata.class);
        util.exportExcel(response, list, "节拍时刻数据");
    }

    /**
     * 获取节拍时刻详细信息
     */
    @PreAuthorize("@ss.hasPermi('music_anaysis:beatdata:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(beatdataService.selectBeatdataById(id));
    }

    /**
     * 新增节拍时刻
     */
    @PreAuthorize("@ss.hasPermi('music_anaysis:beatdata:add')")
    @Log(title = "节拍时刻", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Beatdata beatdata)
    {
        return toAjax(beatdataService.insertBeatdata(beatdata));
    }

    /**
     * 修改节拍时刻
     */
    @PreAuthorize("@ss.hasPermi('music_anaysis:beatdata:edit')")
    @Log(title = "节拍时刻", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Beatdata beatdata)
    {
        return toAjax(beatdataService.updateBeatdata(beatdata));
    }

    /**
     * 删除节拍时刻
     */
    @PreAuthorize("@ss.hasPermi('music_anaysis:beatdata:remove')")
    @Log(title = "节拍时刻", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(beatdataService.deleteBeatdataByIds(ids));
    }
}
