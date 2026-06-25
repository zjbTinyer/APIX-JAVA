package com.apix.memory.mapper;

import com.apix.memory.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息 Mapper。
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT * FROM messages WHERE conversation_uid = #{conversationUid} " +
            "AND is_deleted = false ORDER BY msg_cursor ASC")
    List<Message> findByConversationUid(@Param("conversationUid") String conversationUid);

    @Select("SELECT MAX(msg_cursor) FROM messages WHERE conversation_id = #{conversationId}")
    Long getMaxCursor(@Param("conversationId") Long conversationId);

    @Select("SELECT MAX(msg_cursor) FROM messages WHERE conversation_uid = #{conversationUid}")
    Long getMaxCursorByConversationUid(@Param("conversationUid") String conversationUid);
}
