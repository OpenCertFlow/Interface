package com.certimakers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CertiMakersApplication {

    public static void main(String[] args) {
        SpringApplication.run(CertiMakersApplication.class, args);
    }
}
