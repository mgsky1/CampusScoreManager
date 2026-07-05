package com.campusscore;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 学生成绩管理系统后端启动类。 */
@SpringBootApplication
@MapperScan("com.campusscore.persistence")
public class CampusScoreApplication {

  public static void main(String[] args) {
    SpringApplication.run(CampusScoreApplication.class, args);
  }
}
