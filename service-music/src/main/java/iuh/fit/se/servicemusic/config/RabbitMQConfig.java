package iuh.fit.se.servicemusic.config;

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

    public static final String DLX_EXCHANGE = "music_dlx_exchange";
    public static final String TRANSCODE_DLQ = "transcode_dlq";
    public static final String RESULT_DLQ = "transcode_result_dlq";

    private static final int MESSAGE_TTL = 3600000;
    private static final int MAX_RETRY_COUNT = 3;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue transcodeQueue() {
        return QueueBuilder.durable(TRANSCODE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TRANSCODE_DLQ)
                .withArgument("x-message-ttl", MESSAGE_TTL)
                .build();
    }

    @Bean
    public Queue resultQueue() {
        return QueueBuilder.durable(RESULT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RESULT_DLQ)
                .withArgument("x-message-ttl", MESSAGE_TTL)
                .build();
    }

    @Bean
    public Queue transcodeDLQ() {
        return QueueBuilder.durable(TRANSCODE_DLQ)
                .build();
    }

    @Bean
    public Queue resultDLQ() {
        return QueueBuilder.durable(RESULT_DLQ)
                .build();
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
    public Binding bindingTranscodeDLQ(Queue transcodeDLQ, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(transcodeDLQ).to(deadLetterExchange).with(TRANSCODE_DLQ);
    }

    @Bean
    public Binding bindingResultDLQ(Queue resultDLQ, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(resultDLQ).to(deadLetterExchange).with(RESULT_DLQ);
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