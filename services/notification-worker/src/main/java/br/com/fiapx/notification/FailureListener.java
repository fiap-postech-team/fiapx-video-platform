package br.com.fiapx.notification;

import com.fasterxml.jackson.databind.*;

import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FailureListener {
    private final ObjectMapper json;
    private final JavaMailSender mail;
    private final NotificationRepository repo;

    public FailureListener(ObjectMapper j, JavaMailSender m, NotificationRepository r) {
        json = j;
        mail = m;
        repo = r;
    }

    @RabbitListener(queues = "video.notifications.failure.v1")
    @Transactional
    public void notifyFailure(String body) throws Exception {
        JsonNode e = json.readTree(body);
        UUID eventId = UUID.fromString(e.required("eventId").asText());
        if (repo.existsByEventId(eventId)) return;
        UUID jobId = UUID.fromString(e.required("jobId").asText());
        String recipient = e.path("recipient").asText("dev@fiapx.local");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipient);
        message.setFrom("noreply@fiapx.local");
        message.setSubject("Falha no processamento do vídeo");
        message.setText("O job " + jobId + " falhou: " + e.path("reason").asText("erro não informado"));
        mail.send(message);
        repo.save(new NotificationDelivery(eventId, jobId, recipient));
    }
}
