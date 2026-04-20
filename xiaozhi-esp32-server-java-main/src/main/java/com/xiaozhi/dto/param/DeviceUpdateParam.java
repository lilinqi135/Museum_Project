package com.xiaozhi.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新设备请求参数
 *
 * @author Joey
 */
@Data
@Schema(description = "更新设备请求参数")
public class DeviceUpdateParam {

    @Schema(description = "设备名称", example = "客厅导览终端")
    private String deviceName;

    @Schema(description = "角色ID", example = "1")
    private Integer roleId;

    @Schema(description = "可用function列表", example = "weather,time")
    private String functionNames;

    @Schema(description = "地理位置", example = "北京市朝阳区")
    private String location;

    @Schema(description = "所属博物馆ID", example = "1")
    private Long museumId;

    @Schema(description = "当前关联展品ID", example = "1")
    private Long exhibitId;
}
