package com.donald.demo.temporaldemoserver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.uber.m3.util.ImmutableMap;
import org.checkerframework.checker.units.qual.K;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.donald.demo.temporaldemoserver.hello.HelloWorkflow;
import com.donald.demo.temporaldemoserver.hello.HelloWorkflowImpl;
import com.donald.demo.temporaldemoserver.hello.model.Person;
import com.donald.demo.temporaldemoserver.transfermoney.TransferMoneyWorkflow;
import com.donald.demo.model.moneytransfer.MoneyTransfer;
import com.donald.demo.temporaldemoserver.transfermoney.util.IdGenerator;

import io.temporal.common.SearchAttributeKey;
import io.temporal.common.SearchAttributes;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
// @CrossOrigin(origins = "http://localhost:8080")
@CrossOrigin()
public class TemporalServerDemoRESTController {
  private static final Logger logger = LoggerFactory.getLogger(TemporalServerDemoRESTController.class);

  @Autowired
  WorkflowClient client;




  @PostMapping("hello-world")
  public ResponseEntity<String> helloWorld(@RequestBody Person person) {
    logger.debug("Entered helloWorld controller method");
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

    this.registerWorker("HelloDemoTaskQueue", HelloWorkflowImpl.class);

    HelloWorkflow workflow = client.newWorkflowStub(
        HelloWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue("HelloDemoTaskQueue")
            .setWorkflowId("HelloDemo" + timeStamp)
            .setMemo(ImmutableMap.of("memoKey", "MemoValue"))
          //  .setStartDelay(Duration.ofHours(1)) 
            .build());

    return new ResponseEntity<>("\"" + workflow.sayHello(person) + "\"", HttpStatus.OK);
  }

  @PostMapping("money-transfer")
  public String transferMoney(@RequestBody MoneyTransfer transferRequest) {
    logger.debug("Entered tranferMoney controller method");
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

   // this.registerWorker("TransferMoneyDemoTaskQueue", TransferMoneyWorkflowImpl.class);
    logger.info(transferRequest.toString());
    
    transferRequest.setToAccountAsString(null);

    String workflowID = IdGenerator.generateWorkflowId();
    SearchAttributes searchAttribs = SearchAttributes.newBuilder()
                                                     .set(SearchAttributeKey.forText("WorkflowIdText"), workflowID)
                                                     .build();


    TransferMoneyWorkflow workflow = client.newWorkflowStub(
        TransferMoneyWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue("TransferMoneyDemoTaskQueue")
            .setWorkflowId(workflowID)
            .setTypedSearchAttributes(searchAttribs)
            .build());

        
        WorkflowExecution wfExecution = WorkflowClient.start(workflow::transfer, transferRequest);

        DescribeWorkflowExecutionRequest request =
                              DescribeWorkflowExecutionRequest.newBuilder()
                                             .setNamespace(client.getOptions().getNamespace())
                                             .setExecution(WorkflowExecution.newBuilder().setWorkflowId(workflowID))
                                             .build();
        DescribeWorkflowExecutionResponse response = client.getWorkflowServiceStubs().blockingStub().describeWorkflowExecution(request);

        logger.debug("workflow started with status - " + response.getWorkflowExecutionInfo().getStatus());
     
     return "\"/money-transfer-details?workflowID=" + workflowID + "\"";
  } // End transferMoney


  private void registerWorker(String taskQueue, Class<?> registerWorkflowClass)  {
      // Activities seem OK so it might be just wthe workflows we need to get a worker for.
      WorkerFactory factory = WorkerFactory.newInstance(client);
      Worker worker = factory.newWorker(taskQueue);
      worker.registerWorkflowImplementationTypes(registerWorkflowClass);
      factory.start();
  }
}
