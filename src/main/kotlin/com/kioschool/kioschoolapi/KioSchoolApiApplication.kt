package com.kioschool.kioschoolapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KioSchoolApiApplication

fun main(args: Array<String>) {
    runApplication<KioSchoolApiApplication>(*args)
}
