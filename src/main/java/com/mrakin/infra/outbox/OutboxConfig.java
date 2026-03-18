package com.mrakin.infra.outbox;

import com.gruelbox.transactionoutbox.Dialect;
import com.gruelbox.transactionoutbox.TransactionOutbox;
import com.gruelbox.transactionoutbox.Persistor;
import com.gruelbox.transactionoutbox.spring.SpringInstantiator;
import com.gruelbox.transactionoutbox.spring.SpringTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({SpringTransactionManager.class, SpringInstantiator.class})
public class OutboxConfig {

    @Bean
    public TransactionOutbox transactionOutbox(SpringTransactionManager transactionManager, 
                                               SpringInstantiator instantiator) {
        return TransactionOutbox.builder()
                .transactionManager(transactionManager)
                .instantiator(instantiator)
                .persistor(Persistor.forDialect(Dialect.POSTGRESQL_9))
                .build();
    }
}
