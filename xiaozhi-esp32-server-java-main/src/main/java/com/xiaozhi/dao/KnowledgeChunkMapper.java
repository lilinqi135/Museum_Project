package com.xiaozhi.dao;

import com.xiaozhi.entity.SysKnowledgeChunk;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeChunkMapper {

    @Insert("""
            INSERT INTO sys_knowledge_chunk
            (document_id, chunk_index, content, vector_id, score_hint)
            VALUES
            (#{documentId}, #{chunkIndex}, #{content}, #{vectorId}, #{scoreHint})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysKnowledgeChunk chunk);

    @Select("""
            SELECT id,
                   document_id AS documentId,
                   chunk_index AS chunkIndex,
                   content,
                   vector_id AS vectorId,
                   score_hint AS scoreHint,
                   created_at AS createdAt
            FROM sys_knowledge_chunk
            WHERE document_id = #{documentId}
            ORDER BY chunk_index ASC
            """)
    List<SysKnowledgeChunk> selectByDocumentId(@Param("documentId") Long documentId);

    @Select("""
            SELECT c.id,
                   c.document_id AS documentId,
                   c.chunk_index AS chunkIndex,
                   c.content,
                   c.vector_id AS vectorId,
                   c.score_hint AS scoreHint,
                   c.created_at AS createdAt,
                   d.museum_id AS museumId,
                   d.exhibit_id AS exhibitId,
                   d.file_name AS source
            FROM sys_knowledge_chunk c
            INNER JOIN sys_knowledge_document d ON d.id = c.document_id
            WHERE d.status = 'COMPLETED'
            ORDER BY c.document_id ASC, c.chunk_index ASC
            """)
    List<SysKnowledgeChunk> selectAllCompletedChunks();

    @Delete("DELETE FROM sys_knowledge_chunk WHERE document_id = #{documentId}")
    int deleteByDocumentId(@Param("documentId") Long documentId);

    @Select("SELECT vector_id FROM sys_knowledge_chunk WHERE document_id = #{documentId} AND vector_id IS NOT NULL AND vector_id <> ''")
    List<String> selectVectorIdsByDocumentId(@Param("documentId") Long documentId);
}
