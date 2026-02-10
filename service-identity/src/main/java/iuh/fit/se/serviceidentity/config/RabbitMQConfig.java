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

    /**
     * Creates the TopicExchange used for routing notification messages.
     *
     * @return the TopicExchange named "notification_exchange"
     */
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    /**
     * Declares the dead-letter exchange used to route dead-lettered messages.
     *
     * @return the DirectExchange named "notification_dlx_exchange" used as the dead-letter exchange
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    /**
     * Creates the durable email queue configured with a dead-letter exchange, dead-letter routing key, and message TTL.
     *
     * The queue is named by EMAIL_QUEUE and is configured to route expired or rejected messages to DLX_EXCHANGE
     * using EMAIL_DLQ as the dead-letter routing key, with a message time-to-live of MESSAGE_TTL milliseconds.
     *
     * @return the configured durable Queue instance for email notifications
     */
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", EMAIL_DLQ)
                .withArgument("x-message-ttl", MESSAGE_TTL)
                .build();
    }

    /**
     * Declares the durable dead-letter queue for email notifications.
     *
     * @return the durable Queue named "notification_email_dlq" used as the email dead-letter queue
     */
    @Bean
    public Queue emailDLQ() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    /**
     * Create a binding that connects the email queue to the notification exchange using the email routing key.
     *
     * @return the Binding that routes messages from the notification exchange to the email queue using `EMAIL_ROUTING_KEY`
     */
    @Bean
    public Binding bindingEmail(Queue emailQueue, TopicExchange exchange) {
        return BindingBuilder.bind(emailQueue).to(exchange).with(EMAIL_ROUTING_KEY);
    }

    /**
     * Registers a binding that routes messages from the dead-letter exchange to the email dead-letter queue.
     *
     * @param emailDLQ           the queue that will receive dead-lettered email messages
     * @param deadLetterExchange the dead-letter exchange used for routing dead-lettered messages
     * @return                   the binding that connects the dead-letter exchange to the email DLQ using the EMAIL_DLQ routing key
     */
    @Bean
    public Binding bindingEmailDLQ(Queue emailDLQ, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(emailDLQ).to(deadLetterExchange).with(EMAIL_DLQ);
    }

    /**
     * Creates a Jackson-based JSON MessageConverter for RabbitMQ message payloads.
     *
     * @return a MessageConverter that serializes and deserializes Java objects to/from JSON using Jackson
     */
    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Create an AMQP template backed by the given connection factory and configured for JSON message conversion.
     *
     * @return an `AmqpTemplate` (a `RabbitTemplate`) configured to use the Jackson JSON message converter for payload serialization
     */
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }

    /**
     * Creates the topic exchange used for internal messages and routing.
     *
     * @return the TopicExchange named "internal_exchange"
     */
    @Bean
    public TopicExchange internalExchange() {
        return new TopicExchange(INTERNAL_EXCHANGE);
    }

    /**
     * Declares the identity upgrade queue used for internal upgrade messages.
     *
     * @return the Queue named "identity_user_upgrade_queue"
     */
    @Bean
    public Queue identityUpgradeQueue() {
        return new Queue(IDENTITY_UPGRADE_QUEUE);
    }

    /**
     * Binds the identity upgrade queue to the internal topic exchange using the user-upgrade routing key.
     *
     * @return a {@link Binding} that connects the identity upgrade queue to the internal exchange with routing key {@code internal.user.upgrade}
     */
    @Bean
    public Binding bindingUpgrade(Queue identityUpgradeQueue, TopicExchange internalExchange) {
        return BindingBuilder.bind(identityUpgradeQueue).to(internalExchange).with(USER_UPGRADE_ROUTING_KEY);
    }
}