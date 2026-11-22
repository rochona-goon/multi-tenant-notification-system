package io.github.rochona.notificationsystem.outbox.service;

import io.github.rochona.notificationsystem.outbox.dto.ClaimedOutboxEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.connection.CorrelationData;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventClaimService outboxEventClaimService;
    private final RabbitTemplate rabbitTemplate;
    private final OutboxEventStatusService outboxEventStatusService;

    @Value("${notification.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${notification.rabbitmq.routing-key}")
    private String routingKey;


    public void publishPendingEvents() {

        List<ClaimedOutboxEventDto> events = outboxEventClaimService.claimEvents();

        for (ClaimedOutboxEventDto event : events) {
            try {
                Long notificationId = event.notificationId();

                CorrelationData correlationData =
                        new CorrelationData(event.eventId().toString());

                // Publishes to RabbitMQ here...
                rabbitTemplate.convertAndSend(exchangeName, routingKey, notificationId.toString(),
                        message -> {
                            message.getMessageProperties()
                                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            return message;
                        },
                        correlationData
                );

                var confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);

                if (!confirm.ack()) {
                    throw new IllegalStateException(
                            "RabbitMQ negatively acknowledged event: "
                                    + confirm.reason()
                    );
                }

                if (correlationData.getReturned() != null) {
                    throw new IllegalStateException(
                            "RabbitMQ returned an unroutable message"
                    );
                }

                outboxEventStatusService.markAsPublished(event.eventId());

            } catch (Exception exception) {
                outboxEventStatusService.scheduleRetry(
                        event.eventId(),
                        exception.getMessage() != null
                                ? exception.getMessage()
                                : exception.getClass().getSimpleName()
                );
            }
        }
    }

}
