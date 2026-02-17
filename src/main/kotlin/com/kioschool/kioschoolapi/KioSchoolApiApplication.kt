package com.kioschool.kioschoolapi

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import java.util.*

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
class KioSchoolApiApplication {
    @PostConstruct
    fun started() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    }

    @Bean
    fun commandLineRunner(ctx: ApplicationContext): CommandLineRunner {
        return CommandLineRunner {
            println("!!! [STARTUP_CHECK] APPLICATION IS RUNNING NEW VERSION !!!")
            println("!!! Active Profiles: ${Arrays.toString(ctx.environment.activeProfiles)} !!!")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<KioSchoolApiApplication>(*args)
}
