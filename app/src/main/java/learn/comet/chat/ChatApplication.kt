package learn.comet.chat

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import learn.comet.chat.core.CometChatManager

class ChatApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        initializeCometChat()
    }

    private fun initializeCometChat() {
        applicationScope.launch {
            try {
                CometChatManager.getInstance().initializeCometChat(applicationContext)
            } catch (e: Exception) {
                // Handle initialization error
                e.printStackTrace()
            }
        }
    }
} 