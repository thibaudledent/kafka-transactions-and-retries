package com.example.kafka_transactions_and_retries;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.DefaultAfterRollbackProcessor;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Configuration
@Slf4j
public class ConsumerConfig {
	@Bean
	public Consumer<String> consumeMessage(TestConsumerService service) {
		return service::consumeTestMessage;
	}

	@Bean
	public ListenerContainerCustomizer<AbstractMessageListenerContainer<?, ?>> customizerOkDefaultErrorHandler() {
		log.info("Creating ListenerContainerCustomizer bean");
		return (container, destination, group) -> {
			log.info("Customizing container for destination: {}, group: {}", destination, group);

			container.setCommonErrorHandler(
					new DefaultErrorHandler(
							(record, exception) -> log.error("Record failed after retries: {}, due to {}", record, exception.getMessage()),
							new FixedBackOff(1000L, 5)
					)
			);
			((DefaultErrorHandler) container.getCommonErrorHandler()).addNotRetryableExceptions(IllegalArgumentException.class);
		};
	}

	/**
	 * retried according to config: 3 occurrences
	 * not be retried: 3 occurrences
	 */
	//@Bean
	public ListenerContainerCustomizer<AbstractMessageListenerContainer<?, ?>> customizerKoSameForBoth() {
		log.info("Creating ListenerContainerCustomizer bean");
		return (container, destination, group) -> {
			log.info("Customizing container for destination: {}, group: {}", destination, group);

			container.setAfterRollbackProcessor(
					new DefaultAfterRollbackProcessor<>(
							(record, exception) -> log.error("Failed to process record: {}", record, exception),
							new FixedBackOff(1000L, 1) // 1 maxAttempts -> 3 occurrences of the exception
					)
			);
		};
	}


	/**
	 * retried according to config: forever
	 * not be retried: 3 occurrences
	 */
	//@Bean
	public ListenerContainerCustomizer<AbstractMessageListenerContainer<?, ?>> customizerKoForeverForRetries() {
		log.info("Creating ListenerContainerCustomizer bean");
		return (container, destination, group) -> {
			log.info("Customizing container for destination: {}, group: {}", destination, group);

			container.setAfterRollbackProcessor(
					new DefaultAfterRollbackProcessor<>(
							(record, exception) -> {
								log.error("Record failed after all retries: {}", record, exception);
								Throwable rootCause = getRootCause(exception);
								if (rootCause instanceof IllegalArgumentException) {
									log.warn("Skipping retries for IllegalArgumentException: {}", rootCause.getMessage());
								} else {
									log.warn("Retrying after exception: {}", rootCause.getMessage());
									throw new RuntimeException(exception);
								}
							},
							new FixedBackOff(1000L, 1)
					)
			);
		};
	}

	private static final int MAX_RETRIES = 4;
	private static final ConcurrentHashMap<String, Integer> RETRY_COUNTER_MAP = new ConcurrentHashMap<>();


	/**
	 * retried according to config: 5
	 * not be retried: 1
	 */
	//@Bean
	public ListenerContainerCustomizer<AbstractMessageListenerContainer<?, ?>> customizerOkButWTF() {
		log.info("Creating ListenerContainerCustomizer bean");
		return (container, destination, group) -> {
			log.info("Customizing container for destination: {}, group: {}", destination, group);
			container.setAfterRollbackProcessor(
					new DefaultAfterRollbackProcessor<>(
							(consumerRecord, exception) -> {
								log.error("Failed to process record due to exception: {}", exception.getMessage());

								Throwable rootCause = getRootCause(exception);
								log.info("Root cause is: {}", rootCause != null ? rootCause.getClass().getName() : "null");

								String uniqueKey = consumerRecord.topic() + "_" + consumerRecord.partition() + "_" + consumerRecord.offset();

								if (rootCause instanceof IllegalArgumentException) {
									log.error("Not retrying record with key {} for IllegalArgumentException: {}", uniqueKey, exception.getMessage());
								} else {
									int attempts = RETRY_COUNTER_MAP.compute(uniqueKey, (key, count) -> count == null ? 1 : count + 1);
									log.info("Retry attempt {} for record with key: {} (record: {})", attempts, uniqueKey, consumerRecord);

									if (attempts > MAX_RETRIES) {
										log.error("Max retries ({}) reached for record with key: {}", MAX_RETRIES, uniqueKey);
										RETRY_COUNTER_MAP.remove(uniqueKey);
									} else {
										log.warn("Retrying after exception: record with key: {}, exception: {}", uniqueKey, exception.getMessage());
										throw new RuntimeException(exception); // Allow retry
									}
								}
							},
							new FixedBackOff(0L, 0)) // No retries
			);
		};
	}

	public static Throwable getRootCause(Throwable throwable) {
		List<Throwable> list = getThrowableList(throwable);
		return list.isEmpty() ? null : (Throwable) list.get(list.size() - 1);
	}

	public static List<Throwable> getThrowableList(Throwable throwable) {
		List<Throwable> list;
		for (list = new ArrayList(); throwable != null && !list.contains(throwable); throwable = throwable.getCause()) {
			list.add(throwable);
		}

		return list;
	}

}
