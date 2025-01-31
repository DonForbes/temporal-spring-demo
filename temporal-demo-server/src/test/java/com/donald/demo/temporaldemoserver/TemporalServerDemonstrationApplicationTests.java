package com.donald.demo.temporaldemoserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Assert;

import com.donald.demo.temporaldemoserver.hello.HelloWorkflow;
import com.donald.demo.temporaldemoserver.hello.model.Person;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;

@SpringBootTest(classes = TemporalServerDemonstrationApplicationTests.Configuration.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
class TemporalServerDemonstrationApplicationTests {

	@Autowired
	ConfigurableApplicationContext applicationContext;
	@Autowired
	TestWorkflowEnvironment testWorkflowEnvironment;
	@Autowired
	WorkflowClient workflowClient;

	@BeforeEach
	void setUp() {
		applicationContext.start();
	} // End before each

	@Test
	void contextLoads() {
	}

	@Test
	public void testHello() {
       HelloWorkflow workflow = 
	     workflowClient.newWorkflowStub(
              HelloWorkflow.class,
	          WorkflowOptions.newBuilder()
             	             .setTaskQueue("HelloDemoTaskQueue")
		                     .setWorkflowId("HelloDemoTest")
		                     .build());

		String result = workflow.sayHello(new Person("Donald", "Forbes"));
		Assert.notNull(result, "Greeting shoud not be null.");
		Assert.isTrue(result.equals("Hi Donald Forbes"), "Invalid greeting returned from test.");

	} // End testHello

	@ComponentScan
	public static class Configuration {
	}

}
