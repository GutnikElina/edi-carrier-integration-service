package com.innowisetrainees.camel_as2_server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "hello from server";
    }

    @GetMapping("/")
    public String home() {
        return "server is running";
    }
}
