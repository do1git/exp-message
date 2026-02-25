package site.rahoon.message.monolithic.channeloperator.controller

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import site.rahoon.message.monolithic.channeloperator.application.ChannelOperatorCriteria

/**
 * ChannelOperator Controller 요청 DTO
 */
object ChannelOperatorRequest {
    data class Create(
        val userId: String,
        @field:NotBlank(message = "상담원 표시명은 필수입니다")
        @field:Size(min = 1, max = 50, message = "상담원 표시명은 1자 이상 50자 이하여야 합니다")
        val nickname: String,
    ) {
        fun toCriteria(channelId: String): ChannelOperatorCriteria.Create =
            ChannelOperatorCriteria.Create(
                channelId = channelId,
                userId = this.userId,
                nickname = this.nickname,
            )
    }

    data class Update(
        @field:NotBlank(message = "상담원 표시명은 필수입니다")
        @field:Size(min = 1, max = 50, message = "상담원 표시명은 1자 이상 50자 이하여야 합니다")
        val nickname: String,
    ) {
        fun toCriteria(channelOperatorId: String): ChannelOperatorCriteria.Update =
            ChannelOperatorCriteria.Update(
                channelOperatorId = channelOperatorId,
                nickname = this.nickname,
            )
    }
}
