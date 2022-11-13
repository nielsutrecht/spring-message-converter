package com.nibado.example.jsonl;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class QuoteService {
    private final RestTemplate template;

    private Quotes quotes;

    public QuoteService(RestTemplateBuilder templateBuilder) {
        this.template = templateBuilder.build();
    }

    public Quotes getList() {
        if(quotes == null) {
            quotes = template.getForEntity("/quotes?limit=10", Quotes.class)
                .getBody();
        }
        return quotes;
    }

}
