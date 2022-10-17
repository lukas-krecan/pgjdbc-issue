package com.example.outboxexporter.exporter

import com.example.outboxexporter.DB_TRANSACTION_MANANGER
import com.example.outboxexporter.DbConfiguration
import com.example.outboxexporter.Profiles.TEST
import com.example.outboxexporter.generated.tables.references.KAFKA_OUTBOXES
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Repository
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@JdbcTest(includeFilters = [ComponentScan.Filter(type = FilterType.ANNOTATION, classes = [Repository::class])])
@Import(DbConfiguration::class)
@Transactional(transactionManager = DB_TRANSACTION_MANANGER)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles(TEST)
@Suppress("UnnecessaryAbstractClass")
abstract class AbstractRepositoryTest {
    @Autowired
    private lateinit var context: DSLContext

    @BeforeEach
    fun cleanOutbox() {
        context.deleteFrom(KAFKA_OUTBOXES).execute()
    }
}
