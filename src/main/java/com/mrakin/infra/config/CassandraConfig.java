package com.mrakin.infra.config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.config.SessionBuilderConfigurer;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

@Configuration
@ConditionalOnProperty(name = "cassandra.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Value("${cassandra.contact-points:localhost}")
    private String contactPoints;

    @Value("${cassandra.port:9042}")
    private int port;

    @Value("${cassandra.keyspace:url_shortener}")
    private String keyspace;

    @Value("${cassandra.local-datacenter:datacenter1}")
    private String localDatacenter;

    @Value("${cassandra.shard-count:10}")
    private int shardCount;

    @Override
    protected String getKeyspaceName() {
        return keyspace;
    }

    @Override
    protected String getContactPoints() {
        return contactPoints;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected String getLocalDataCenter() {
        return localDatacenter;
    }

    @Override
    protected List<String> getStartupScripts() {
        // Create keyspace and table in startup scripts
        // Note: Counter tables can only have counter columns and primary key columns
        return List.of(
                "CREATE KEYSPACE IF NOT EXISTS " + keyspace +
                " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}",

                "CREATE TABLE IF NOT EXISTS " + keyspace + ".url_access_by_shard (" +
                "    shard_id int," +
                "    access_day date," +
                "    full_url text," +
                "    access_count counter," +
                "    PRIMARY KEY ((shard_id), access_day, full_url)" +
                ") WITH CLUSTERING ORDER BY (access_day DESC, full_url ASC)"
        );
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.NONE; // We handle schema creation manually
    }

    @Bean
    public int cassandraShardCount() {
        return shardCount;
    }

    @Bean
    public SessionBuilderConfigurer sessionBuilderConfigurer() {
        return sessionBuilder -> sessionBuilder
                .withLocalDatacenter(localDatacenter);
    }
}
