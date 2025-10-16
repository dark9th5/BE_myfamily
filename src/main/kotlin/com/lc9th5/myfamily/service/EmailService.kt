package com.lc9th5.myfamily.service

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender
) {
    fun sendVerificationEmail(to: String, code: String) {
        val message = SimpleMailMessage()
        message.setTo(to)
        message.setSubject("Xác thực tài khoản MyFamily")
        message.setText("""
            Chào bạn,

            Cảm ơn bạn đã đăng ký tài khoản tại MyFamily!

            Mã xác thực của bạn là: $code

            Vui lòng nhập mã này vào app để hoàn tất đăng ký.

            Nếu bạn không đăng ký, hãy bỏ qua email này.

            Trân trọng,
            MyFamily Team ❤️
        """.trimIndent())
        mailSender.send(message)
    }
}