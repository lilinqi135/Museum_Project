package com.xiaozhi.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 展品知识库实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class SysExhibit implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 展品ID
     */
    private Long id;

    /**
     * 博物馆ID
     */
    private Long museumId;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 展品名称
     */
    private String name;

    /**
     * 详细讲解文案
     */
    private String description;

    /**
     * 所属年代
     */
    private String era;

    /**
     * 所属展厅
     */
    private String hall;

    /**
     * 展品图片
     */
    private String imageUrl;

    /**
     * 标签，逗号分隔
     */
    private String tags;

    /**
     * 状态 ENABLED/DISABLED
     */
    private String status;

    /**
     * 排序值
     */
    private Integer sort;

    /**
     * 逻辑删除 0/1
     */
    private Integer deleted;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
