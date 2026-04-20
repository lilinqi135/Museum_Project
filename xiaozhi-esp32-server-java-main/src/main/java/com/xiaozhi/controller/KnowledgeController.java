package com.xiaozhi.controller;

import com.xiaozhi.common.web.ResultMessage;
import com.xiaozhi.common.web.ResultStatus;
import com.xiaozhi.dialogue.rag.service.KnowledgeService;
import com.xiaozhi.entity.SysKnowledgeDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/knowledge")
@Tag(name = "RAG知识库管理", description = "文档上传、状态追踪、重建索引")
public class KnowledgeController {

    @Resource
    private KnowledgeService knowledgeService;

    @PostMapping("/document/upload")
    @ResponseBody
    @Operation(summary = "上传知识文档", description = "上传后异步解析并建立向量索引")
    public ResultMessage upload(@RequestParam("file") MultipartFile file,
                                @RequestParam("museumId") Long museumId,
                                @RequestParam(value = "exhibitId", required = false) Long exhibitId) {
        try {
            SysKnowledgeDocument document = knowledgeService.uploadAndIndex(file, museumId, exhibitId);
            return ResultMessage.success("上传成功，已提交索引任务", document);
        } catch (Exception e) {
            return buildErrorResult(e, "上传失败");
        }
    }

    @GetMapping("/document/list")
    @ResponseBody
    @Operation(summary = "查询文档列表", description = "按博物馆和状态分页查询")
    public ResultMessage list(@RequestParam("museumId") Long museumId,
                              @RequestParam(value = "status", required = false) String status,
                              @RequestParam(value = "page", required = false) Integer page,
                              @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        try {
            return ResultMessage.success(knowledgeService.list(museumId, status, page, pageSize));
        } catch (Exception e) {
            return buildErrorResult(e, "查询失败");
        }
    }

    @GetMapping("/document/status/{documentId}")
    @ResponseBody
    @Operation(summary = "查询文档处理状态", description = "返回文档当前状态和错误信息")
    public ResultMessage status(@PathVariable("documentId") Long documentId,
                                @RequestParam("museumId") Long museumId) {
        try {
            return ResultMessage.success(knowledgeService.getStatus(documentId, museumId));
        } catch (Exception e) {
            return buildErrorResult(e, "查询状态失败");
        }
    }

    @DeleteMapping("/document/{documentId}")
    @ResponseBody
    @Operation(summary = "删除文档", description = "删除文档和切片记录，并重建向量库")
    public ResultMessage delete(@PathVariable("documentId") Long documentId,
                                @RequestParam("museumId") Long museumId) {
        try {
            knowledgeService.deleteDocument(documentId, museumId);
            return ResultMessage.success("删除成功");
        } catch (Exception e) {
            return buildErrorResult(e, "删除失败");
        }
    }

    @PostMapping("/rebuild")
    @ResponseBody
    @Operation(summary = "按博物馆重建索引", description = "将指定博物馆下的文档重置并重新索引")
    public ResultMessage rebuild(@RequestParam("museumId") Long museumId) {
        try {
            knowledgeService.rebuildByMuseum(museumId);
            return ResultMessage.success("重建任务已提交");
        } catch (Exception e) {
            return buildErrorResult(e, "重建失败");
        }
    }

    @GetMapping("/document/by-exhibit")
    @ResponseBody
    @Operation(summary = "查询展品关联文档列表")
    public ResultMessage listByExhibit(@RequestParam("museumId") Long museumId,
                                       @RequestParam("exhibitId") Long exhibitId,
                                       @RequestParam(value = "page", required = false) Integer page,
                                       @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        try {
            return ResultMessage.success(knowledgeService.listByExhibit(museumId, exhibitId, page, pageSize));
        } catch (Exception e) {
            return buildErrorResult(e, "查询失败");
        }
    }

    @PostMapping("/document/{documentId}/bind-exhibit")
    @ResponseBody
    @Operation(summary = "文档绑定展品")
    public ResultMessage bindExhibit(@PathVariable("documentId") Long documentId,
                                     @RequestParam("museumId") Long museumId,
                                     @RequestParam("exhibitId") Long exhibitId) {
        try {
            knowledgeService.bindDocumentToExhibit(documentId, museumId, exhibitId);
            return ResultMessage.success("绑定成功，已提交索引更新任务");
        } catch (Exception e) {
            return buildErrorResult(e, "绑定失败");
        }
    }

    @PostMapping("/document/{documentId}/unbind-exhibit")
    @ResponseBody
    @Operation(summary = "文档解绑展品")
    public ResultMessage unbindExhibit(@PathVariable("documentId") Long documentId,
                                       @RequestParam("museumId") Long museumId) {
        try {
            knowledgeService.unbindDocumentExhibit(documentId, museumId);
            return ResultMessage.success("解绑成功，已提交索引更新任务");
        } catch (Exception e) {
            return buildErrorResult(e, "解绑失败");
        }
    }

    private ResultMessage buildErrorResult(Exception exception, String fallbackMessage) {
        if (exception instanceof IllegalArgumentException) {
            return ResultMessage.error(ResultStatus.BAD_REQUEST, exception.getMessage());
        }
        return ResultMessage.error(fallbackMessage + ": " + exception.getMessage());
    }
}
