package org.aivan.ratserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringBootConfig {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}