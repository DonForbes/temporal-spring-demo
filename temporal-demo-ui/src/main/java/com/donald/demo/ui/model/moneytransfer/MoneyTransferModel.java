package com.donald.demo.ui.model.moneytransfer;

import java.util.List;

import com.donald.demo.model.moneytransfer.Person;
import com.donald.demo.model.moneytransfer.WorkflowOption;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties("money-transfer")
public class MoneyTransferModel {
    private String defaultAmount;
    private List<Person> destinationAccounts;
    private List<WorkflowOption> workflowOptions;

}
