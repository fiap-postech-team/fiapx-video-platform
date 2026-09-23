package br.com.fiapx.notification.application.port.out;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;

public record OutboundNotification(
        @NotBlank(message = "recipient is required") @Email(message = "recipient must be a valid email address") @Size(max = 320, message = "recipient must have at most 320 characters")
        String recipient,
        @NotBlank(message = "subject is required")
        String subject,
        @NotBlank(message = "body is required")
        String body) {

}
