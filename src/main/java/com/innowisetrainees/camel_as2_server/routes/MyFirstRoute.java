package com.innowisetrainees.camel_as2_server.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class MyFirstRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("timer:hello?period=5000").log("Hello from Camel! 🐪 Время: ${date:now:yyyy-MM-dd HH:mm:ss}");
    }
}
