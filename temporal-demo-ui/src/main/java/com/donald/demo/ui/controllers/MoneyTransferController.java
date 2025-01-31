package com.donald.demo.ui.controllers;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.donald.demo.ui.model.moneytransfer.MoneyTransferModel;
import com.donald.demo.model.moneytransfer.MoneyTransferResponse;
import com.donald.demo.model.moneytransfer.MoneyTransferState;
import com.donald.demo.model.moneytransfer.WorkflowStatus;
import com.donald.demo.util.TemporalClient;

import io.temporal.api.filter.v1.StartTimeFilter;
import io.temporal.api.filter.v1.WorkflowTypeFilter;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.ListClosedWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListClosedWorkflowExecutionsResponse;
import io.temporal.api.workflowservice.v1.ListOpenWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListOpenWorkflowExecutionsResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;

@Controller
public class MoneyTransferController {
    @Autowired
    private MoneyTransferModel toAccounts;
    @Autowired
    WorkflowClient client;
    private static final Logger logger = LoggerFactory.getLogger(MoneyTransferController.class);

    @GetMapping("/money-transfer-welcome")
    public String getMoneyTransferWelcome(Model model) {
        model.addAttribute("appName","Money Transfer");
        model.addAttribute("accounts", toAccounts.getDestinationAccounts());
        model.addAttribute("options", toAccounts.getWorkflowOptions());
        return new String("money-transfer-welcome");
    }

    @GetMapping("/money-transfer-details")
    public String getMoneyTransferDetails (@RequestParam(required = false) String workflowID, Model model) {
        System.out.println("money-transfer-details method entry.");
        System.out.println("Request parameter is -" + workflowID);
        model.addAttribute("appName","Money Transfer");
        model.addAttribute("selectedWorkflow", workflowID);
        // Setup a new model state object that is returned if workflow identified is not found.
        MoneyTransferState moneyTransferState = new MoneyTransferState();
        MoneyTransferResponse moneyTransferResp = new MoneyTransferResponse();
        moneyTransferResp.setChargeId("Not yet set");
        moneyTransferState.setMoneyTransferResponse(moneyTransferResp);


        if ((workflowID != null) && (workflowID.length() > 1))
        {
               WorkflowStub wfStub = client.newUntypedWorkflowStub(workflowID);
               moneyTransferState = wfStub.query("transferStatus", MoneyTransferState.class);
               logger.debug("moneyTransferState Returned: " +moneyTransferState.toString());
        }

        model.addAttribute("moneyTransferState", moneyTransferState);
        
        WorkflowServiceStubs wfStubs = client.getWorkflowServiceStubs();
        ListOpenWorkflowExecutionsResponse openResponse =
            wfStubs.blockingStub()
                   .listOpenWorkflowExecutions(
                    ListOpenWorkflowExecutionsRequest.newBuilder()
                      //.setStartTimeFilter(StartTimeFilter.newBuilder().setEarliestTime(TemporalClient.getOneHourAgo()).build())
                      .setTypeFilter(WorkflowTypeFilter.newBuilder().setName("TransferMoneyWorkflow").build())
                      .setNamespace(client.getOptions().getNamespace())
                      .build()
                );
             
        ListClosedWorkflowExecutionsResponse closedResponse =
          wfStubs.blockingStub()
                 .listClosedWorkflowExecutions(
                    ListClosedWorkflowExecutionsRequest.newBuilder()
                                    .setStartTimeFilter(StartTimeFilter.newBuilder().setEarliestTime(TemporalClient.getOneHourAgo()).build())
                                    .setTypeFilter(WorkflowTypeFilter.newBuilder().setName("TransferMoneyWorkflow").build())
                                    .setNamespace(client.getOptions().getNamespace())
                                    .build()
                 );


        List<WorkflowStatus> workflowStatii = new ArrayList<>();

        logger.debug("We have "+ openResponse.getExecutionsList().size() + " open workflows and " + closedResponse.getExecutionsList().size() + " Closed");
        WorkflowStatus aWfStatus;
        for (WorkflowExecutionInfo wfExecutionInfo : openResponse.getExecutionsList())  {
            aWfStatus = new WorkflowStatus();
            aWfStatus.setWorkflowId(wfExecutionInfo.getExecution().getWorkflowId());
            aWfStatus.setWorkflowStatus(wfExecutionInfo.getStatus().toString());
            aWfStatus.setUrl(TemporalClient.getWorkflowUrl(aWfStatus.getWorkflowId(), wfStubs.getOptions().getTarget(), client.getOptions().getNamespace()));
            
        workflowStatii.add(aWfStatus);
        }
        for (WorkflowExecutionInfo wfExecutionInfo : closedResponse.getExecutionsList())  {
            aWfStatus = new WorkflowStatus();
            aWfStatus.setWorkflowId(wfExecutionInfo.getExecution().getWorkflowId());
            aWfStatus.setWorkflowStatus(wfExecutionInfo.getStatus().toString());
            aWfStatus.setUrl(TemporalClient.getWorkflowUrl(aWfStatus.getWorkflowId(), wfStubs.getOptions().getTarget(), client.getOptions().getNamespace()));

            
            workflowStatii.add(aWfStatus);
        }

        for (WorkflowStatus wfStatus : workflowStatii) {
            logger.debug("Workflows " + wfStatus.getWorkflowId() + " " + wfStatus.getWorkflowStatus() + " " + wfStatus.getUrl());

            model.addAttribute("workflows",workflowStatii);
        }
        return new String("money-transfer-details");
    } // End getMoneyTransfer Details
   
    @PostMapping("/approveTransfer")
    public ResponseEntity postApproveTransfer(@RequestBody MoneyTransferState moneyTransferState) {
        // TODO: process POST request
        System.out.println("postapproveTransfer method entry.");

        System.out.println(moneyTransferState.toString());

        try {
            WorkflowStub workflowStub = client.newUntypedWorkflowStub(moneyTransferState.getWorkflowId());

            workflowStub.signal("approveTransfer");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        return new ResponseEntity<>("/money-transfer-details?workflowID=" + moneyTransferState.getWorkflowId(), HttpStatus.OK);
    }
}
