package com.springaws.SpringVideoApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class })
public class SpringVideoAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringVideoAppApplication.class, args);
	}

}
