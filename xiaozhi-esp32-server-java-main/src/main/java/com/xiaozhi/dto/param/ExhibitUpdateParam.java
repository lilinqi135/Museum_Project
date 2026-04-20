package com.xiaozhi.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 展品更新参数
 */
@Data
@Schema(description = "展品更新参数")
public class ExhibitUpdateParam {

    @Schema(description = "博物馆ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "博物馆ID不能为空")
    private Long museumId;

    @Schema(description = "展品名称")
    private String name;

    @Schema(description = "详细讲解文案")
    private String description;

    @Schema(description = "所属年代")
    private String era;

    @Schema(description = "所属展厅")
    private String hall;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "展品图片")
    private String imageUrl;

    @Schema(description = "标签，逗号分隔")
    private String tags;

    @Schema(description = "状态 ENABLED/DISABLED")
    private String status;

    @Schema(description = "排序值")
    private Integer sort;
}
