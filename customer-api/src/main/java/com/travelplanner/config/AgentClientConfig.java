package com.travelplanner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

@Configuration
public class AgentClientConfig {

    /**
     * The Python agent service speaks snake_case JSON while the customer API
     * stays camelCase, so this client gets its own ObjectMapper. Timeouts come
     * from {@code spring.http.client.*}.
     */
    @Bean
    RestClient agentRestClient(RestClient.Builder builder, ObjectMapper objectMapper, AgentServiceProperties props) {
        ObjectMapper snakeCase = objectMapper.copy()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return builder
                .baseUrl(props.baseUrl())
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(new MappingJackson2HttpMessageConverter(snakeCase));
                })
                .build();
    }
}
