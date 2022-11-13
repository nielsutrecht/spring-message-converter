package com.nibado.example.jsonl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

//{"_id":"qho6kC7InWuX","content":"They can conquer who believe they can.","author":"Virgil","tags":["famous-quotes"],"authorSlug":"virgil","length":38,"dateAdded":"2020-06-24","dateModified":"2020-06-24"}
public record Quote(
    @JsonProperty("_id") String id,
    String author,
    String content,
    List<String> tags,
    String authorSlug,
    LocalDate dateAdded,
    LocalDate dateModified) {
}
