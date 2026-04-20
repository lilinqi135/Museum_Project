package com.xiaozhi.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class SysKnowledgeChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long documentId;
    private Integer chunkIndex;
    private String content;
    private String vectorId;
    private BigDecimal scoreHint;
    private Date createdAt;

    // 仅用于重建向量库时携带文档元数据，不落库到 sys_knowledge_chunk
    private Long museumId;
    private Long exhibitId;
    private String source;
}
