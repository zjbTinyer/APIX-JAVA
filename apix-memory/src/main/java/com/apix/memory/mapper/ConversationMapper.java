package com.apix.memory.mapper;

import com.apix.memory.entity.Conversation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 会话 Mapper。
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    @Select("SELECT * FROM conversations WHERE user_uid = #{userUid} AND is_deleted = false " +
            "ORDER BY is_pinned DESC, last_active_at DESC")
    List<Conversation> findActiveByUserUid(@Param("userUid") String userUid);

    @Select("SELECT * FROM conversations WHERE conversation_uid = #{conversationUid}")
    Conversation findByConversationUid(@Param("conversationUid") String conversationUid);

    @Update("UPDATE conversations SET is_deleted = true WHERE conversation_uid = #{conversationUid}")
    void softDelete(@Param("conversationUid") String conversationUid);
}
