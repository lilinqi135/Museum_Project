package com.xiaozhi.dao;

import com.xiaozhi.entity.SysKnowledgeDocument;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper {

    @Insert("""
            INSERT INTO sys_knowledge_document
            (museum_id, exhibit_id, file_name, file_url, file_hash, file_size, status, chunk_count, error_msg)
            VALUES
            (#{museumId}, #{exhibitId}, #{fileName}, #{fileUrl}, #{fileHash}, #{fileSize}, #{status}, #{chunkCount}, #{errorMsg})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysKnowledgeDocument document);

    @Select("""
            SELECT id,
                   museum_id AS museumId,
                   exhibit_id AS exhibitId,
                   file_name AS fileName,
                   file_url AS fileUrl,
                   file_hash AS fileHash,
                   file_size AS fileSize,
                   status,
                   chunk_count AS chunkCount,
                   error_msg AS errorMsg,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM sys_knowledge_document
            WHERE id = #{id}
            """)
    SysKnowledgeDocument selectById(@Param("id") Long id);

    @Select("""
            <script>
            SELECT id,
                   museum_id AS museumId,
                   exhibit_id AS exhibitId,
                   file_name AS fileName,
                   file_url AS fileUrl,
                   file_hash AS fileHash,
                   file_size AS fileSize,
                   status,
                   chunk_count AS chunkCount,
                   error_msg AS errorMsg,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM sys_knowledge_document
            WHERE museum_id = #{museumId}
            <if test='status != null and status != ""'>
                AND status = #{status}
            </if>
            ORDER BY id DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<SysKnowledgeDocument> selectByMuseumPaged(@Param("museumId") Long museumId,
                                                   @Param("status") String status,
                                                   @Param("offset") Integer offset,
                                                   @Param("size") Integer size);

    @Select("""
            <script>
            SELECT COUNT(1) FROM sys_knowledge_document
            WHERE museum_id = #{museumId}
            <if test='status != null and status != ""'>
                AND status = #{status}
            </if>
            </script>
            """)
    long countByMuseum(@Param("museumId") Long museumId, @Param("status") String status);

    @Select("""
            SELECT id,
                   museum_id AS museumId,
                   exhibit_id AS exhibitId,
                   file_name AS fileName,
                   file_url AS fileUrl,
                   file_hash AS fileHash,
                   file_size AS fileSize,
                   status,
                   chunk_count AS chunkCount,
                   error_msg AS errorMsg,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM sys_knowledge_document
            WHERE museum_id = #{museumId} AND file_hash = #{fileHash}
            LIMIT 1
            """)
    SysKnowledgeDocument selectByMuseumAndHash(@Param("museumId") Long museumId, @Param("fileHash") String fileHash);

    @Select("""
            SELECT id,
                   museum_id AS museumId,
                   exhibit_id AS exhibitId,
                   file_name AS fileName,
                   file_url AS fileUrl,
                   file_hash AS fileHash,
                   file_size AS fileSize,
                   status,
                   chunk_count AS chunkCount,
                   error_msg AS errorMsg,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM sys_knowledge_document
            WHERE status = 'COMPLETED'
            ORDER BY id ASC
            """)
    List<SysKnowledgeDocument> selectAllCompleted();

    @Update("""
            UPDATE sys_knowledge_document
            SET status = #{status},
                chunk_count = #{chunkCount},
                error_msg = #{errorMsg},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int updateStatusAndChunk(@Param("id") Long id,
                             @Param("status") String status,
                             @Param("chunkCount") Integer chunkCount,
                             @Param("errorMsg") String errorMsg);

    @Delete("DELETE FROM sys_knowledge_document WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("""
            SELECT id,
                   museum_id AS museumId,
                   exhibit_id AS exhibitId,
                   file_name AS fileName,
                   file_url AS fileUrl,
                   file_hash AS fileHash,
                   file_size AS fileSize,
                   status,
                   chunk_count AS chunkCount,
                   error_msg AS errorMsg,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM sys_knowledge_document
            WHERE museum_id = #{museumId}
            """)
    List<SysKnowledgeDocument> selectByMuseumId(@Param("museumId") Long museumId);

    @Select("""
            <script>
            SELECT id,
                   museum_id AS museumId,
                   exhibit_id AS exhibitId,
                   file_name AS fileName,
                   file_url AS fileUrl,
                   file_hash AS fileHash,
                   file_size AS fileSize,
                   status,
                   chunk_count AS chunkCount,
                   error_msg AS errorMsg,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM sys_knowledge_document
            WHERE museum_id = #{museumId} AND exhibit_id = #{exhibitId}
            ORDER BY id DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<SysKnowledgeDocument> selectByExhibitPaged(@Param("museumId") Long museumId,
                                                    @Param("exhibitId") Long exhibitId,
                                                    @Param("offset") Integer offset,
                                                    @Param("size") Integer size);

    @Select("SELECT COUNT(1) FROM sys_knowledge_document WHERE museum_id = #{museumId} AND exhibit_id = #{exhibitId}")
    long countByExhibit(@Param("museumId") Long museumId, @Param("exhibitId") Long exhibitId);

    @Update("""
            UPDATE sys_knowledge_document
            SET exhibit_id = #{exhibitId},
                updated_at = NOW()
            WHERE id = #{id} AND museum_id = #{museumId}
            """)
    int updateExhibitId(@Param("id") Long id, @Param("museumId") Long museumId, @Param("exhibitId") Long exhibitId);

    @Update("""
            UPDATE sys_knowledge_document
            SET exhibit_id = NULL,
                updated_at = NOW()
            WHERE id = #{id} AND museum_id = #{museumId}
            """)
    int unbindExhibitId(@Param("id") Long id, @Param("museumId") Long museumId);
}
