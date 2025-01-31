package com.donald.demo.temporaldemoserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsServerConfig;




@SpringBootApplication
public class TemporalServerDemonstrationApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(TemporalServerDemonstrationApplication.class, args);
	}

// Addition to allow looking at the beans created.  (Useful for setup of worker activity-bean config)
/**  
	@Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
    return args -> {

      System.out.println("Let's inspect the beans provided by Spring Boot:");

      String[] beanNames = ctx.getBeanDefinitionNames();
      //Arrays.sort(beanNames);
      for (String beanName : beanNames) {
        System.out.println(beanName);
      }
    };
  }
*/

  

}
