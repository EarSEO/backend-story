package com.earseo.story;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class StoryApplication {

	public static void main(String[] args) {
		SpringApplication.run(StoryApplication.class, args);
	}

}
