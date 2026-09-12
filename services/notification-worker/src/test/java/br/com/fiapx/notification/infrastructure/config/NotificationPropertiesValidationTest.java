package br.com.fiapx.notification.infrastructure.config;

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

class NotificationPropertiesValidationTest {

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
    void shouldAcceptValidConfiguration() {
        NotificationProperties properties = new NotificationProperties(
                "dev@fiapx.local",
                "noreply@fiapx.local"
        );

        assertTrue(validator.validate(properties).isEmpty());
    }

    @Test
    void shouldRejectInvalidEmailConfiguration() {
        NotificationProperties properties = new NotificationProperties(
                "invalid-email",
                "also-invalid"
        );

        Set<ConstraintViolation<NotificationProperties>> violations = validator.validate(properties);

        assertEquals(2, violations.size());
    }
}
