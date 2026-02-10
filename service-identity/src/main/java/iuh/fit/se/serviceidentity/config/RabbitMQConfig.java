package iuh.fit.se.serviceidentity.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "notification_exchange";

    public static final String EMAIL_QUEUE = "notification_email_queue";
    public static final String EMAIL_ROUTING_KEY = "notification_email_key";

    public static final String DLX_EXCHANGE = "notification_dlx_exchange";
    public static final String EMAIL_DLQ = "notification_email_dlq";

    public static final String INTERNAL_EXCHANGE = "internal_exchange";
    public static final String USER_UPGRADE_ROUTING_KEY = "internal.user.upgrade";
    public static final String IDENTITY_UPGRADE_QUEUE = "identity_user_upgrade_queue";

    private static final int MESSAGE_TTL = 3600000;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", EMAIL_DLQ)
                .withArgument("x-message-ttl", MESSAGE_TTL)
                .build();
    }

    @Bean
    public Queue emailDLQ() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    public Binding bindingEmail(Queue emailQueue, TopicExchange exchange) {
        return BindingBuilder.bind(emailQueue).to(exchange).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding bindingEmailDLQ(Queue emailDLQ, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(emailDLQ).to(deadLetterExchange).with(EMAIL_DLQ);
    }

    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }

    @Bean
    public TopicExchange internalExchange() {
        return new TopicExchange(INTERNAL_EXCHANGE);
    }

    @Bean
    public Queue identityUpgradeQueue() {
        return new Queue(IDENTITY_UPGRADE_QUEUE);
    }

    @Bean
    public Binding bindingUpgrade(Queue identityUpgradeQueue, TopicExchange internalExchange) {
        return BindingBuilder.bind(identityUpgradeQueue).to(internalExchange).with(USER_UPGRADE_ROUTING_KEY);
    }
}