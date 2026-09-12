package br.com.fiapx.notification.infrastructure.config;

import br.com.fiapx.notification.application.port.in.NotifyProcessingFailureUseCase;
import br.com.fiapx.notification.application.port.out.NotificationDeliveryRepository;
import br.com.fiapx.notification.application.port.out.NotificationSender;
import br.com.fiapx.notification.application.service.NotifyProcessingFailureService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NotificationProperties.class)
public class ApplicationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    NotifyProcessingFailureUseCase notifyProcessingFailureUseCase(
            NotificationDeliveryRepository deliveryRepository,
            NotificationSender notificationSender,
            Clock clock
    ) {
        return new NotifyProcessingFailureService(deliveryRepository, notificationSender, clock);
    }
}
