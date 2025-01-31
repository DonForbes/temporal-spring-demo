package com.donald.demo.temporaldemoserver.namespace.activities;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.donald.demo.temporaldemoserver.namespace.model.CloudOperations;
import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsNamespace;
import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsServerConfig;
import com.donald.demo.temporaldemoserver.namespace.model.WorkflowMetadata;
import com.donald.demo.temporaldemoserver.transfermoney.AccountTransferActivitiesImpl;

import io.temporal.api.nexus.v1.Response;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;

@Component
@ActivityImpl
public class NamespaceManagementImpl implements NamespaceManagement {
    private static final Logger logger = LoggerFactory.getLogger(AccountTransferActivitiesImpl.class);
    @Autowired
    private CloudOperationsServerConfig cloudOpsServerConfig;

    @Override
    public CloudOperationsNamespace getExistingNamespace(CloudOperationsNamespace pCloudOpsNamespace, String apiKey) {
        // Method will query the cloudOps API to gather the namespace details for the
        // namespace identified in the parameter.
        URI uri = UriComponentsBuilder
                .fromUriString("{baseURI}/namespace/{namespace}")
                .queryParam("apiKey", "{apiKey}")
                .buildAndExpand(cloudOpsServerConfig.getBaseURI(), pCloudOpsNamespace.getName(), apiKey)
                .toUri();

        logger.debug("The URI to be used is[{}]", uri.toString());

        CloudOperationsNamespace cloudOpsNS = RestClient.create().get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    logger.info("Got an error back from operations.  Status[{}], Headers [{}]",
                            response.getStatusCode().toString(), response.getHeaders().toString());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    logger.info("Got an error back from operations.  Status[{}], Headers [{}]",
                            response.getStatusCode().toString(), response.getHeaders().toString());
                })
                .body(CloudOperationsNamespace.class);

        return cloudOpsNS;
    }

    @Override
    public String createNamespace(CloudOperationsNamespace pCloudOpsNamespace, String apiKey) {
        // Method will send the POST REST request to the cloud ops server to create the namespace.
        URI uri = UriComponentsBuilder
                .fromUriString("{baseURI}/namespace")
                .buildAndExpand(cloudOpsServerConfig.getBaseURI())
                .toUri();

        logger.debug("The URI to be used is[{}]", uri.toString());

        CloudOperations cloudOps = new CloudOperations();
        WorkflowMetadata wfMetadata = new WorkflowMetadata();
        wfMetadata.setApiKey(apiKey);
        cloudOps.setWfMetadata(wfMetadata);
        cloudOps.setCloudOpsNamespace(pCloudOpsNamespace);

        String returnString = "Successful Create";
        try {
        ResponseEntity response = RestClient.create().post()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .body(cloudOps)
                .retrieve()
                .toBodilessEntity();
        }
        catch (RestClientException ex)
        {
            logger.debug("Failed to post the namespace to the Temporal Operations [{}] ", ex.getMessage());
            if (ex.getMessage().contains("INVALID_ARGUMENT")){
                logger.debug("Arguments passed to create were invalid");
                throw ApplicationFailure.newNonRetryableFailure("Failed to create namespace [" + ex.getMessage() + "]",  "Invalid-Parameters", null);
            }
            else    
                throw ApplicationFailure.newFailureWithCause("Failed to create namespace.[" + ex.getMessage() + "]", "Operations-API-Failure", null);
        }

        return returnString;
    }

    @Override
    public String updateNamespace(CloudOperationsNamespace cloudOpsNamespace, String apiKey) {
        logger.debug("methodEntry - updateNamespace for Namespace [{}]", cloudOpsNamespace.getName());
        URI uri = UriComponentsBuilder
            .fromUriString("{baseURI}/namespace/{namespaceName}")
            .buildAndExpand(cloudOpsServerConfig.getBaseURI(), cloudOpsNamespace.getName())
            .toUri();

        logger.debug("The URI to be used is[{}]", uri.toString());


        try {
            ResponseEntity<Void> response = RestClient.create().post()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(cloudOpsNamespace)
                    .retrieve()
                    .toBodilessEntity();
            }
            catch (RestClientException ex)
            {
                logger.debug("Failed to update the namespace to the Temporal Operations [{}] ", ex.getMessage());
                return "Failed to update namespace [" + ex.getMessage() + "]";
            }

        return "Namespace [" + cloudOpsNamespace.getName() + "] is being updated.";
    }

    @Override
    public String emailChanges(CloudOperationsNamespace cloudOpsNamespace) {
        logger.debug("Pretending to send an e-mail to the users of this [{}] namespace to let them know of the changes.", cloudOpsNamespace.getName() );
        try {
            Thread.sleep(1500); // Making it slightly real
        } catch (InterruptedException e) {
            e.printStackTrace();
        } 
        return "E-mail sent";
    }

    @Override
    public String deleteNamespace(CloudOperationsNamespace cloudOpsNamespace, String apiKey) {
        logger.debug("methodEntry - deleteNameslace for Namespace [{}]", cloudOpsNamespace.getName());
        URI uri = UriComponentsBuilder
            .fromUriString("{baseURI}/namespace/{namespaceName}")
            .buildAndExpand(cloudOpsServerConfig.getBaseURI(), cloudOpsNamespace.getName())
            .toUri();

        logger.debug("The URI to be used is[{}]", uri.toString());


        try {
            ResponseEntity<Void> response = RestClient.create().delete()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .toBodilessEntity();
            }
            catch (RestClientException ex)
            {
                logger.debug("Failed to delete the namespace to the Temporal Operations [{}] ", ex.getMessage());
                return "Failed to delete namespace [" + ex.getMessage() + "]";
            }

        return "Namespace [" + cloudOpsNamespace.getName() + "] is being deleted.";
    }

    @Override
    public String emailFailure(CloudOperationsNamespace cloudOpsNamespace) {
        logger.debug("Pretending to send an e-mail to the users of this [{}] namespace to let them know the changes failed.", cloudOpsNamespace.getName() );
        try {
            Thread.sleep(1500); // Making it slightly real
        } catch (InterruptedException e) {
            e.printStackTrace();
        } 
        return "Failure E-mail sent";
    }
}
