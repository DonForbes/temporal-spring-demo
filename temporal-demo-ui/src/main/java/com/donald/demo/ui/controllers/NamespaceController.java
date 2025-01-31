package com.donald.demo.ui.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import com.donald.demo.ui.model.operations.CloudOpsConfig;
import com.donald.demo.ui.model.operations.CloudOpsServerConfig;
import com.donald.demo.ui.model.operations.WorkflowMetadata;

import io.temporal.api.enums.v1.ScheduleOverlapPolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.schedules.Schedule;
import io.temporal.client.schedules.ScheduleActionStartWorkflow;
import io.temporal.client.schedules.ScheduleCalendarSpec;
import io.temporal.client.schedules.ScheduleClient;
import io.temporal.client.schedules.ScheduleHandle;
import io.temporal.client.schedules.ScheduleIntervalSpec;
import io.temporal.client.schedules.ScheduleOptions;
import io.temporal.client.schedules.SchedulePolicy;
import io.temporal.client.schedules.ScheduleRange;
import io.temporal.client.schedules.ScheduleSpec;
import io.temporal.client.schedules.ScheduleState;
import io.temporal.client.schedules.ScheduleUpdate;
import io.temporal.client.schedules.ScheduleUpdateInput;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.workflow.ExternalWorkflowStub;
import io.temporal.workflow.Workflow;

import com.donald.demo.ui.model.operations.CloudOperationsNamespace;
import com.donald.demo.ui.model.operations.CloudOperationsRegions;
import com.donald.demo.ui.model.operations.CloudOperationsSchedule;

@Controller
public class NamespaceController {
  @Autowired
  CloudOpsServerConfig cloudOpsServerConfig;
  @Autowired
  ScheduleClient scheduleClient;
  @Autowired
  WorkflowClient client;
  @Autowired
  CloudOperationsRegions cloudOpsRegions;
  private static final Logger logger = LoggerFactory.getLogger(NamespaceController.class);
  private RestClient restClient = RestClient.create();
  private static final String MANAGE_WORKFLOW_PREFIX = "manage-namespace-";
  private static final String WORKFLOW_TYPE_MANAGE_NAMESPACE = "ManageNamespace";
  private static final String DELETE_WORKFLOW_PREFIX = "delete-namespace-";
  private static final String WORKFLOW_TYPE_DELETE_NAMESPACE = "DeleteNamespace";
  private static final String SCHEDULE_WORKFLOW_PREFIX = "schedule-namespace-cert-rotation-";
  private static final String WORKFLOW_TYPE_SCHEDULE_NAMESPACE = "ScheduleNamespaceCertRotation";

  private static final String MANAGE_WORKFLOW_TASK_QUEUE = "ManageNamespaceTaskQueue";

  @PostMapping("/namespace-update")
  public ResponseEntity<String> namespaceCreateOrUpdate(@RequestBody CloudOperationsNamespace cloudOpsNamespace,
      Model model) {
    // Method will signal the workflow with the latest version of the namespace and
    // then signal it to continue processing to complete or update the
    // workflow.
    logger.debug("Namespace to be updated/created is [{}]", cloudOpsNamespace.toString());
    WorkflowStub wfStub = client.newUntypedWorkflowStub(this.MANAGE_WORKFLOW_PREFIX + cloudOpsNamespace.getName());
    // WorkflowMetadata wfStatus =
    // wfStub.query("getWFMetadata",WorkflowMetadata.class);
    // model.addAttribute("apiKey", wfStatus.getApiKey());
    wfStub.signal("createOrUpdateNamespace", cloudOpsNamespace);
    return new ResponseEntity<>("\"" + "Namespace Update Completed" + "\"", HttpStatus.CREATED);
  } // End namespaceCreateOrUpdate

