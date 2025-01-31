package com.donald.demo.temporaldemoserver.namespace.workflows;

import com.donald.demo.temporaldemoserver.namespace.activities.NamespaceManagement;
import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsNamespace;
import com.donald.demo.temporaldemoserver.namespace.model.WorkflowMetadata;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;

import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.spring.boot.autoconfigure.properties.TemporalProperties;
import io.temporal.spring.boot.autoconfigure.properties.WorkerProperties;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.Optional;

import org.springframework.context.ApplicationContext;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContextAware;

@WorkflowImpl
public class DeleteNamespaceImpl implements DeleteNamespace, ApplicationContextAware {
    public static final Logger logger = Workflow.getLogger(DeleteNamespaceImpl.class);
    private ApplicationContext ctx;

    private CloudOperationsNamespace cloudOpsNamespace = new CloudOperationsNamespace();
    private WorkflowMetadata wfMetadata;


  

    @Override
    public String deleteNamespace( WorkflowMetadata pWFMetadata, CloudOperationsNamespace pCloudOpsNamespace) {
        wfMetadata = pWFMetadata;

        // Parse the config to pick out the task queue for the activity. (Will be simpler once issue #1647 implemented)
        TemporalProperties props = ctx.getBean(TemporalProperties.class);
        Optional<WorkerProperties> wp =
              props.getWorkers().stream().filter(w -> w.getName().equals("OpsActivityDemoWorker")).findFirst();
        String taskQueue = wp.get().getTaskQueue();

        logger.debug("The task queue we are setting for the activity is [{}]", taskQueue);

        NamespaceManagement namespaceManagement = Workflow.newActivityStub(NamespaceManagement.class,
        ActivityOptions.newBuilder()
                       .setStartToCloseTimeout(Duration.ofSeconds(30))
                       .setTaskQueue(taskQueue)
                       .build());

        cloudOpsNamespace = namespaceManagement.getExistingNamespace(pCloudOpsNamespace, wfMetadata.getApiKey());

        // Set data gathered to indicate to UI that we have queried the API and have the data, either initially for the CA only 
        // or for all existing details.
        wfMetadata.setNsDataGathered(true);
        wfMetadata.setApproved(false);

        while (!wfMetadata.getApproved())
        {

          Workflow.await(Duration.ofMinutes(wfMetadata.getManageNamespaceTimeoutMins()), () -> wfMetadata.getApproved());
          if (wfMetadata.getApproved())
          {
            // Approval for delete has been received
            logger.debug("Deleting namespace [{}]", cloudOpsNamespace.getName());

            namespaceManagement.deleteNamespace(cloudOpsNamespace, wfMetadata.getApiKey());

        
          }
          else
          {  
            // Break out of the loop.
            logger.debug("The timer fired with no approval so we are completing the workflow with no action taken.");
            return "Timed out waiting for approval.  Not deleting namespace [" +  cloudOpsNamespace.getName() + "]";
          }
        }

        logger.debug("Mailing out to users [{}]", namespaceManagement.emailChanges(cloudOpsNamespace));
        
        return "Successful deletion of namespace";
   
    }
    @Override
    public CloudOperationsNamespace getNamespaceDetails() {
        return cloudOpsNamespace;
    }
    @Override
    public WorkflowMetadata getWFMetadata() {
        logger.debug("Returning Metadata to query - [{}]", wfMetadata.toString());
        return wfMetadata;
    }
    @Override
    public void setApproved() {
       wfMetadata.setApproved(true);
    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        ctx = applicationContext;
    }

}
