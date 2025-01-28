package com.example.kafka_transactions_and_retries;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TestConsumerService {

	public void consumeTestMessage(String message) {
		log.info("Attempt to consume message at {}: {}", System.currentTimeMillis(), message);
		if (message.contains("retry")) {
			throw new UnsupportedOperationException("This should be retried according to config");
		} else {
			throw new IllegalArgumentException("This should not be retried");
		}
	}

