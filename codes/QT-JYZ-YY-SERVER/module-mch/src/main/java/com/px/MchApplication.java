package com.px;

import com.px.web.WebApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MchApplication extends WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(MchApplication.class, args);
    }
}