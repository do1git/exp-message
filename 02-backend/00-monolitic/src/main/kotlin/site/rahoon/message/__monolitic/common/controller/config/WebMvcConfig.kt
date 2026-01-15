package site.rahoon.message.__monolitic.common.controller.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import site.rahoon.message.__monolitic.common.controller.filter.CommonAuthInfoArgumentResolver

/**
 * WebMvc 설정
 * ArgumentResolver를 등록하여 AuthInfo 파라미터를 자동으로 주입합니다.
 */
@Configuration
class WebMvcConfig(
    private val commonAuthInfoArgumentResolver: CommonAuthInfoArgumentResolver
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        // AuthInfoArgumentResolver를 가장 먼저 등록하여 validation 전에 처리되도록 함
        resolvers.add(0, commonAuthInfoArgumentResolver)
    }
}
