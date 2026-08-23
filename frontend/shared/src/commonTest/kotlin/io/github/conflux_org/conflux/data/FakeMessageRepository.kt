package io.github.conflux_org.conflux.data

import io.github.conflux_org.conflux.domain.model.Message
import io.github.conflux_org.conflux.domain.model.User
import io.github.conflux_org.conflux.domain.repository.MessageRepository

class FakeMessageRepository(
    var shouldSucceed: Boolean = true,
    var mockMessages: List<Message> =
        listOf(
            Message(id = 1001L, author = User(id = 1L, name = "Alex"), content = "Hello world!"),
            Message(id = 1002L, author = User(id = 2L, name = "Bob"), content = "Welcome to Conflux!"),
        ),
    var errorMessage: String = "載入訊息失敗",
) : MessageRepository {
    override suspend fun getMessagesByChannelId(channelId: Long): Result<List<Message>> =
        if (shouldSucceed) {
            Result.success(mockMessages)
        } else {
            Result.failure(Exception(errorMessage))
        }

    override suspend fun sendMessage(
        channelId: Long,
        content: String,
    ): Result<Message> =
        if (shouldSucceed) {
            Result.success(mockMessages.first().copy(content = content))
        } else {
            Result.failure(Exception(errorMessage))
        }
}
