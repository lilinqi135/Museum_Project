package com.xiaozhi.service;

import com.xiaozhi.common.web.PageFilter;
import com.xiaozhi.entity.SysExhibit;
import java.util.List;

/**
 * 展品管理服务接口
 */
public interface SysExhibitService {

    /**
     * 添加展品
     */
    int add(SysExhibit exhibit);

    /**
     * 更新展品
     */
    int update(SysExhibit exhibit);

    /**
     * 根据ID查询展品
     */
    SysExhibit selectById(Long id);

    /**
     * 分页查询展品
     */
    List<SysExhibit> query(SysExhibit exhibit, PageFilter pageFilter);

    /**
     * 逻辑删除展品
     */
    int delete(Long id, Long museumId);

    /**
     * 检查展品是否存在
     */
    boolean exists(Long id, Long museumId);
}
