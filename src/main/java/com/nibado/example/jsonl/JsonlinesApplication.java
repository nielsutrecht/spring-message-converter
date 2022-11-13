package com.nibado.example.jsonl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class JsonlinesApplication {
    public static void main(String[] args) {
        SpringApplication.run(JsonlinesApplication.class, args);
    }

    @Bean
    public RestTemplateBuilder quoteApiBuilder() {
        return new RestTemplateBuilder()
            .rootUri("https://api.quotable.io/");
    }

}
