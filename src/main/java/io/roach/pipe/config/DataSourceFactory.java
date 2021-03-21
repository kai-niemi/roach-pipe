package io.roach.pipe.config;

import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

@Component
public class DataSourceFactory {
    protected final Logger traceLogger = LoggerFactory.getLogger("io.roach.sql_trace");

    @Value("${spring.datasource.hikari.maximum-pool-size}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle}")
    private int minimumIdle;

    public DataSource createDataSource(Map<String, String> allParams) {
        final String url = allParams.get("url");
        final String user = allParams.get("user");
        final String password = allParams.get("password");

        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl(url);
        properties.setUsername(user);
        properties.setPassword(password);

        return createDataSource(properties);
    }

    public DataSource createDataSource(DataSourceProperties properties) {
        HikariDataSource ds = new HikariDataSource();
        ds.setDataSource(properties.initializeDataSourceBuilder().build());
        ds.setAutoCommit(true);
        ds.setMaximumPoolSize(maximumPoolSize);
        ds.setMinimumIdle(minimumIdle);
        ds.setPoolName(properties.getName());
        ds.setConnectionInitSql("select 1");

        return traceLogger.isDebugEnabled() ?
                ProxyDataSourceBuilder
                        .create(ds)
                        .name("SQL")
                        .asJson()
                        .countQuery()
                        .logQueryBySlf4j(SLF4JLogLevel.DEBUG, "io.roach.sql_trace")
                        .build()
                : ds;
    }

    public String databaseVersion(DataSource dataSource) {
        return new JdbcTemplate(dataSource).queryForObject("select version()", String.class);
    }
}
