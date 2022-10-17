package com.example.outboxexporter.exporter

import com.example.outboxexporter.generated.enums.KafkaMessageStatus
import java.time.LocalDateTime

/**
 * This is intentionally trivial as not to encounter any deserialization issues
 */
data class OutboxRecord(
    val id: Long,
    val topic: String,
    val subject: String,
    val key: String?,
    val partitionKey: String?,
    val payload: String,
    val schemaId: Int,
    val headers: String,
    val status: KafkaMessageStatus?,
    val createdAt: LocalDateTime
)
