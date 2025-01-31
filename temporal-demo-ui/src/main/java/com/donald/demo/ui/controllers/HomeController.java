package com.donald.demo.ui.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


import com.donald.demo.ui.model.Demonstration;
import com.donald.demo.ui.model.moneytransfer.MoneyTransferModel;

import io.temporal.client.WorkflowClient;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Controller
public class HomeController {
    @Autowired
    private Demonstration theDemos;
    @Autowired
    private MoneyTransferModel toAccounts;
    @Autowired
    WorkflowClient client;

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @GetMapping("/")
    public String welcome(Model model) {
        System.out.println(theDemos.toString());
        model.addAttribute("listDemos", theDemos.getDemo());
        return "welcome";
    }

    @GetMapping("/hello-world")
    public String getHelloWorld(Model model) {
        model.addAttribute("sample", "Hello World");
        return new String("hello-world");
    }




    @GetMapping("/hello")
    public String getHello() {
        System.out.println("Hello running from a get request");
        return new String("/");
    }
    @PostMapping("/post-hello")
    public String postMethodName(@RequestBody String entity) {
        //TODO: process POST request
        System.out.println("post-hello Entered");

        System.out.println(entity.toString());
        
        return "hello-world";
    }
    
}
