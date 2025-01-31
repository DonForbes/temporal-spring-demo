package com.donald.demo.ui.model.operations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties("cloud-operations-service")
public class CloudOpsServerConfig {
    private  String host;
    private  int port;
    private  String protocol;


    public  String getBaseURI() {
        return protocol + "://" + host + ":" + port;
    }
}
