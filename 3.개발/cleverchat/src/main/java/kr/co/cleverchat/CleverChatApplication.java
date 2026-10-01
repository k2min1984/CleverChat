package kr.co.cleverchat;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableScheduling
@MapperScan(basePackages = "kr.co.cleverchat", annotationClass = Mapper.class)
public class CleverChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleverChatApplication.class, args);
    }
}
