package com.kioschool.kioschoolapi.global.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.Properties

@Configuration
class MailConfiguration(
    private val mailProperties: MailProperties
) {

    @Bean
    fun javaMailSender(): JavaMailSender {
        val mailSender = JavaMailSenderImpl()
        mailSender.host = mailProperties.host
        mailSender.port = mailProperties.port
        mailSender.username = mailProperties.username
        mailSender.password = mailProperties.password

        val props = Properties()
        props["mail.smtp.auth"] = mailProperties.properties.smtp.auth
        props["mail.smtp.starttls.enable"] = mailProperties.properties.smtp.starttls.enable
        mailSender.javaMailProperties = props

        return mailSender
    }
}
