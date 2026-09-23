package br.com.fiapx.notification.infrastructure.mail;

import br.com.fiapx.notification.application.port.out.NotificationSender;
import br.com.fiapx.notification.application.port.out.OutboundNotification;
import br.com.fiapx.notification.infrastructure.config.NotificationProperties;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@RequiredArgsConstructor
public class SmtpNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    @Override
    public void send(@Valid OutboundNotification notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFromAddress());
        message.setTo(notification.recipient());
        message.setSubject(notification.subject());
        message.setText(notification.body());

        mailSender.send(message);
    }
}
