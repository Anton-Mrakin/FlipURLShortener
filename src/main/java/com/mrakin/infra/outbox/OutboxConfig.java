package com.mrakin.infra.outbox;

import com.gruelbox.transactionoutbox.Dialect;
import com.gruelbox.transactionoutbox.TransactionOutbox;
import com.gruelbox.transactionoutbox.Persistor;
import com.gruelbox.transactionoutbox.Submitter;
import com.gruelbox.transactionoutbox.spring.SpringInstantiator;
import com.gruelbox.transactionoutbox.spring.SpringTransactionManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@Import({SpringTransactionManager.class, SpringInstantiator.class})
public class OutboxConfig {

    private TransactionOutbox outbox;
    private ThreadPoolTaskExecutor outboxExecutor;

    @Bean(name = "outboxExecutor")
    public ThreadPoolTaskExecutor outboxExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("outbox-worker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(2);
        executor.initialize();
        this.outboxExecutor = executor;
        return executor;
    }

    @Bean
    public TransactionOutbox transactionOutbox(SpringTransactionManager transactionManager,
                                               SpringInstantiator instantiator,
                                               ThreadPoolTaskExecutor outboxExecutor) {
        Submitter submitter = Submitter.withExecutor(outboxExecutor.getThreadPoolExecutor());

        outbox = TransactionOutbox.builder()
                .transactionManager(transactionManager)
                .instantiator(instantiator)
                .persistor(Persistor.forDialect(Dialect.POSTGRESQL_9))
                .blockAfterAttempts(2)
                .attemptFrequency(Duration.ofSeconds(1))
                .submitter(submitter)
                .build();
        return outbox;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down TransactionOutbox");
        if (outboxExecutor != null) {
            log.info("Stopping outbox executor immediately");
            outboxExecutor.shutdown();
        }
        // Don't flush - it will try to access DB which may be already stopped in tests
        // In production, pending items will be processed on next startup
        log.info("TransactionOutbox shutdown complete (pending items will be processed on next startup)");
    }
}
