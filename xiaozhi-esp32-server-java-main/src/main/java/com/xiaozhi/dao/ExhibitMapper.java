package com.xiaozhi.dao;

import com.xiaozhi.entity.SysExhibit;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ExhibitMapper {

    @Insert("""
            INSERT INTO sys_exhibit
            (museum_id, category_id, name, description, era, hall, image_url, tags, status, sort, deleted)
            VALUES
            (#{museumId}, #{categoryId}, #{name}, #{description}, #{era}, #{hall}, #{imageUrl}, #{tags}, #{status}, #{sort}, #{deleted})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysExhibit exhibit);

    @Update("""
            <script>
            UPDATE sys_exhibit
            <set>
                <if test="categoryId != null">category_id = #{categoryId},</if>
                <if test="name != null">name = #{name},</if>
                <if test="description != null">description = #{description},</if>
                <if test="era != null">era = #{era},</if>
                <if test="hall != null">hall = #{hall},</if>
                <if test="imageUrl != null">image_url = #{imageUrl},</if>
                <if test="tags != null">tags = #{tags},</if>
                <if test="status != null">status = #{status},</if>
                <if test="sort != null">sort = #{sort},</if>
                <if test="deleted != null">deleted = #{deleted},</if>
                updateTime = NOW()
            </set>
            WHERE id = #{id} AND museum_id = #{museumId}
            </script>
            """)
    int update(SysExhibit exhibit);

    @Select("""
            SELECT id, museum_id as museumId, category_id as categoryId, name, description, era, hall, 
                   image_url as imageUrl, tags, status, sort, deleted, createTime, updateTime
            FROM sys_exhibit
            WHERE id = #{id} AND deleted = 0
            """)
    SysExhibit selectById(@Param("id") Long id);

    @Select("""
            <script>
            SELECT id, museum_id as museumId, category_id as categoryId, name, description, era, hall, 
                   image_url as imageUrl, tags, status, sort, deleted, createTime, updateTime
            FROM sys_exhibit
            WHERE museum_id = #{museumId} AND deleted = 0
            <if test="name != null and name != ''">
                AND name LIKE CONCAT('%', #{name}, '%')
            </if>
            <if test="status != null and status != ''">
                AND status = #{status}
            </if>
            ORDER BY sort DESC, id DESC
            </script>
            """)
    List<SysExhibit> query(SysExhibit exhibit);

    @Update("UPDATE sys_exhibit SET deleted = 1, updateTime = NOW() WHERE id = #{id} AND museum_id = #{museumId}")
    int softDelete(@Param("id") Long id, @Param("museumId") Long museumId);

    @Select("SELECT COUNT(1) FROM sys_exhibit WHERE id = #{id} AND museum_id = #{museumId} AND deleted = 0")
    int exists(@Param("id") Long id, @Param("museumId") Long museumId);
}
