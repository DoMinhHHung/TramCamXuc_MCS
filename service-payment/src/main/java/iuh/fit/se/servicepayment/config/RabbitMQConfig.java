package iuh.fit.se.servicepayment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String INTERNAL_EXCHANGE = "internal_exchange";

    public static final String USER_UPGRADE_ROUTING_KEY = "internal.user.upgrade";

    /**
     * Declares the internal TopicExchange used for application RabbitMQ messaging.
     *
     * @return the TopicExchange configured with the INTERNAL_EXCHANGE name
     */
    @Bean
    public TopicExchange internalExchange() {
        return new TopicExchange(INTERNAL_EXCHANGE);
    }

    /**
     * Provides a Jackson-based JSON MessageConverter for AMQP message serialization and deserialization.
     *
     * @return a MessageConverter that serializes and deserializes message payloads to and from JSON using Jackson
     */
    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Creates an AmqpTemplate backed by RabbitTemplate and configures it to use the module's JSON message converter.
     *
     * @param connectionFactory the ConnectionFactory used to construct the RabbitTemplate
     * @return an AmqpTemplate (RabbitTemplate) configured to use the Jackson2JsonMessageConverter for message serialization
     */
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
}