package com.donald.demo.temporaldemoserver.hello;

import java.time.Duration;
import java.util.Optional;

import io.temporal.common.VersioningIntent;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.donald.demo.temporaldemoserver.hello.model.Person;
import com.donald.demo.temporaldemoserver.transfermoney.AccountTransferActivities;

import io.temporal.activity.ActivityOptions;
import io.temporal.api.common.v1.SearchAttributesOrBuilder;
import io.temporal.common.SearchAttributeKey;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.spring.boot.autoconfigure.properties.TemporalProperties;
import io.temporal.spring.boot.autoconfigure.properties.WorkerProperties;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;

@Component
@WorkflowImpl
public class HelloWorkflowImpl implements HelloWorkflow, ApplicationContextAware {

    public static final Logger logger = Workflow.getLogger(HelloWorkflowImpl.class);
    private static ApplicationContext ctx;
//    private HelloActivity activity =
//       Workflow.newActivityStub(
//        HelloActivity.class,
//        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

    @Override
    public String sayHello(Person person) {

        TemporalProperties props = HelloWorkflowImpl.getApplicationContext().getBean(TemporalProperties.class);
        Optional<WorkerProperties> wp =
              props.getWorkers().stream().filter(w -> w.getName().equals("HelloDemoWorker")).findFirst();
        String taskQueue = wp.get().getTaskQueue();
        logger.info("Task Queue used for activity [{}]",taskQueue);
        HelloActivity activity = Workflow.newActivityStub(
            HelloActivity.class,
            ActivityOptions.newBuilder()
                             .setStartToCloseTimeout(Duration.ofSeconds(5))
                             .setTaskQueue(taskQueue)
                             .setVersioningIntent(VersioningIntent.VERSIONING_INTENT_COMPATIBLE)
                             .build());

        logger.info(person.toString());

        SearchAttributeKey key = SearchAttributeKey.forOffsetDateTime("TemporalScheduledStartTime");  // Need to find what these should be called.  Feels like an enum of some sort!
        logger.info("The workflow scheduled time is [{}]", Workflow.getTypedSearchAttributes().get(key));
        String helloResponse = activity.hello(person);

        return helloResponse;
    }


    public static ApplicationContext getApplicationContext() {
        return ctx;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ctx = applicationContext;
    }

}
