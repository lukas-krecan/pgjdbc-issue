package com.example.outboxexporter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [JooqAutoConfiguration::class])
class OutboxExporterApplication

fun main(args: Array<String>) {

    runApplication<OutboxExporterApplication>(*args)
}
