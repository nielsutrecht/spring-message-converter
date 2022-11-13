# Spring JSON Lines / Custom Media Type example

In this piece of example code I'm going to demonstrate a number of options that Spring gives you to implement your own custom
mediatypes. The Application returns a list of Quotes in [JSON Lines](https://jsonlines.org/) format it gets from the
[Quotable API](https://github.com/lukePeavey/quotable).

## JSON Lines

### Why JSON lines

The typical way to handle REST responses with arrays is to have them inside an 'envelope' objects, for example:

    {
        "results": [
            {
                "_id": "JIk3cbQ8s",
                "author": "Michael Jordan",
                "content": "I've missed more than 9000 shots in my care[...]",
                "tags": [
                    "sports",
                    "competition",
                 "famous-quotes"
                ]
            },
            [...]
        ]
    }

The main reason to never use arrays at the top level in JSON responses is because it makes it harder to extend the API,
for example if you want to also include a "next-page" element to support paging. Moving from having an array as root element
to an object, is a breaking change.

The downside of this approach is that you will need to parse the entire structure to 'databind' them into a corresponding
object tree. Not an issue for a few thousand objects, but when you're dealing with millions, it can cause memory issues.

### Writing JSON Lines

JSON Lines is, as the name explains, a format where you read and write individual messages serialized as JSON line by line.
This can be beneficial for very large datasets since you can now both read and write objects on a line per line basis.
Doing this in Spring is relatively straightforward; we'll just use the provided ObjectMapper with some changes, as demonstrated
in JsonlMessageConverter.writeValues:

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

This method iterates over the provided collections of values and writes them one by one to the provided OutputStream,
separating them with newline in between. Two things to note here are that we first forcefully disable output indentation.
While Spring does not have this on by default, someone could configure their service this way breaking the format.

The second thing of note is that we disable the writer automatically closing the output stream after writing a value,
which is enabled by default.

This method can now be used to write JSON lines to any output stream; files, S3 objects or, as I'm going to demonstrate,
REST responses.

## JSONL REST responses

### Mime Type

Unfortunately JSONL doesn't have a 'standard' Mime Type yet. The de facto standard convention seems to have settled on
`application/x-jsonlines` for now, so that is what I'm going to use here. You can use your own obviously, but it's best
not to use an existing one like `text/plain` or `application/json`.

### Direct Response Writes

One of the most flexible ways to do 'non-standard' things in Spring Rest Controllers is to just write to the
HttpServletResponse yourself. It exposes both the response headers and output stream for you. Creating such a method
is relatively simple:

    @GetMapping("/ex1")
    public void getQuotesEx1(HttpServletResponse response) throws IOException {
        var quotes = service.getList().results();

        response.setContentType(JSONL_MEDIA_TYPE.toString());

        try (var outs = response.getOutputStream()) {
            JsonlMessageConverter.writeValues(mapper, quotes, outs);
        }
    }

On the response we set the content type to `application/x-jsonlines` and then just call the `writeValues` method that
was shown before. We just have to make sure to close the output stream. This is very similar to how you would implement a
file download in a controller, as [demonstrated in a previous blog post](https://niels.nu/blog/2022/spring-file-upload-download).

### Using StreamingResponseBody

    @GetMapping("/ex2")
    public ResponseEntity<StreamingResponseBody> getQuotesEx2() {
        var quotes = service.getList().results();

        var headers = new HttpHeaders();
        headers.setContentType(JSONL_MEDIA_TYPE);

        StreamingResponseBody stream = outs -> JsonlMessageConverter.writeValues(mapper, quotes, outs);

        return new ResponseEntity<>(stream, headers, HttpStatus.OK);
    }

Another *very* similar approach is to return a StreamingResponseBody. This is a Functional interface provided by Spring
that provides you with an OutputStream to write to. That is all StreamingResponseBody contains, so if you want to do things
like set a content type header, you're still going to have to wrap it in a ResponseEntity, as demonstrated above.

### Using a custom MessageConvertor

And in the last example I'm going to show a more reusable approach. Our controller method now looks like this:

    @GetMapping(value = "/ex3", produces = JSONL_MEDIA_TYPE_VALUE)
    public List<Quote> getQuotesEx3() {
        return service.getList().results();
    }

Extremely simple! The only thing different from a normal JSON response is that we specifically specify that it 'produces'
our own Media Type. So how does Spring then knows how to handle this? For this we need to implement our own Message Converter:

    @Component
    public class JsonlMessageConverter implements HttpMessageConverter<Collection<Object>> {

        @Override
        public boolean canWrite(Class<?> clazz, MediaType mediaType) {
            return Collection.class.isAssignableFrom(clazz) && mediaType.equals(JSONL_MEDIA_TYPE);
        }

        @Override
        public void write(Collection<Object> values, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
            outputMessage.getHeaders().setContentType(JSONL_MEDIA_TYPE);
            try (var outs = outputMessage.getBody()) {
                writeValues(mapper, values, outs);
            }
        }
    }

You can see the entire class [here](), but I'll go over the most important bits, the 'canWrite' and 'write' methods.

Since this is a Spring @Component it gets picked up by Spring automatically. There is unfortunately a lot of older outdated
information on how to register Spring components, but just know that generally *either* configuring a @Bean *OR* marking it
as a @Component is enough. You also don't have to manually add a message convertor (or most other middleware); Spring 'knows'
what it is because it implements HttpMessageConverter.

The 'canWrite' method gets called for every object Spring thinks it needs to be able to serialize that's not picked up by
another converter yet. I check whether it's a collection type (I need to be able to iterate over it), and whether the MediaType is
the defined `application/x-jsonlines`. If either are not true I answer 'false' and Spring won't call this message convertor to
convert the message.

The 'write' message is very straightforward; it sets the media/content type and then calls the method I've shown earlier
to just write out the values as individual JSON lines.

Reading these types I have disabled / not implemented in this message convertor, so it's write-only.

## Testing

If we start the service and call the /quote/exN end-points, we should always see the same result:

    $ curl -v http://localhost:8080/quote/ex3
    * Trying ::1:8080...
    * Connected to localhost (::1) port 8080 (#0)
    > GET /quote/ex3 HTTP/1.1
    > Host: localhost:8080
    > User-Agent: curl/7.77.0
    > Accept: */*
    >
    * Mark bundle as not supporting multiuse
    < HTTP/1.1 200
    < Content-Type: application/x-jsonlines
    < Transfer-Encoding: chunked
    < Date: Sun, 13 Nov 2022 09:37:33 GMT
    <
    {"_id":"JIk3cbQ8s","author":"Michael Jordan","content":"I've missed more tha [...]"}
    {"_id":"ch-0pti9X6U","author":"Joe Adcock","content":"Trying to sneak a fastball [...]"}
    {"_id":"MsGmNTCtAXd","author":"Mike Singletary (basketball)","content":"Do you know [...]"}
    * Connection #0 to host localhost left intact

We see `application/x-jsonlines` as our content type and also see Spring sets `Transfer-Encoding: chunked` because we don't
know up front how large the response is going to be. And in the body we see every quote object (from which I cut out quite
a bit of data) neatly on their own line.

# Conclusion

In this post I've shown you three ways to do 'custom' output in Spring. So, which one should you use? Well, that's up to
you! :) For a single method in a controller, I would probably favor either the first or second method, because it's
straightforward to see what happens. If I however would have to support multiple end-points with custom Media Types, I
would implement a custom convertor so I only have to write the code and it's tests once. That said; by extracting the
write method the complexity is low and you reuse most of the code already. So; it mostly depends on your personal
preferences!
