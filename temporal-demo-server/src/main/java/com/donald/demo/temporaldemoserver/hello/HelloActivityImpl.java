package com.donald.demo.temporaldemoserver.hello;

import org.springframework.stereotype.Component;

import com.donald.demo.temporaldemoserver.hello.model.Person;

import io.temporal.spring.boot.ActivityImpl;

@Component
@ActivityImpl(taskQueues = "HelloDemoTaskQueue")
public class HelloActivityImpl implements HelloActivity {

    @Override
    public String hello(Person person) {
        // TODO Auto-generated method stub
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } // Sleeping the thread for a second to simulate a real external call.
        return "Hi " + person.getFirstName() + " " + person.getLastName();
    }
    
}
