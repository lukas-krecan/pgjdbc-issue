package com.example.outboxexporter.exporter

import com.example.outboxexporter.generated.enums.KafkaMessageStatus
import com.example.outboxexporter.generated.enums.KafkaMessageStatus.PENDING
import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.postgresql.util.PSQLException
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import com.example.outboxexporter.generated.tables.KafkaOutboxes.Companion.KAFKA_OUTBOXES as OUTBOX

@Repository
class OutboxRepository(private val context: DSLContext) {
    fun getNextPendingBatch(batchSize: Int): List<OutboxRecord> =
        getNextBatch(batchSize, PENDING)

    fun getNextBatch(batchSize: Int, status: KafkaMessageStatus): List<OutboxRecord> {
        return context.selectFrom(OUTBOX)
            .where(OUTBOX.STATUS.eq(status))
            .orderBy(OUTBOX.ID)
            .limit(batchSize)
            .fetch {
                OutboxRecord(
                    id = it[OUTBOX.ID]!!,
                    topic = it[OUTBOX.TOPIC]!!,
                    subject = it[OUTBOX.SUBJECT]!!,
                    key = it[OUTBOX.KEY],
                    partitionKey = it[OUTBOX.PARTITION_KEY],
                    payload = it[OUTBOX.PAYLOAD]!!.data(),
                    schemaId = it[OUTBOX.SCHEMA_VERSION]!!,
                    headers = it[OUTBOX.HEADERS]!!.data(),
                    status = it[OUTBOX.STATUS],
                    createdAt = it[OUTBOX.CREATED_AT]!!
                )
            }

    }
}

fun Instant.inUtc(): LocalDateTime = atOffset(UTC).toLocalDateTime()
