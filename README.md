# Kafka Transactions and Retries with Spring Cloud Stream

This repository explores solutions to the issue of ensuring proper retry mechanisms with Kafka transactions in Spring Cloud Stream. It addresses [Spring Cloud Stream Issue #3066](https://github.com/spring-cloud/spring-cloud-stream/issues/3066).

## Problem
The issue, as described in [#3066](https://github.com/spring-cloud/spring-cloud-stream/issues/3066), involves a challenge where:
- Messages may not be properly redelivered after retries when using Kafka transactions.
- Message processing consistency can be compromised due to transactional nuances.

## Attempts to solve
This repository contains a few attempts to solve this problem is implemented in the file [`ConsumerConfig.java`](https://github.com/thibaudledent/kafka-transactions-and-retries/blob/main/src/main/java/com/example/kafka_transactions_and_retries/ConsumerConfig.java).

## Best solution so far
The best solution currently relies on insights from [this comment](https://github.com/spring-cloud/spring-cloud-stream/issues/3066#issuecomment-2614606317).
