package com.azienda.documentmanager;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "15m")
@EnableAsync
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class DocumentManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentManagerApplication.class, args);
    }
}
