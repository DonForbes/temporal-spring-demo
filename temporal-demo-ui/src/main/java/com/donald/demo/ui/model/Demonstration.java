package com.donald.demo.ui.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties("demonstration")
public class Demonstration {
    //private Collection<Demo> demonstrations;
    private String test;
    private List<Demo> demo;
}
