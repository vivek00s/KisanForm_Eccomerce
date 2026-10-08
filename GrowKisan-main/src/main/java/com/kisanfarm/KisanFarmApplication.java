package com.kisanfarm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * KisanFarm - Agricultural E-commerce Marketplace.
 * Single Spring Boot application; also serves the React production build.
 */
@SpringBootApplication
public class KisanFarmApplication {

    public static void main(String[] args) {
        SpringApplication.run(KisanFarmApplication.class, args);
    }
}
