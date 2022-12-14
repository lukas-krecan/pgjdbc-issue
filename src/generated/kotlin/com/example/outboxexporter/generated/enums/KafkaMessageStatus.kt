/*
 * This file is generated by jOOQ.
 */
package com.example.outboxexporter.generated.enums


import com.example.outboxexporter.generated.Public

import org.jooq.Catalog
import org.jooq.EnumType
import org.jooq.Schema


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
enum class KafkaMessageStatus(@get:JvmName("literal") public val literal: String) : EnumType {
    PENDING("PENDING"),
    SENT("SENT"),
    FAILED("FAILED");
    override fun getCatalog(): Catalog? = schema.catalog
    override fun getSchema(): Schema = Public.PUBLIC
    override fun getName(): String = "kafka_message_status"
    override fun getLiteral(): String = literal
}
