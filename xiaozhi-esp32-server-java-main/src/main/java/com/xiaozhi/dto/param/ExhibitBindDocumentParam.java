package com.xiaozhi.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 展品绑定文档参数
 */
@Data
@Schema(description = "展品绑定文档参数")
public class ExhibitBindDocumentParam {

    @Schema(description = "博物馆ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "博物馆ID不能为空")
    private Long museumId;

    @Schema(description = "展品ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "展品ID不能为空")
    private Long exhibitId;

    @Schema(description = "文档ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文档ID不能为空")
    private Long documentId;
}
