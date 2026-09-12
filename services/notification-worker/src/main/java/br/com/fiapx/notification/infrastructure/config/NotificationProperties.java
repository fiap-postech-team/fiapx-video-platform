package br.com.fiapx.notification.infrastructure.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Validated
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    @NotBlank(message = "app.notification.default-recipient must not be blank")
    @Email(message = "app.notification.default-recipient must be a valid email address")
    @Size(max = 320, message = "app.notification.default-recipient must have at most 320 characters")
    private String defaultRecipient;

    @NotBlank(message = "app.notification.from-address must not be blank")
    @Email(message = "app.notification.from-address must be a valid email address")
    @Size(max = 320, message = "app.notification.from-address must have at most 320 characters")
    private String fromAddress;
}
