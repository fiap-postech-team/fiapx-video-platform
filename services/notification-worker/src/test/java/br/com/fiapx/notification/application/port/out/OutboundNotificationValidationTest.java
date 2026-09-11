package br.com.fiapx.notification.application.port.out;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboundNotificationValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void shouldAcceptValidNotification() {
        OutboundNotification notification = new OutboundNotification(
                "student@example.com",
                "Falha no processamento do vídeo",
                "O processamento falhou"
        );

        assertTrue(validator.validate(notification).isEmpty());
    }

    @Test
    void shouldRejectInvalidNotification() {
        OutboundNotification notification = new OutboundNotification(
                "invalid-email",
                "   ",
                ""
        );

        Set<ConstraintViolation<OutboundNotification>> violations = validator.validate(notification);

        assertEquals(3, violations.size());
    }
}
