package com.donald.demo.temporaldemoserver.namespace.workflows;

import com.donald.demo.temporaldemoserver.namespace.activities.CertificateManagement;
import com.donald.demo.temporaldemoserver.namespace.activities.NamespaceManagement;
import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsCertAuthority;
import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsNamespace;
import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsRotationHistory;
import com.donald.demo.temporaldemoserver.namespace.model.WorkflowMetadata;

import ch.qos.logback.core.joran.conditional.ElseAction;
import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.autoconfigure.properties.TemporalProperties;
import io.temporal.spring.boot.autoconfigure.properties.WorkerProperties;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.util.Optional;
import java.util.Collection;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ScheduleNamesapceCertRotationImpl implements ScheduleNamespaceCertRotation, ApplicationContextAware {
    public static final Logger logger = Workflow.getLogger(ScheduleNamesapceCertRotationImpl.class);
    ApplicationContext ctx;
    private CloudOperationsNamespace cloudOpsNamespace;
    private Collection<CloudOperationsRotationHistory> auditHistory = new ArrayList<>();
    private WorkflowMetadata wfMetadata;
    // Times (in days) for taking various actions.
    private long defaultWarningForCert = 31; // Default warning about about a month to go
    private long checkForNewCert = 7; // If a week before then look for new CA certs and if available automatically
                                      // add

    @Override
    public void checkAndRotateCACertificates(CloudOperationsNamespace pCloudOpsNamespace,
            WorkflowMetadata pWFMetadata) {
        logger.debug("methodEntry - checkAndRotateCACertificates");
        cloudOpsNamespace = pCloudOpsNamespace;
        wfMetadata = pWFMetadata;
        if (wfMetadata == null) {
            logger.error("No APIKey Provided to workflow.  This will cause errors.");
            wfMetadata = new WorkflowMetadata();
        }

        // Parse the config to pick out the task queue for the activity. (Will be
        // simpler once issue #1647 implemented)
        TemporalProperties props = ctx.getBean(TemporalProperties.class);
        Optional<WorkerProperties> wp = props.getWorkers().stream()
                .filter(w -> w.getName().equals("OpsActivityDemoWorker")).findFirst();
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

        // Get the current namespace details.
        cloudOpsNamespace = namespaceManagement.getExistingNamespace(pCloudOpsNamespace, wfMetadata.getApiKey());

        // Get the current CA. Assuming that we are likely to need it!
        CloudOperationsCertAuthority currentCACert = certManagement.getCurrentCACert();

        CloudOperationsRotationHistory aHistory;
        LocalDateTime timeForWorkflowRun = Instant.ofEpochMilli(Workflow.currentTimeMillis())
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
        boolean caCertAlreadyPresent = false; // Will be looking at all the CACerts in place and if this issuer is not
                                              // present then add it automatically.
        boolean certsChanged = false; // If a cert is removed then use this flag to ensure the namespace is updated.



        for (CloudOperationsCertAuthority certAuth : cloudOpsNamespace.getCertAuthorityPublicCerts()) {

            /**
             * TODO put in the logic.
             * 
             * if this cert is from the same issuer as the current one then check serial
             * numbers.
             * If the same then update flag and check if the serial numbers match
             * if match then Nothing
             * else
             * clear the flag, want to add this cert authority....
             */
            if (certAuth.getSerialNumber().equals(currentCACert.getSerialNumber())) {
                logger.debug("There is a matching certificate already present in the namespace.");
                caCertAlreadyPresent = true;
            }

            if (certAuth.getExpiryDate().isBefore(timeForWorkflowRun)) {
                aHistory = new CloudOperationsRotationHistory(timeForWorkflowRun, "INFO",
                        "This current CA certificate has expired!  Removing from the list of valid certificates. Serial No ["
                                + certAuth.getSerialNumber() + "] Expiry ["
                                + certAuth.getExpiryDate() + "] Issuer ["
                                + certAuth.getIssuerPrincipal() + "]");
                auditHistory.add(aHistory);
                cloudOpsNamespace.getCertAuthorityPublicCerts().remove(certAuth);
                certsChanged = true;

            } else {
                if (certAuth.getExpiryDate().minus(defaultWarningForCert, ChronoUnit.DAYS)
                        .isBefore(timeForWorkflowRun)) {

                    aHistory = new CloudOperationsRotationHistory(timeForWorkflowRun, "INFO",
                            "This current CA certificate is due to expire in the next month. Serial No ["
                                    + certAuth.getSerialNumber() + "] Expiry ["
                                    + certAuth.getExpiryDate() + "] Issuer ["
                                    + certAuth.getIssuerPrincipal() + "]");
                    auditHistory.add(aHistory);

                    if (certAuth.getExpiryDate().minus(checkForNewCert, ChronoUnit.DAYS).isBefore(timeForWorkflowRun)) {
                        aHistory = new CloudOperationsRotationHistory(timeForWorkflowRun, "INFO",
                                "This current CA certificate is due to expire in the next week.  Please rotate CA. Serial No ["
                                        + certAuth.getSerialNumber() + "] Expiry ["
                                        + certAuth.getExpiryDate() + "] Issuer ["
                                        + certAuth.getIssuerPrincipal() + "]");
                        auditHistory.add(aHistory);
                    }

                } else {
                    logger.debug("Cert expiry is in more than 31 days.  Nothing to do for this one");
                    aHistory = new CloudOperationsRotationHistory(timeForWorkflowRun, "INFO",
                            "This current CA certificate has plenty of time to run,  Serial No ["
                                    + certAuth.getSerialNumber() + "] Expiry ["
                                    + certAuth.getExpiryDate() + "] Issuer ["
                                    + certAuth.getIssuerPrincipal() + "]");
                    auditHistory.add(aHistory);
                }
            }
        }

    if (!caCertAlreadyPresent) {
        aHistory = new CloudOperationsRotationHistory(timeForWorkflowRun, "INFO",
                "The current CA Certificate is not present.  Adding it to the list of valid CAs,  Serial No ["
                + currentCACert.getSerialNumber() + "] Expiry ["
                + currentCACert.getExpiryDate() + "] Issuer ["
                + currentCACert.getIssuerPrincipal() + "]");
        auditHistory.add(aHistory);
        Collection<CloudOperationsCertAuthority> certs = cloudOpsNamespace.getCertAuthorityPublicCerts();
        certs.add(currentCACert);
        cloudOpsNamespace.setCertAuthorityPublicCerts(certs);
        certsChanged = true; 
      }
      if (certsChanged) {
        namespaceManagement.updateNamespace(cloudOpsNamespace, pWFMetadata.getApiKey());
        // Send out an e-mail with details of the changes that occurred.
        namespaceManagement.emailChanges(cloudOpsNamespace);
      }
      else {
        auditHistory.add(new CloudOperationsRotationHistory(timeForWorkflowRun, 
                                                            "INFO",
                                                            "All certificates present are valid and no new default CA available yet."));
      }
        logger.debug("method Exit - checkAndRotateCACertificates");
    }  // End

    @Override
    public CloudOperationsNamespace getNamespaceDetails() {
        return cloudOpsNamespace;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ctx = applicationContext;
    }

    @Override
    public Collection<CloudOperationsRotationHistory> getAuditHistory() {
        return auditHistory;
    }

}
