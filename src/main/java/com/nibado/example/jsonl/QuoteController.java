package com.nibado.example.jsonl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static com.nibado.example.jsonl.JsonlMessageConverter.JSONL_MEDIA_TYPE;
import static com.nibado.example.jsonl.JsonlMessageConverter.JSONL_MEDIA_TYPE_VALUE;

@RestController
@RequestMapping("/quote")
public class QuoteController {
    private final QuoteService service;
    private final ObjectMapper mapper;

    public QuoteController(QuoteService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping()
    public Quotes getQuoteList() {
        return service.getList();
    }

    @GetMapping("/ex1")
    public void getQuotesEx1(HttpServletResponse response) throws IOException {
        var quotes = service.getList().results();

        response.setContentType(JSONL_MEDIA_TYPE.toString());

        try (var outs = response.getOutputStream()) {
            JsonlMessageConverter.writeValues(mapper, quotes, outs);
        }
    }

    @GetMapping("/ex2")
    public ResponseEntity<StreamingResponseBody> getQuotesEx2() {
        var quotes = service.getList().results();

        var headers = new HttpHeaders();
        headers.setContentType(JSONL_MEDIA_TYPE);

        StreamingResponseBody stream = outs -> JsonlMessageConverter.writeValues(mapper, quotes, outs);

        return new ResponseEntity<>(stream, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/ex3", produces = JSONL_MEDIA_TYPE_VALUE)
    public List<Quote> getQuotesEx3() {
        return service.getList().results();
    }
}
