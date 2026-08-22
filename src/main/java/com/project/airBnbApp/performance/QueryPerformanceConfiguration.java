package com.project.airBnbApp.performance;

import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(
        name = "performance.query-profiling.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class QueryPerformanceConfiguration {

    @Bean
    public QueryPerformanceListener queryPerformanceListener() {
        return new QueryPerformanceListener();
    }

    @Bean
    public BeanPostProcessor dataSourcePerformanceProxy(QueryPerformanceListener listener) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (!(bean instanceof DataSource dataSource)) {
                    return bean;
                }

                // Avoid wrapping our own proxy again if the context is refreshed.
                if (bean.getClass().getName().contains("ProxyDataSource")) {
                    return bean;
                }

                return ProxyDataSourceBuilder
                        .create(dataSource)
                        .name("AirBnbDataSource")
                        .listener(listener)
                        .buildProxy();
            }
        };
    }

    @Bean
    public QueryPerformanceFilter queryPerformanceFilter() {
        return new QueryPerformanceFilter();
    }
}
