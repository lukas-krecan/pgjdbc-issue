package com.example.outboxexporter.exporter

import com.fasterxml.jackson.databind.ObjectMapper
import com.example.outboxexporter.generated.enums.KafkaMessageStatus
import com.example.outboxexporter.generated.enums.KafkaMessageStatus.PENDING
import com.example.outboxexporter.generated.tables.references.KAFKA_OUTBOXES
import liquibase.repackaged.org.apache.commons.lang3.RandomStringUtils
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.JSONB.jsonb
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import java.time.Instant
import java.time.Instant.now

class OutboxRepositoryTest(
    @Autowired private val outboxRepository: OutboxRepository,
    @Autowired private val context: DSLContext,
) : AbstractRepositoryTest() {

    @Test
    fun `Should list pending messages`() {
        repeat(21) {
            insertRecord(it.toString(), PENDING)
            logger.info { "inserted $it" }
        }
        outboxRepository.getNextPendingBatch(100)
    }

    private val payload = """{"space_id": 1, "operation": "update", "attributes": {"name": "Save personalised filters", "type": "feature", "effort": null, "archived": false, "html_url": "https://test.com", "owner_id": 1, "timeframe": {"end_date": null, "start_date": null, "granularity": null}, "created_at": "2019-11-13T10:59:16.11203+00:00", "updated_at": "2022-09-26T11:26:24.331934+00:00", "description": "${RandomStringUtils.randomAlphanumeric(1_000_0000)}", "parent_uuid": "00000000-0000-0000-0000-000000000000", "status_uuid": "00000000-0000-0000-0000-000000000000"}, "feature_id": 1, "feature_uuid": "00000000-0000-0000-0000-000000000000", "source_service": null, "updated_attributes": ["updated_at", "parent_id", "parent_rank", "parent_uuid"]}"""

    private fun insertRecord(key: String, status: KafkaMessageStatus, createdAt: Instant = now()) {
        context.insertInto(KAFKA_OUTBOXES)
            .set(KAFKA_OUTBOXES.SUBJECT, "Subject")
            .set(KAFKA_OUTBOXES.TOPIC, "Topic")
            .set(KAFKA_OUTBOXES.KEY, key)
            .set(KAFKA_OUTBOXES.STATUS, status)
            .set(KAFKA_OUTBOXES.SCHEMA_VERSION, 0)
            .set(KAFKA_OUTBOXES.PAYLOAD, jsonb(payload))
            .set(KAFKA_OUTBOXES.HEADERS, jsonb("""{"X-Role": "admin", "X-User-Id": "2", "X-Producer": "pb-backend", "X-Space-Id": "1", "X-Request-Id": "00000000-0000-0000-0000-000000000000", "X-Space-Stage": "active_contract", "X-Original-Producer": "pb-backend"}"""))
            .set(KAFKA_OUTBOXES.ENCODED_MESSAGE_BYTESIZE, 0)
            .set(KAFKA_OUTBOXES.CREATED_AT, createdAt.inUtc())
            .execute()
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        fun objectMapper() = ObjectMapper()
    }

    companion object : KLogging()
}
