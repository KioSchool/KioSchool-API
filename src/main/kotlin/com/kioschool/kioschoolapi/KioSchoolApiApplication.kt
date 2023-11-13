package com.kioschool.kioschoolapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class KioSchoolApiApplication

fun main(args: Array<String>) {
    runApplication<KioSchoolApiApplication>(*args)
}
