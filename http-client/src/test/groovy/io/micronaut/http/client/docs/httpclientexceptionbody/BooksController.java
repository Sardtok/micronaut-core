package io.micronaut.http.client.docs.httpclientexceptionbody;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import java.util.HashMap;
import java.util.Map;

@Requires(property = "spec.name", value = "BindHttpClientExceptionBodySpec")
//tag::clazz[]
@Controller("/books")
public class BooksController {

    @Get("/{isbn}")
    public HttpResponse find(String isbn) {
        if (isbn.equals("1680502395")) {
            Map<String, Object> m = new HashMap<>();
            m.put("status", 401);
            m.put("error", "Unauthorized");
            m.put("message", "No message available");
            m.put("path", "/books/"+isbn);
            return HttpResponse.status(HttpStatus.UNAUTHORIZED).body(m);

        }
        return HttpResponse.ok(new Book("1491950358", "Building Microservices"));
    }
}
//end::clazz[]
