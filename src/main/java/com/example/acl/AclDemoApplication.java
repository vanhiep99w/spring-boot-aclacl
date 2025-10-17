package com.example.acl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AclDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AclDemoApplication.class, args);
    }
}
