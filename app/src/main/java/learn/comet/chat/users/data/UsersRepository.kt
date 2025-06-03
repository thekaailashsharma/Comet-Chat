package learn.comet.chat.users.data

import android.util.Log
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "UsersRepository"

class UsersRepository {
    
    fun getUsers(limit: Int = 30, searchKeyword: String? = null): Flow<List<User>> = callbackFlow {
        val builder = UsersRequest.UsersRequestBuilder()
            .setLimit(limit)
        
        searchKeyword?.let {
            builder.setSearchKeyword(it)
        }
        
        val usersRequest = builder.build()
        
        try {
            usersRequest.fetchNext(object : CometChat.CallbackListener<List<User>>() {
                override fun onSuccess(users: List<User>) {
                    Log.d(TAG, "Successfully fetched ${users.size} users")
                    trySend(users.filter { it.uid != CometChat.getLoggedInUser()?.uid })
                }

                override fun onError(e: CometChatException?) {
                    Log.e(TAG, "Error fetching users: ${e?.message}")
                    e?.let { close(it) }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Exception while fetching users: ${e.message}")
            close(e)
        }

        awaitClose {
            Log.d(TAG, "Cleaning up users request")
        }
    }
} 