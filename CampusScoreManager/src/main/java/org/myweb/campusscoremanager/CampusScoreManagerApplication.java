package org.myweb.campusscoremanager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.myweb.campusscoremanager.mapper")
public class CampusScoreManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusScoreManagerApplication.class, args);
    }

}
