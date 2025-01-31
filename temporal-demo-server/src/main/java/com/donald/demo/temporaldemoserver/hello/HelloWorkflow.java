package com.donald.demo.temporaldemoserver.hello;

import com.donald.demo.temporaldemoserver.hello.model.Person;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HelloWorkflow {

    @WorkflowMethod
    String sayHello(Person person);
}
