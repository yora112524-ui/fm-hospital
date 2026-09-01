package com.hospital.hms.config.mq;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DirectConfig {


    /**
     * 在rabbitMQ上创建consultationReport Topic
     * */
    @Bean
    public Queue prescriptionQueue() {

        return new Queue("consultationReport");
    }
}
