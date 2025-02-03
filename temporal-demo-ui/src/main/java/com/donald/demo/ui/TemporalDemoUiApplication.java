package com.donald.demo.ui;

import com.donald.demo.customize.TemporalOptionsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;


@SpringBootApplication
@Import(TemporalOptionsConfig.class)
public class TemporalDemoUiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TemporalDemoUiApplication.class, args);
	}

}
