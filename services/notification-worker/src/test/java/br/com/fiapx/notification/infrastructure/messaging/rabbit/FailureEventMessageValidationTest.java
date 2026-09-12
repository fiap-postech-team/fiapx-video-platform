package br.com.fiapx.notification.infrastructure.messaging.rabbit;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FailureEventMessageValidationTest {

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
    void shouldAcceptValidMessage() {
        FailureEventMessage message = new FailureEventMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "student@example.com",
                "timeout"
        );

        assertTrue(validator.validate(message).isEmpty());
    }

    @Test
    void shouldAcceptMissingRecipientBecauseMapperUsesConfiguredFallback() {
        FailureEventMessage message = new FailureEventMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "   ",
                "timeout"
        );

        assertTrue(validator.validate(message).isEmpty());
        org.junit.jupiter.api.Assertions.assertNull(message.getRecipient());
    }

    @Test
    void shouldRejectMissingEventIdAndJobId() {
        FailureEventMessage message = new FailureEventMessage(
                null,
                null,
                "student@example.com",
                "timeout"
        );

        Set<ConstraintViolation<FailureEventMessage>> violations = validator.validate(message);

        assertEquals(2, violations.size());
    }

    @Test
    void shouldRejectInvalidRecipient() {
        FailureEventMessage message = new FailureEventMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "invalid-email",
                "timeout"
        );

        Set<ConstraintViolation<FailureEventMessage>> violations = validator.validate(message);

        assertEquals(1, violations.size());
        assertEquals("recipient", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void shouldRejectReasonLargerThanFiveHundredCharacters() {
        FailureEventMessage message = new FailureEventMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "student@example.com",
                "x".repeat(501)
        );

        Set<ConstraintViolation<FailureEventMessage>> violations = validator.validate(message);

        assertEquals(1, violations.size());
        assertEquals("reason", violations.iterator().next().getPropertyPath().toString());
    }
}
