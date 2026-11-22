package io.github.rochona.notificationsystem.integration;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class RabbitMQTestcontainersSmokeTest {

    @Container
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4-management");

    @Test
    void shouldStartRabbitMQAndEstablishAmqpConnection() throws Exception {

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbit.getHost());
        connectionFactory.setPort(rabbit.getAmqpPort());
        connectionFactory.setUsername(rabbit.getAdminUsername());
        connectionFactory.setPassword(rabbit.getAdminPassword());

        // Establishes connection & opens a channel
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {

            assertTrue(connection.isOpen());
            assertTrue(channel.isOpen());
        }
    }
}