  @PostMapping("/namespace-management-details")
  public String namespaceUpdate(@ModelAttribute(value = "namespace") CloudOperationsNamespace cloudOpsNamespace,
      Model model) {
    logger.debug("method Entry: namespaceUpdate");
    logger.debug(cloudOpsNamespace.toString());
    model.addAttribute("title", "Namespace Management");
    model.addAttribute("regions", cloudOpsRegions.getRegions());
    logger.debug("Cloud operations regions [{}]", cloudOpsRegions.toString());

    WorkflowStub wfStub = client.newUntypedWorkflowStub(this.MANAGE_WORKFLOW_PREFIX + cloudOpsNamespace.getName());
    wfStub.signal("setNamespace", cloudOpsNamespace);

    // Having signalled the workflow now get the latest version of the namespace
    // from the workflow to display again. (Cos display fields are not included in
    // the form data.)
    cloudOpsNamespace = wfStub.query("getNamespaceDetails", CloudOperationsNamespace.class);
    WorkflowMetadata wfMetadata = wfStub.query("getWFMetadata", WorkflowMetadata.class);
    if (wfMetadata.getPageDisplay() == 1) {
      // Toggle to show page 2.
      wfMetadata.setPageDisplay(2);
    } else {
      // Toggle to show page 1
      wfMetadata.setPageDisplay(1);
    }
    wfStub.signal("setPageDisplay", wfMetadata);
    model.addAttribute("namespace", cloudOpsNamespace);
    model.addAttribute("page", wfMetadata.getPageDisplay());
    model.addAttribute("metadata", wfMetadata);

    return "namespace-management-details";
  }

  @GetMapping("/namespace-management/{namespaceName}")
  public String getNamespace(
      @RequestParam(required = false, value = "apiKey") String apiKey,
      @RequestParam(required = false, value = "isNewNamespace") Boolean isNewNamespace,
      @PathVariable(required = false, value = "namespaceName") String namespaceName,
      Model model) {
    model.addAttribute("title", "Namespace Management");
    logger.debug("getNamespace method entry - namepace[{}]", namespaceName);

    // Setup variables and parameters used to start the workflow.
    WorkflowMetadata wfMetadata = new WorkflowMetadata();
    wfMetadata.setApiKey(apiKey);
    wfMetadata.setIsNewNamespace(isNewNamespace);
    CloudOperationsNamespace cloudOpsNS = new CloudOperationsNamespace();
    cloudOpsNS.setName(namespaceName);

    try {
      startWorkflow(NamespaceController.WORKFLOW_TYPE_MANAGE_NAMESPACE, cloudOpsNS, wfMetadata);

    } catch (io.temporal.client.WorkflowException e) {
      logger.debug("Cause of error is [{}]", e.getCause().getMessage());
      model.addAttribute("status", e.getCause().getMessage());
      if (e.getCause().getMessage().contains("ALREADY_EXISTS")) {
        WorkflowStub untypedWFStub = client.newUntypedWorkflowStub(
            this.getWorkflowID(NamespaceController.WORKFLOW_TYPE_MANAGE_NAMESPACE, namespaceName));
        logger.debug("Workflow Stub [{}]", untypedWFStub.getOptions().toString());
        cloudOpsNS = untypedWFStub.query("getNamespaceDetails", CloudOperationsNamespace.class);
      }
    }

    WorkflowStub untypedWFStub = client
        .newUntypedWorkflowStub(this.getWorkflowID(NamespaceController.WORKFLOW_TYPE_MANAGE_NAMESPACE, namespaceName));
    cloudOpsNS = untypedWFStub.query("getNamespaceDetails", CloudOperationsNamespace.class);

    model.addAttribute("namespace", cloudOpsNS);
    model.addAttribute("workflowId", NamespaceController.WORKFLOW_TYPE_MANAGE_NAMESPACE + cloudOpsNS.getName());
    model.addAttribute("page", 1);
    model.addAttribute("regions", cloudOpsRegions.getRegions());
    model.addAttribute("metadata", untypedWFStub.query("getWFMetadata", WorkflowMetadata.class));

    return new String("/namespace-management-details");
  } // End getNamespace (With namespace to manage)

