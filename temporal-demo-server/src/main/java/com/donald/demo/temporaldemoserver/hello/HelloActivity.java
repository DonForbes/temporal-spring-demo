package com.donald.demo.temporaldemoserver.hello;

import com.donald.demo.temporaldemoserver.hello.model.Person;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface HelloActivity {

    String hello(Person person);
}
