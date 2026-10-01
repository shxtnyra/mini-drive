package com.example.minidrive.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailSendService {
    private final JavaMailSender mailSender;
    private final String fromEmail;

    public EmailSendService(@Value("${spring.mail.username}") String fromEmail, JavaMailSender mailSender) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Async
    public void sendVerificationCode(String toEmail, String code) {
        String text = "Здравствуйте!\n\n" +
                "Ваш код подтверждения для MiniDrive: " + code + "\n\n" +
                "Код действителен в течение 10 минут.\n" +
                "Если вы не регистрировались, просто проигнорируйте это письмо.";

        sendSimpleEmail(toEmail, "Код подтверждения MiniDrive", text);
    }

    @Async
    public void sendPasswordChangedNotification(String toEmail) {
        String text = "Ваш пароль был изменён. Если это были не вы — немедленно смените пароль.";

        sendSimpleEmail(toEmail, "Пароль от аккаунт изменён", text);
    }

    @Async
    public void sendPasswordResetCode(String email, String code) {
        String text = "Ваш код для сброса пароля: " + code + "\n\n" +
                "Код действителен 10 минут.\n" +
                "Если вы не запрашивали сброс — проигнорируйте письмо.";

        sendSimpleEmail(email, "Сброс пароля MiniDrive", text);
    }

    // TODO: Добавить try catch
    private void sendSimpleEmail(String toEmail, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }
}
