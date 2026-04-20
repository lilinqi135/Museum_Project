package com.xiaozhi.controller;

import com.xiaozhi.common.web.PageFilter;
import com.xiaozhi.common.web.ResultMessage;
import com.xiaozhi.dto.param.ExhibitAddParam;
import com.xiaozhi.dto.param.ExhibitUpdateParam;
import com.xiaozhi.entity.SysExhibit;
import com.xiaozhi.service.SysExhibitService;
import com.xiaozhi.utils.DtoConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 展品管理控制器
 */
@RestController
@RequestMapping("/api/exhibit")
@Tag(name = "展品管理", description = "展品增删改查业务接口")
public class ExhibitController extends BaseController {

    @Resource
    private SysExhibitService exhibitService;

    @GetMapping("")
    @ResponseBody
    @Operation(summary = "查询展品列表", description = "支持按博物馆ID、名称、状态进行筛选")
    public ResultMessage query(SysExhibit exhibit, HttpServletRequest request) {
        try {
            if (exhibit == null) {
                return ResultMessage.error("查询参数不能为空");
            }
            if (exhibit.getMuseumId() == null) {
                return ResultMessage.error("museumId 不能为空");
            }
            PageFilter pageFilter = initPageFilter(request);
            List<SysExhibit> exhibitList = exhibitService.query(exhibit, pageFilter);
            
            ResultMessage result = ResultMessage.success();
            // 这里简单返回列表，实际生产中可以转换 DTO
            result.put("data", DtoConverter.toPageInfo(exhibitList, list -> list));
            return result;
        } catch (Exception e) {
            logger.error("查询展品失败", e);
            return ResultMessage.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @ResponseBody
    @Operation(summary = "获取展品详情")
    public ResultMessage getById(@PathVariable Long id) {
        try {
            if (id == null || id <= 0) {
                return ResultMessage.error("展品ID非法");
            }
            SysExhibit exhibit = exhibitService.selectById(id);
            if (exhibit == null) {
                return ResultMessage.error("展品不存在");
            }
            return ResultMessage.success(exhibit);
        } catch (Exception e) {
            logger.error("获取展品详情失败", e);
            return ResultMessage.error("获取失败: " + e.getMessage());
        }
    }

    @PostMapping("")
    @ResponseBody
    @Operation(summary = "新增展品")
    public ResultMessage add(@Valid @RequestBody ExhibitAddParam param) {
        try {
            SysExhibit exhibit = new SysExhibit();
            BeanUtils.copyProperties(param, exhibit);
            int row = exhibitService.add(exhibit);
            if (row > 0) {
                return ResultMessage.success("新增成功", exhibitService.selectById(exhibit.getId()));
            }
            return ResultMessage.error("新增失败");
        } catch (Exception e) {
            logger.error("新增展品失败", e);
            return ResultMessage.error("新增失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @ResponseBody
    @Operation(summary = "更新展品信息")
    public ResultMessage update(@PathVariable Long id, @Valid @RequestBody ExhibitUpdateParam param) {
        try {
            if (id == null || id <= 0) {
                return ResultMessage.error("展品ID非法");
            }
            if (!exhibitService.exists(id, param.getMuseumId())) {
                return ResultMessage.error("展品不存在或不属于该场馆");
            }

            SysExhibit exhibit = exhibitService.selectById(id);
            BeanUtils.copyProperties(param, exhibit);
            exhibit.setId(id);
            int row = exhibitService.update(exhibit);
            if (row > 0) {
                return ResultMessage.success("更新成功", exhibitService.selectById(id));
            }
            return ResultMessage.error("更新失败");
        } catch (Exception e) {
            logger.error("更新展品失败", e);
            return ResultMessage.error("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    @Operation(summary = "逻辑删除展品")
    public ResultMessage delete(@PathVariable Long id, @RequestParam Long museumId) {
        try {
            if (id == null || id <= 0 || museumId == null || museumId <= 0) {
                return ResultMessage.error("参数非法");
            }
            int row = exhibitService.delete(id, museumId);
            if (row > 0) {
                return ResultMessage.success("删除成功");
            }
            return ResultMessage.error("删除失败，展品不存在或不属于该场馆");
        } catch (Exception e) {
            logger.error("删除展品失败", e);
            return ResultMessage.error("删除失败: " + e.getMessage());
        }
    }
}
