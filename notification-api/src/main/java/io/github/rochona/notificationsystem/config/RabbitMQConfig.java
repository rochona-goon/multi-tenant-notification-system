package io.github.rochona.notificationsystem.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${notification.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${notification.rabbitmq.queue}")
    private String queueName;

    @Value("${notification.rabbitmq.routing-key}")
    private String routingKey;



    @Value("${notification.rabbitmq.retry-exchange}")
    private String retryExchangeName;

    @Value("${notification.rabbitmq.retry-5s-queue}")
    private String retry5sQueueName;

    @Value("${notification.rabbitmq.retry-10s-queue}")
    private String retry10sQueueName;

    @Value("${notification.rabbitmq.retry-5s-routing-key}")
    private String retry5sRoutingKey;

    @Value("${notification.rabbitmq.retry-10s-routing-key}")
    private String retry10sRoutingKey;



    @Value("${notification.rabbitmq.dlq-exchange}")
    private String dlqExchangeName;

    @Value("${notification.rabbitmq.dlq-queue}")
    private String dlqQueueName;

    @Value("${notification.rabbitmq.dlq-routing-key}")
    private String dlqRoutingKey;




    @Bean
    public DirectExchange notificationExchange(){
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding notificationBinding(@Qualifier("notificationQueue") Queue notificationQueue,
                                       @Qualifier("notificationExchange") DirectExchange notificationExchange) {

        return BindingBuilder
                .bind(notificationQueue)
                .to(notificationExchange)
                .with(routingKey);
    }



    @Bean
    public DirectExchange notificationRetryExchange(){
        return new DirectExchange(retryExchangeName, true, false);
    }


    // For 5 seconds...

    @Bean
    public Queue notificationRetry5sQueue(){
        return QueueBuilder.durable(retry5sQueueName)
                .withArgument("x-message-ttl", 5000)
                .withArgument("x-dead-letter-exchange", exchangeName)
                .withArgument("x-dead-letter-routing-key", routingKey)
                .build();
    }

    @Bean
    public Binding notificationRetry5sBinding( @Qualifier("notificationRetry5sQueue") Queue notificationRetry5sQueue,
                                               @Qualifier("notificationRetryExchange") DirectExchange notificationRetryExchange){

        return BindingBuilder.bind(notificationRetry5sQueue)
                .to(notificationRetryExchange)
                .with(retry5sRoutingKey);
    }



    // For 10 seconds...

    @Bean
    public Queue notificationRetry10sQueue(){
        return QueueBuilder.durable(retry10sQueueName)
                .withArgument("x-message-ttl", 10000)
                .withArgument("x-dead-letter-exchange", exchangeName)
                .withArgument("x-dead-letter-routing-key", routingKey)
                .build();
    }


    @Bean
    public Binding notificationRetry10sBinding(@Qualifier("notificationRetry10sQueue") Queue notificationRetry10sQueue,
                                               @Qualifier("notificationRetryExchange") DirectExchange notificationRetryExchange){

        return BindingBuilder.bind(notificationRetry10sQueue)
                .to(notificationRetryExchange)
                .with(retry10sRoutingKey);
    }



    // For Dead letter...

    @Bean
    public DirectExchange notificationDlqExchange(){
        return new DirectExchange(dlqExchangeName, true, false);
    }


    @Bean
    public Queue notificationDlqQueue(){
        return QueueBuilder.durable(dlqQueueName).build();
    }


    @Bean
    public Binding notificationDlqBinding( @Qualifier("notificationDlqQueue") Queue notificationDlqQueue,
                                           @Qualifier("notificationDlqExchange") DirectExchange notificationDlqExchange){

        return BindingBuilder.bind(notificationDlqQueue)
                .to(notificationDlqExchange)
                .with(dlqRoutingKey);

    }





//    @Bean
//    public ApplicationRunner checkRabbitAdmin(ApplicationContext context) {
//        return args -> {
//            System.out.println("=== RABBITMQ BEAN CHECK ===");
//
//            System.out.println(
//                    "ConnectionFactory: " +
//                            context.containsBean("rabbitConnectionFactory")
//            );
//
//            System.out.println(
//                    "RabbitTemplate: " +
//                            context.containsBean("rabbitTemplate")
//            );
//
//            System.out.println(
//                    "AmqpAdmin: " +
//                            context.getBeansOfType(AmqpAdmin.class).keySet()
//            );
//
//            System.out.println("===========================");
//        };
//    }
//
//
//    @Bean
//    public ApplicationRunner inspectRabbitAdmin(
//            AmqpAdmin amqpAdmin,
//            ConnectionFactory connectionFactory) {
//
//        return args -> {
//            System.out.println("=== RABBITMQ ADMIN CHECK ===");
//
//            System.out.println("Admin class: " + amqpAdmin.getClass().getName());
//
//            System.out.println(
//                    "Connection factory class: " +
//                            connectionFactory.getClass().getName()
//            );
//
//            System.out.println(
//                    "Declared exchanges: " +
//                            amqpAdmin.getQueueInfo("notification.delivery.queue")
//            );
//
//            System.out.println("============================");
//        };
//    }

    @Bean
    public ApplicationRunner inspectRabbitTopology(ApplicationContext context, AmqpAdmin amqpAdmin) {

        return args -> {
            System.out.println("=== RABBITMQ CONFIGURATION CHECK ===");

            System.out.println("Queues:");
            context.getBeansOfType(Queue.class)
                    .forEach((name, queue) ->
                            System.out.println(name + " -> " + queue.getName()));

            System.out.println("Exchanges:");
            context.getBeansOfType(Exchange.class)
                    .forEach((name, exchange) ->
                            System.out.println(name + " -> " + exchange.getName()));

            System.out.println("Bindings:");
            context.getBeansOfType(Binding.class)
                    .forEach((name, binding) ->
                            System.out.println(name + " -> " + binding));

            System.out.println("AmqpAdmin: " + amqpAdmin.getClass().getName());
            System.out.println("====================================");


            try {
                amqpAdmin.initialize();
                System.out.println("RabbitMQ topology initialization successful!");
            } catch (Exception exception) {
                System.err.println("RabbitMQ topology initialization failed!");
                exception.printStackTrace();
            }
        };
    }
}
