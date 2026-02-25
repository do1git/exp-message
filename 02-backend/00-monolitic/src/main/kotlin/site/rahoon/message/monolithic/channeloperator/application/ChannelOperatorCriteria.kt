package site.rahoon.message.monolithic.channeloperator.application

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import site.rahoon.message.monolithic.channeloperator.domain.ChannelOperatorCommand

/**
 * ChannelOperator Application Layer 입력 DTO
 */
object ChannelOperatorCriteria {
    data class Create(
        val channelId: String,
        val userId: String,
        @field:NotBlank(message = "상담원 표시명은 필수입니다")
        @field:Size(min = 1, max = 50, message = "상담원 표시명은 1자 이상 50자 이하여야 합니다")
        val nickname: String,
    ) {
        fun toCommand(): ChannelOperatorCommand.Create =
            ChannelOperatorCommand.Create(
                channelId = this.channelId,
                userId = this.userId,
                nickname = this.nickname,
            )
    }

    data class Update(
        val channelOperatorId: String,
        @field:NotBlank(message = "상담원 표시명은 필수입니다")
        @field:Size(min = 1, max = 50, message = "상담원 표시명은 1자 이상 50자 이하여야 합니다")
        val nickname: String,
    ) {
        fun toCommand(): ChannelOperatorCommand.Update =
            ChannelOperatorCommand.Update(
                id = this.channelOperatorId,
                nickname = this.nickname,
            )
    }

    data class Delete(
        val channelOperatorId: String,
    ) {
        fun toCommand(): ChannelOperatorCommand.Delete =
            ChannelOperatorCommand.Delete(
                id = this.channelOperatorId,
            )
    }
}
