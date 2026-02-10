package iuh.fit.se.servicenotification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_QUEUE = "notification_email_queue";
    public static final String NOTIFICATION_EXCHANGE = "notification_exchange";
    public static final String NOTIFICATION_ROUTING_KEY = "notification_email_key";

    /**
     * Creates the RabbitMQ queue used for notification emails.
     *
     * @return the durable Queue named by {@link #NOTIFICATION_QUEUE}
     */
    @Bean
    public Queue emailQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    /**
     * Declares a TopicExchange for notification message routing.
     *
     * @return the TopicExchange configured with the notification exchange name
     */
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    /**
     * Creates a binding that routes messages from the given topic exchange to the email queue using the notification routing key.
     *
     * @param emailQueue the queue that will receive routed messages
     * @param exchange the topic exchange used to route messages
     * @return a Binding that connects the provided queue to the provided exchange using {@code NOTIFICATION_ROUTING_KEY}
     */
    @Bean
    public Binding binding(Queue emailQueue, TopicExchange exchange) {
        return BindingBuilder.bind(emailQueue).to(exchange).with(NOTIFICATION_ROUTING_KEY);
    }

    /**
     * Create a Jackson-based JSON MessageConverter for AMQP message serialization.
     *
     * @return a MessageConverter that serializes and deserializes message bodies to and from JSON using Jackson
     */
    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Creates an AMQP template backed by a RabbitTemplate configured to use the JSON message converter.
     *
     * @return an {@link AmqpTemplate} (RabbitTemplate) whose message converter is set to the application's Jackson2JsonMessageConverter
     */
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
}