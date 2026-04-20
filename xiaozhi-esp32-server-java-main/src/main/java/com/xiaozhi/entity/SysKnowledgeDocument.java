package com.xiaozhi.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class SysKnowledgeDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long museumId;
    private Long exhibitId;
    private String fileName;
    private String fileUrl;
    private String fileHash;
    private Long fileSize;
    private String status;
    private Integer chunkCount;
    private String errorMsg;
    private Date createdAt;
    private Date updatedAt;
}
