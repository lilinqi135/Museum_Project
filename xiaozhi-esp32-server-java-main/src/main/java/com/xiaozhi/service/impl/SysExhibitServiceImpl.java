package com.xiaozhi.service.impl;

import com.github.pagehelper.PageHelper;
import com.xiaozhi.common.web.PageFilter;
import com.xiaozhi.dao.ExhibitMapper;
import com.xiaozhi.entity.SysExhibit;
import com.xiaozhi.service.SysExhibitService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 展品管理服务实现类
 */
@Service
public class SysExhibitServiceImpl extends BaseServiceImpl implements SysExhibitService {
    private static final Logger logger = LoggerFactory.getLogger(SysExhibitServiceImpl.class);

    @Resource
    private ExhibitMapper exhibitMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int add(SysExhibit exhibit) {
        exhibit.setDeleted(0);
        return exhibitMapper.insert(exhibit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(SysExhibit exhibit) {
        return exhibitMapper.update(exhibit);
    }

    @Override
    public SysExhibit selectById(Long id) {
        return exhibitMapper.selectById(id);
    }

    @Override
    public List<SysExhibit> query(SysExhibit exhibit, PageFilter pageFilter) {
        if (pageFilter != null) {
            PageHelper.startPage(pageFilter.getStart(), pageFilter.getLimit());
        }
        return exhibitMapper.query(exhibit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(Long id, Long museumId) {
        // 先检查是否存在
        if (!exists(id, museumId)) {
            return 0;
        }
        return exhibitMapper.softDelete(id, museumId);
    }

    @Override
    public boolean exists(Long id, Long museumId) {
        return exhibitMapper.exists(id, museumId) > 0;
    }
}
