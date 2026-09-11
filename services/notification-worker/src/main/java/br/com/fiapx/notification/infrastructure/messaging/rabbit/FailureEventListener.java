package br.com.fiapx.notification.infrastructure.messaging.rabbit;

import br.com.fiapx.notification.application.port.in.NotificationResult;
import br.com.fiapx.notification.application.port.in.NotifyProcessingFailureUseCase;
import br.com.fiapx.notification.domain.model.FailureNotification;
import br.com.fiapx.notification.infrastructure.config.NotificationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class FailureEventListener {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final NotifyProcessingFailureUseCase useCase;
    private final NotificationProperties properties;

    @RabbitListener(queues = RabbitMessagingConfiguration.FAILURE_QUEUE)
    public void consume(String body) throws JsonProcessingException {
        FailureEventMessage message = objectMapper.readValue(body, FailureEventMessage.class);
        validate(message);

        FailureNotification failure = FailureEventMapper.toDomain(message, properties.getDefaultRecipient());
        NotificationResult result = useCase.notify(failure);

        log.info(
                "Failure notification handled eventId={} jobId={} result={}",
                failure.getEventId(),
                failure.getJobId(),
                result
        );
    }

    private void validate(FailureEventMessage message) {
        Set<ConstraintViolation<FailureEventMessage>> violations = validator.validate(message);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException("Invalid failure event", violations);
        }
    }
}
