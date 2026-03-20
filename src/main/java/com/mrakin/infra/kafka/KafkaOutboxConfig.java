package com.mrakin.infra.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaOutboxConfig {

    @Bean
    public NewTopic urlAccessedTopic() {
        return TopicBuilder.name("url-accessed")
                .partitions(10)
                .replicas(1)
                .build();
    }
}
