package site.rahoon.message.monolithic.user.domain

sealed class UserCommand {
    data class Create(
        val email: String,
        val password: String, // raw password
        val nickname: String,
    ) : UserCommand()

    data class Update(
        val id: String,
        val nickname: String,
    ) : UserCommand()

    data class Delete(
        val id: String,
    ) : UserCommand()
}
