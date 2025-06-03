package learn.comet.chat.messages.data

import com.cometchat.chat.models.BaseMessage
import org.json.JSONObject

data class ReplyMetadata(
    val messageId: Int,
    val text: String,
    val type: String,
    val senderName: String,
    val senderUid: String
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("reply_message_id", messageId)
            put("reply_text", text)
            put("reply_type", type)
            put("reply_sender_name", senderName)
            put("reply_sender_uid", senderUid)
        }
    }

    companion object {
        fun fromJson(json: JSONObject?): ReplyMetadata? {
            if (json == null) return null
            return try {
                ReplyMetadata(
                    messageId = json.optInt("reply_message_id"),
                    text = json.optString("reply_text"),
                    type = json.optString("reply_type"),
                    senderName = json.optString("reply_sender_name"),
                    senderUid = json.optString("reply_sender_uid")
                )
            } catch (e: Exception) {
                null
            }
        }

        fun fromMessage(message: BaseMessage): ReplyMetadata {
            val text = when (message) {
                is com.cometchat.chat.models.TextMessage -> message.text
                is com.cometchat.chat.models.MediaMessage -> message.caption ?: "Media message"
                else -> "Message"
            }

            return ReplyMetadata(
                messageId = message.id,
                text = text,
                type = message.type,
                senderName = message.sender?.name ?: "Unknown",
                senderUid = message.sender?.uid ?: ""
            )
        }
    }
} 