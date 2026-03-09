package com.danil.library.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "Library API is running over HTTPS (student: 1БИБ23354)";
    }
}
