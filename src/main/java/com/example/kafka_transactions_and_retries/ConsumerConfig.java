package com.example.kafka_transactions_and_retries;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class ConsumerConfig {
	@Bean
	public Consumer<String> consumeMessage() {
		return s -> {
			log.info("Consuming {}", s);
			throw new IllegalArgumentException(s);
		};
	}
}
