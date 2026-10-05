package com.swiftdeliver.backend.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.datatype.hibernate7.Hibernate7Module;

/**
 * Lets Jackson serialise JPA entities without choking on lazy associations (they are written as
 * null instead of throwing). Spring Boot 4 uses Jackson 3 and Hibernate 7, hence these classes.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer hibernate7ModuleCustomizer() {
        return builder -> builder.addModule(new Hibernate7Module());
    }
}
