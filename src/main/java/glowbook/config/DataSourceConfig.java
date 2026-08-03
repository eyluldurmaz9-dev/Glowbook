package glowbook.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource(Environment environment) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(required(environment, "spring.datasource.url"));
        dataSource.setUsername(cleanUsername(environment.getProperty("spring.datasource.username", "")));
        dataSource.setPassword(environment.getProperty("spring.datasource.password", ""));

        String driverClassName = environment.getProperty("spring.datasource.driver-class-name");
        if (StringUtils.hasText(driverClassName)) {
            dataSource.setDriverClassName(driverClassName);
        }

        return dataSource;
    }

    private String required(Environment environment, String key) {
        String value = environment.getProperty(key);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(key + " must be configured");
        }
        return value;
    }

    private String cleanUsername(String username) {
        String value = username == null ? "" : username.trim();
        while (value.startsWith("}") || value.startsWith("{")) {
            value = value.substring(1).trim();
        }
        while (value.endsWith("}") || value.endsWith("{")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        return value;
    }
}