  private String getWorkflowID(String workflowType, String pNamespaceName) {
    String workflowIDPrefix;
    logger.debug("workflowType is [{}]", workflowType);
    switch (workflowType) {
      case NamespaceController.WORKFLOW_TYPE_MANAGE_NAMESPACE:
        workflowIDPrefix = NamespaceController.MANAGE_WORKFLOW_PREFIX;
        break;
      case NamespaceController.WORKFLOW_TYPE_DELETE_NAMESPACE:
        workflowIDPrefix = NamespaceController.DELETE_WORKFLOW_PREFIX;
        break;
      case NamespaceController.WORKFLOW_TYPE_SCHEDULE_NAMESPACE:
        workflowIDPrefix = NamespaceController.SCHEDULE_WORKFLOW_PREFIX;
        break;
      default:
        workflowIDPrefix = "Unknown-Namespace-Management-Process-";
    }

    return workflowIDPrefix + pNamespaceName;
  }

  private WorkflowStub getWFstub(String workflowType, String pNamespaceName) {

    String workflowId = getWorkflowID(workflowType, pNamespaceName);
    logger.debug("Returning workflow stub for workflow ID [{}]", workflowId);
    /**
     * Start a workflow to manage the state for the UI.
     */
    return client.newUntypedWorkflowStub(workflowType,
        WorkflowOptions.newBuilder()
            .setWorkflowId(workflowId)
            .setTaskQueue(this.MANAGE_WORKFLOW_TASK_QUEUE)
            .build());

  } // End getWFStub

  private CloudOperationsNamespace startWorkflow(String workflowType,
      CloudOperationsNamespace pCloudOpsNamespace,
      WorkflowMetadata pwfMetadata)
      throws io.temporal.client.WorkflowException {

    /**
     * Start a workflow to manage the state for the UI.
     */
    WorkflowStub untypedWFStub = this.getWFstub(workflowType, pCloudOpsNamespace.getName());

    // blocks until Workflow Execution has been started (not until it completes)
    WorkflowMetadata wfStatus = new WorkflowMetadata();

    untypedWFStub.start(pwfMetadata, pCloudOpsNamespace);

    boolean awaitPopulationOfNamespaceDetails = true;
    int counter = 0;
    while (awaitPopulationOfNamespaceDetails) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      counter++;
      wfStatus = untypedWFStub.query("getWFMetadata", WorkflowMetadata.class);
      if (wfStatus == null) {
        logger.debug(
            "The query on the worflow metadata returned null.  Retrying in the hope that the workflow is still initialising itself.");
        if (counter > 100)
          break;
      } else if ((wfStatus.getNsDataGathered() != null) && (wfStatus.getNsDataGathered())) {
        logger.debug("Got the initial information on the namespace to show user.");
        awaitPopulationOfNamespaceDetails = false;
      }
    }
    CloudOperationsNamespace cloudOpsNS = untypedWFStub.query("getNamespaceDetails", CloudOperationsNamespace.class);
    if (cloudOpsNS == null)
      logger.debug("For some reason our query returned the namespace as null.");
    else
      logger.debug("The namespace returned from the workflow is [{}]", cloudOpsNS.toString());

