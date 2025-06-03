package learn.comet.chat.messages.data

import com.cometchat.chat.models.BaseMessage
import org.json.JSONObject

data class ReplyMetadata(
    val repliedToMessageId: Int,
    val repliedToText: String,
    val repliedToType: String,
    val repliedToSender: String
) {
    companion object {
        private const val KEY_REPLIED_TO_ID = "repliedToMessageId"
        private const val KEY_REPLIED_TO_TEXT = "repliedToText"
        private const val KEY_REPLIED_TO_TYPE = "repliedToType"
        private const val KEY_REPLIED_TO_SENDER = "repliedToSender"

        fun fromMessage(message: BaseMessage): ReplyMetadata {
            return ReplyMetadata(
                repliedToMessageId = message.id,
                repliedToText = when (message) {
                    is com.cometchat.chat.models.TextMessage -> message.text
                    is com.cometchat.chat.models.MediaMessage -> message.caption ?: "Media message"
                    else -> "Message"
                },
                repliedToType = message.type,
                repliedToSender = message.sender?.name ?: "Unknown"
            )
        }

        fun toJson(metadata: ReplyMetadata): JSONObject {
            return JSONObject().apply {
                put(KEY_REPLIED_TO_ID, metadata.repliedToMessageId)
                put(KEY_REPLIED_TO_TEXT, metadata.repliedToText)
                put(KEY_REPLIED_TO_TYPE, metadata.repliedToType)
                put(KEY_REPLIED_TO_SENDER, metadata.repliedToSender)
            }
        }

        fun fromJson(json: JSONObject): ReplyMetadata? {
            return try {
                ReplyMetadata(
                    repliedToMessageId = json.getInt(KEY_REPLIED_TO_ID),
                    repliedToText = json.getString(KEY_REPLIED_TO_TEXT),
                    repliedToType = json.getString(KEY_REPLIED_TO_TYPE),
                    repliedToSender = json.getString(KEY_REPLIED_TO_SENDER)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
} 