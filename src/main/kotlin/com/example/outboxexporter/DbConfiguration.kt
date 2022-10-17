package com.example.outboxexporter

import org.jooq.ConnectionProvider
import org.jooq.SQLDialect
import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultDSLContext
import org.jooq.impl.DefaultExecuteListenerProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jooq.JooqExceptionTranslator
import org.springframework.boot.autoconfigure.jooq.SpringTransactionProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

// We can't use the autoconfiguration as there is a clash between transaction managers
@Configuration
class DbConfiguration {
    @Bean
    fun dataSourceConnectionProvider(dataSource: DataSource): DataSourceConnectionProvider {
        return DataSourceConnectionProvider(TransactionAwareDataSourceProxy(dataSource))
    }

    @Bean
    @Order(0)
    fun jooqExceptionTranslatorExecuteListenerProvider(): DefaultExecuteListenerProvider {
        return DefaultExecuteListenerProvider(JooqExceptionTranslator())
    }

    @Bean
    @Qualifier(DB_TRANSACTION_MANANGER)
    fun transactionManager(dataSource: DataSource): DataSourceTransactionManager {
        return DataSourceTransactionManager(dataSource)
    }

    @Bean
    fun transactionProvider(@Qualifier(DB_TRANSACTION_MANANGER) transactionManager: PlatformTransactionManager): SpringTransactionProvider {
        return SpringTransactionProvider(transactionManager)
    }

    @Bean
    fun dsl(connectionProvider: ConnectionProvider): DefaultDSLContext {
        return DefaultDSLContext(connectionProvider, SQLDialect.POSTGRES)
    }
}

const val DB_TRANSACTION_MANANGER = "dbTransactionManager"