    return cloudOpsNS;
  } // End startWorkflow

  @GetMapping("namespace-management")
  public String getNamespaces(@RequestParam(required = false) String apiKey, Model model) {
    model.addAttribute("title", "Namespace Management");

    logger.debug("getNamespaces method entry");
    if (apiKey == null) // If no API key parameter then simply show page.
      return "namespace-management";

    if ((apiKey != null) && (apiKey.length() != 0)) {
      logger.debug("ApiKey to use is [{}] ", apiKey);
      // Retain the API key in the model for ease of user interaction.
      model.addAttribute("apiKey", apiKey);
      model.addAttribute("status", "OK");

      logger.debug(cloudOpsServerConfig.toString());
      System.out.println("Just About to make the remote call.");

      try {
        System.out.println("About to make the remote call.");
        List<CloudOperationsNamespace> namespaces = restClient.get()
            .uri(cloudOpsServerConfig.getBaseURI() + "/namespaces?apiKey=" + apiKey)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
              logger.info("Got an error back from operations.  Status[{}], Headers [{}]",
                  response.getStatusCode().toString(), response.getHeaders().toString());
              model.addAttribute("status", response.getStatusCode() + "-" + response.getHeaders().get("opsresponse"));
            })
            .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
              logger.info("Got an error back from operations.  Status[{}], Headers [{}]",
                  response.getStatusCode().toString(), response.getHeaders().toString());
              model.addAttribute("status", response.getStatusCode() + "-" + response.getHeaders().toString());
            })
            .body(List.class);

        model.addAttribute("namespaces", namespaces);

      } catch (HttpServerErrorException e) {
        logger.info("Error from the server [{}]", e.getMessage());
      } catch (HttpClientErrorException e) {
        logger.info("Error from cloud ops service [{}]", e.getMessage());
      } catch (Exception e) {
        logger.info("Exception from cloud ops service [{}]", e.getMessage());
      }

    } else
      model.addAttribute("status", "Please enter a valid API key to access Temporal Operations");

    return "namespace-management";

  }

  @GetMapping(value = { "/namespace-management-delete", "/namespace-management-delete/{namespaceName}" })
  public String deleteNamespace(@PathVariable(required = true, value = "namespaceName") String namespaceName,
      @RequestParam(required = true, value = "apiKey") String apiKey,
      Model model) {
    logger.debug("MethodEntry - deleteNamespace");
    logger.debug("The namespace we want to delete is - {}", namespaceName);

    // Start the delete workflow, wait for it to initialise itself and then set the
    // opsNamespace model and wf status up
    // Once initialised return control to the user to consider performing the actual
    // delete via form post.
    CloudOperationsNamespace cloudOpsNS = new CloudOperationsNamespace();
    cloudOpsNS.setName(namespaceName);
    WorkflowMetadata wfMetadata = new WorkflowMetadata();
    wfMetadata.setApiKey(apiKey);

    try {
      cloudOpsNS = startWorkflow(NamespaceController.WORKFLOW_TYPE_DELETE_NAMESPACE, cloudOpsNS, wfMetadata);

    } catch (io.temporal.client.WorkflowException e) {
      logger.debug("Cause of error is [{}]", e.getCause().getMessage());
      model.addAttribute("status", e.getCause().getMessage());
      if (e.getCause().getMessage().contains("ALREADY_EXISTS")) {
        WorkflowStub untypedWFStub = client.newUntypedWorkflowStub(
            this.getWorkflowID(NamespaceController.WORKFLOW_TYPE_DELETE_NAMESPACE, namespaceName));
        cloudOpsNS = untypedWFStub.query("getNamespaceDetails", CloudOperationsNamespace.class);
      }
    }
    model.addAttribute("title", "Namespace Management");
    model.addAttribute("metadata", wfMetadata);
    model.addAttribute("namespace", cloudOpsNS);

    return "namespace-management-delete";

  } // End deleteNamespace

  @DeleteMapping(value = "/namespace-management-delete/{namespaceName}")
  public String signalNamespaceDeletion(@PathVariable(required = true, value = "namespaceName") String namespaceName,
      Model model) {
    logger.debug("methodEntry - signalNamespaceDeletion foe Namespace [{}]", namespaceName);

    // Signal the delete workflow to progress with the delete.
    WorkflowStub untypedWFStub = client.newUntypedWorkflowStub(
        this.getWorkflowID(NamespaceController.WORKFLOW_TYPE_DELETE_NAMESPACE, namespaceName));

    untypedWFStub.signal("setApproved");

    model.addAttribute("status", "Namespace [" + namespaceName + "] is being deleted.");
    return "namespace-management";
  }

  @PostMapping(value = "/namespace-management-schedule/{namespaceName}")
  public String scheduleNamespaceCertRotation(
      @PathVariable(required = true, value = "namespaceName") String namespaceName,
      @RequestHeader("Authorization") String apiKeyBearer,
      @RequestBody CloudOperationsSchedule cloudOpsSchedule,
      Model model) {
    // Controller to set the schedule for a workflow and schedule it to run on a
    // given frequency.
    // Not providing all possible options, just using the interval to allow it to be
    // set every so many mins/hours/days
    CloudOperationsNamespace cloudOpsNamespace = new CloudOperationsNamespace();
    cloudOpsNamespace.setName(namespaceName);
    logger.debug("The authentication details are [{}]" + apiKeyBearer);
    logger.debug("THe schedule is [{}]", cloudOpsSchedule.toString());
    WorkflowMetadata wfMetadata = new WorkflowMetadata();
    wfMetadata.setApiKey(apiKeyBearer.replace("Bearer ", ""));

    WorkflowOptions workflowOptions = WorkflowOptions.newBuilder()
        .setWorkflowId(this.getWorkflowID(this.WORKFLOW_TYPE_SCHEDULE_NAMESPACE, namespaceName))
        .setTaskQueue(this.MANAGE_WORKFLOW_TASK_QUEUE)
        .build();

    ScheduleActionStartWorkflow action = ScheduleActionStartWorkflow.newBuilder()
        .setWorkflowType("ScheduleNamespaceCertRotation")
        .setArguments(cloudOpsNamespace, wfMetadata)
        .setOptions(workflowOptions)
        .build();

    Schedule schedule = Schedule.newBuilder()
        .setAction(action)
        .setSpec(ScheduleSpec.newBuilder().build())
        .build();

    ScheduleHandle handle;
    try {
        handle = scheduleClient.createSchedule(this.getWorkflowID(this.WORKFLOW_TYPE_SCHEDULE_NAMESPACE, namespaceName),
                                                             schedule,
                                                             ScheduleOptions.newBuilder().build());

    } catch (io.temporal.client.schedules.ScheduleAlreadyRunningException except) {
      logger.error("Schedule already exists.  Capturing the error in the logs but continue processing.");
    }

    handle = scheduleClient.getHandle(this.getWorkflowID(this.WORKFLOW_TYPE_SCHEDULE_NAMESPACE, namespaceName));
    handle.trigger(ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_TERMINATE_OTHER);

    Duration scheduleDuration = cloudOpsSchedule.getDuration();

    // Update the schedule with a spec, so it will run periodically
    handle.update(
        (ScheduleUpdateInput input) -> {

          Schedule.Builder builder = Schedule.newBuilder(input.getDescription().getSchedule());

          builder.setSpec(
              ScheduleSpec.newBuilder()
                  .setIntervals(Collections.singletonList(new ScheduleIntervalSpec(scheduleDuration)))
                  .build());
          
          builder.setState(
              ScheduleState.newBuilder()
                  .setPaused(false)
                  .setLimitedAction(true)
                  .setRemainingActions(10)   // For testing purposes just limit to 10 runs - tis just a demo
                  .build());

            // Temporal's default schedule policy is 'skip'
            builder.setPolicy(
              SchedulePolicy.newBuilder()
                  .setOverlap(ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_TERMINATE_OTHER)
                  .build());

          return new ScheduleUpdate(builder.build());
        });

    return "namespace-management";

  } // End scheduleNamespaceCertRotation (Post Mapping)

  @GetMapping(value = "/namespace-management-schedule/{namespaceName}")
  public String displayNamespaceCertRotation(
      @PathVariable(required = true, value = "namespaceName") String namespaceName,
      @RequestParam(required = true, value = "apiKey") String apiKey,
      Model model) {

    CloudOperationsNamespace cloudOpsNamespace = new CloudOperationsNamespace();
    cloudOpsNamespace.setName(namespaceName);
    WorkflowMetadata wfMetadata = new WorkflowMetadata();
    wfMetadata.setApiKey(apiKey);

    model.addAttribute("namespace", cloudOpsNamespace);
    model.addAttribute("title", "Schedule Namespace CA Certificate Rotation - " + cloudOpsNamespace.getName());
    model.addAttribute("metadata", wfMetadata);
    model.addAttribute("status", "OK");
    return "namespace-management-schedule";
  }
}
