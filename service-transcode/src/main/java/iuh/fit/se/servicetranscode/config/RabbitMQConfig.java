package iuh.fit.se.servicetranscode.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "music_exchange";
    public static final String TRANSCODE_QUEUE = "transcode_queue";
    public static final String TRANSCODE_ROUTING_KEY = "transcode_key";

    public static final String RESULT_QUEUE = "transcode_result_queue";
    public static final String RESULT_ROUTING_KEY = "transcode_result_key";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue transcodeQueue() {
        return new Queue(TRANSCODE_QUEUE);
    }

    @Bean
    public Queue resultQueue() {
        return new Queue(RESULT_QUEUE);
    }

    @Bean
    public Binding bindingTranscode(Queue transcodeQueue, TopicExchange exchange) {
        return BindingBuilder.bind(transcodeQueue).to(exchange).with(TRANSCODE_ROUTING_KEY);
    }

    @Bean
    public Binding bindingResult(Queue resultQueue, TopicExchange exchange) {
        return BindingBuilder.bind(resultQueue).to(exchange).with(RESULT_ROUTING_KEY);
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
}