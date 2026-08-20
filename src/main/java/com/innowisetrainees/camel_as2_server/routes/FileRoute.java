package com.innowisetrainees.camel_as2_server.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class FileRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("file:data/inbox?delete=false")
                .routeId("file-route")
                .log("Получен файл: ${file:name}, размер: ${file:size} байт")
                .to("file:data/processed")
                .log("Файл ${file:name} перемещен в папку processed");
    }
}
