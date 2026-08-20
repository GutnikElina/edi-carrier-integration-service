package com.innowisetrainees.camel_as2_server.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class RestRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {

        rest("/api/camel")
                .get("/hello")
                .to("direct:hello")          // Отправляем запрос в отдельный маршрут

                .post("/echo")
                .to("direct:echo");

        from("direct:hello")
                .log("GET /hello called")
                .setBody(simple("Hello from Camel 4.8.7 on Spring Boot!"));

        from("direct:echo")
                .log("POST /echo: ${body}")
                .setBody(simple("Echo: ${body}"));

    }
}
