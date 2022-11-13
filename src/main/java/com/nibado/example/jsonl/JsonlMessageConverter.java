package com.nibado.example.jsonl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

@Component
public class JsonlMessageConverter implements HttpMessageConverter<Collection<Object>> {
    private final ObjectMapper mapper;

    public JsonlMessageConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public static final String JSONL_MEDIA_TYPE_VALUE = "application/x-jsonlines";
    public static final MediaType JSONL_MEDIA_TYPE = MediaType.parseMediaType(JSONL_MEDIA_TYPE_VALUE);

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return Collection.class.isAssignableFrom(clazz) && mediaType.equals(JSONL_MEDIA_TYPE);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return List.of(JSONL_MEDIA_TYPE);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
        return getSupportedMediaTypes();
    }

    @Override
    public Collection<Object> read(Class<? extends Collection<Object>> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        throw new RuntimeException("Reading not supported");
    }

    @Override
    public void write(Collection<Object> values, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        outputMessage.getHeaders().setContentType(JSONL_MEDIA_TYPE);
        try (var outs = outputMessage.getBody()) {
            writeValues(mapper, values, outs);
        }
    }

    public static void writeValues(ObjectMapper mapper, Collection<?> values, OutputStream outs) throws IOException {
        var writer = mapper.writer()
            .without(SerializationFeature.INDENT_OUTPUT)
            .without(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

        var newLine = "\n".getBytes(StandardCharsets.UTF_8);

        for (var value : values) {
            writer.writeValue(outs, value);
            outs.write(newLine);
        }
    }
}
