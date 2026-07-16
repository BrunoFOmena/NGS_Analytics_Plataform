package com.ngs.analytics.analytics;

import com.ngs.analytics.common.NgsProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "ngs.queue", name = "enabled", havingValue = "true")
public class RabbitConfig {

    @Bean
    DirectExchange analysisExchange(NgsProperties properties) {
        return new DirectExchange(properties.getQueue().getExchange(), true, false);
    }

    @Bean
    Queue analysisQueue(NgsProperties properties) {
        return new Queue(properties.getQueue().getQueue(), true);
    }

    @Bean
    Binding analysisBinding(Queue analysisQueue, DirectExchange analysisExchange, NgsProperties properties) {
        return BindingBuilder.bind(analysisQueue).to(analysisExchange).with(properties.getQueue().getRoutingKey());
    }

    @Bean
    MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
