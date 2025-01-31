package com.donald.demo.ui.model.operations;

import java.util.Collection;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties("cloud-operations-regions")
public class CloudOperationsRegions {
    private Collection<String> regions;
}
