package kr.co.cleverchat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@MapperScan("kr.co.cleverchat.domain.**.mapper")
public class CleverChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleverChatApplication.class, args);
    }
}
