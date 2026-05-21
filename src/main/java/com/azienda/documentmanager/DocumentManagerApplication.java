package com.azienda.documentmanager;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "15m")
@EnableAsync
public class DocumentManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentManagerApplication.class, args);
    }

}
