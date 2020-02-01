package nu.fgv.register.server.config

import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.afterburner.AfterburnerModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.zalando.problem.ProblemModule
import org.zalando.problem.validation.ConstraintViolationProblemModule

@Configuration
class JacksonConfig {

    @Bean
    fun javaTimeModule() = JavaTimeModule()

    @Bean
    fun jdk8TimeModule() = Jdk8Module()

    @Bean
    fun hibernate5Module() = Hibernate5Module()

    @Bean
    fun afterburnerModule() = AfterburnerModule()

    @Bean
    fun problemModule() = ProblemModule()

    @Bean
    fun constraintViolationProblemModule() = ConstraintViolationProblemModule()
}
