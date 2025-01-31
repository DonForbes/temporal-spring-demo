package com.donald.demo.temporaldemoserver.namespace.workflows;

import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import com.donald.demo.temporaldemoserver.hello.HelloWorkflowImpl;
import com.donald.demo.temporaldemoserver.namespace.activities.CertificateManagement;
import com.donald.demo.temporaldemoserver.namespace.activities.CertificateManagementImpl;
import com.donald.demo.temporaldemoserver.namespace.activities.NamespaceManagement;
import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsCertAuthority;
import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsNamespace;
import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsRotationHistory;
import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsServerConfig;
import com.donald.demo.temporaldemoserver.namespace.model.WorkflowMetadata;
import com.donald.demo.temporaldemoserver.transfermoney.AccountTransferActivities;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.autoconfigure.properties.TemporalProperties;
import io.temporal.spring.boot.autoconfigure.properties.WorkerProperties;
import io.temporal.workflow.Workflow;

public class ManageNamespaceImpl implements ManageNamespace,ApplicationContextAware {
    public static final Logger logger = Workflow.getLogger(ManageNamespaceImpl.class);
    private boolean changed = false;
    private boolean processNamespace = false;
    private CloudOperationsNamespace cloudOpsNamespace = new CloudOperationsNamespace();
    private WorkflowMetadata wfMetadata;
    private ApplicationContext ctx;

    @Override
    public CloudOperationsNamespace manageNamespace(WorkflowMetadata pWFMetadata,
            CloudOperationsNamespace pCloudOpsNamespace) {
        wfMetadata = pWFMetadata;
        

        // Parse the config to pick out the task queue for the activity. (Will be simpler once issue #1647 implemented)
        TemporalProperties props = ctx.getBean(TemporalProperties.class);
        Optional<WorkerProperties> wp =
              props.getWorkers().stream().filter(w -> w.getName().equals("OpsActivityDemoWorker")).findFirst();
        String taskQueue = wp.get().getTaskQueue();

        logger.debug("The task queue we are setting for the activity is [{}]", taskQueue);

        CertificateManagement certManagement = Workflow.newActivityStub(
            CertificateManagement.class,
            ActivityOptions.newBuilder()
                           .setStartToCloseTimeout(Duration.ofSeconds(30))
                           .setTaskQueue(taskQueue)
                           .build());
        NamespaceManagement namespaceManagement = Workflow.newActivityStub(
            NamespaceManagement.class,
            ActivityOptions.newBuilder()
                           .setStartToCloseTimeout(Duration.ofSeconds(30))
                           .setTaskQueue(taskQueue)
                           .build());


        if (wfMetadata.getIsNewNamespace()) {
            logger.debug("Running workflow to create a new namespace.");
            // Activity call to get the current CA.
            cloudOpsNamespace.setName(pCloudOpsNamespace.getName());
            CloudOperationsCertAuthority aCertAuth = certManagement.getCurrentCACert();
            logger.debug("The cert authority details returned by the api contained [{}]", aCertAuth.toString());
            Collection<CloudOperationsCertAuthority> certAuthorities = new ArrayList<>();
            certAuthorities.add(aCertAuth);
            cloudOpsNamespace.setCertAuthorityPublicCerts(certAuthorities);
        } else {
            logger.debug("Running workflow to gather existing NS details and allow editing.");
            cloudOpsNamespace = namespaceManagement.getExistingNamespace(pCloudOpsNamespace, wfMetadata.getApiKey());
        }

        // Set data gathered to indicate to UI that we have queried the API and have the
        // data, either initially for the CA only
        // or for all existing details.
        wfMetadata.setNsDataGathered(true);

        // Keep workflow alive until it has remained untouched for more than the
        // metadata duration time or it has completed successfully.

        while (!changed) {
            logger.debug("Current value of namespace [{}] just after initial population", cloudOpsNamespace.toString());

            Workflow.await(Duration.ofMinutes(wfMetadata.getManageNamespaceTimeoutMins()), () -> changed);
            if (changed) {
                // Reset changed back to true and await again
                if (processNamespace) {
                    logger.debug("User looking to create or update the namespace so continuing processing");
                    break;
                } else {
                    logger.debug("changed set to true so changing it to be false and waiting a further [{}] minutes.",
                            wfMetadata.getManageNamespaceTimeoutMins());
                    changed = false;
                }
            } else {
                // Break out of the loop.
                logger.debug(
                        "The timer fired with no updates inbetween so we are completing the workflow with no actions.");
                return cloudOpsNamespace;
            }
        }

        // Continuing processing update or new namespace

        try {
            if (wfMetadata.getIsNewNamespace()) {
                logger.debug("Creating a new namespace [{}]", cloudOpsNamespace.toString());

                namespaceManagement.createNamespace(cloudOpsNamespace, wfMetadata.getApiKey());

            } else {
                logger.debug("Updating namespace [{}]", cloudOpsNamespace.getName());
            }
            logger.debug("E-mailing users.  Result of mail [{}]", namespaceManagement.emailChanges(pCloudOpsNamespace));

        } catch (ActivityFailure appEx) {
            logger.debug("Failed to update/create the namespace. [{}]", appEx.getMessage());
            logger.debug("E-mailing users.  Result of Failure mail [{}]",
                    namespaceManagement.emailFailure(pCloudOpsNamespace));
        }

        return cloudOpsNamespace;
    }

    @Override
    public CloudOperationsNamespace getNamespaceDetails() {
        return cloudOpsNamespace;
    }

    @Override
    public void setNamespace(CloudOperationsNamespace pCloudOpsNamespace) {
        logger.debug("MethodEntry - setNamespace called to set the values of the NS.");
        updateNS(pCloudOpsNamespace);
        changed = true;
    }

    @Override
    public void createOrUpdateNamespace(CloudOperationsNamespace pCloudOpsNamespace) {
        logger.debug("MethodEntry - createOrUpdateNamespace called to set the values of the NS. [{}]",
                pCloudOpsNamespace);
        processNamespace = true;
        updateNS(pCloudOpsNamespace);
        changed = true;

    }

    private void updateNS(CloudOperationsNamespace pCloudOpsNamespace) {
        // Only allowing the change a few of the attributes just now
        cloudOpsNamespace.setRetentionPeriod(pCloudOpsNamespace.getRetentionPeriod());
        cloudOpsNamespace.setActiveRegion(pCloudOpsNamespace.getActiveRegion());
        cloudOpsNamespace.setCodecEndPoint(pCloudOpsNamespace.getCodecEndPoint());
    } // End updateNS

    @Override
    public WorkflowMetadata getWFMetadata() {
        logger.debug("Returning Metadata to query - [{}]", wfMetadata.toString());
        return wfMetadata;
    }

    @Override
    public void setPageDisplay(WorkflowMetadata pWFMetadata) {
        // This method simply sets the page to be displayed in the wfMetadata object.
        wfMetadata.setPageDisplay(pWFMetadata.getPageDisplay());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ctx = applicationContext;
    }

}