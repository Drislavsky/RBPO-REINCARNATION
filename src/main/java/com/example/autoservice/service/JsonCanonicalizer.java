package com.example.autoservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;

@Service
public class JsonCanonicalizer {
    private final ObjectMapper mapper;

    public JsonCanonicalizer() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public byte[] canonicalize(Object payload) throws Exception {
        String rawJson = mapper.writeValueAsString(payload);
        Object obj = mapper.readValue(rawJson, Object.class);
        return mapper.writeValueAsString(obj).getBytes(StandardCharsets.UTF_8);
    }
}