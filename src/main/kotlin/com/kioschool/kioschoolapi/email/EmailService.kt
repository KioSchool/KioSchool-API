package com.kioschool.kioschoolapi.email

import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val javaMailSender: JavaMailSender
) {
    fun sendTestEmail() {
        val message = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        helper.setTo("jin225675@naver.com")
        helper.setSubject("test")
        helper.setText("test", true)
        javaMailSender.send(message)
    }
}