package com.xiaozhi.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 添加设备请求参数
 *
 * @author Joey
 */
@Data
@Schema(description = "添加设备请求参数")
public class DeviceAddParam {

    @Schema(description = "设备验证码", example = "ABCD1234", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "设备验证码不能为空")
    private String code;

    @Schema(description = "所属博物馆ID", example = "1")
    private Long museumId;

    @Schema(description = "初始展品ID", example = "1")
    private Long exhibitId;
}
