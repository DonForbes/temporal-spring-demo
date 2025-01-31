package com.donald.demo.temporaldemoserver.namespace.activities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsCertAuthority;
import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsNamespace;
import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsServerConfig;
import com.donald.demo.temporaldemoserver.transfermoney.AccountTransferActivitiesImpl;

import io.temporal.spring.boot.ActivityImpl;

@Component
@ActivityImpl()
public class CertificateManagementImpl implements CertificateManagement {
    private static final Logger logger = LoggerFactory.getLogger(AccountTransferActivitiesImpl.class);
    @Autowired
    private CloudOperationsServerConfig cloudOpsServerConfig;

    @Override
    public CloudOperationsCertAuthority getCurrentCACert() {
        logger.debug("Attempting to connect using [{}]", cloudOpsServerConfig.toString());

        CloudOperationsCertAuthority cloudOpsCertAuth = new CloudOperationsCertAuthority();
        CloudOperationsCertAuthority currentCAPublicCert = RestClient.create().get()
                .uri(cloudOpsServerConfig.getBaseURI() + "/get-current-ca-certificate")
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
                .body(CloudOperationsCertAuthority.class);
        if (currentCAPublicCert == null)
        {
            logger.debug("No public CA returned from the namespace operations app.  Returning a blank entity.");
            return new CloudOperationsCertAuthority();
        }
        else
            return currentCAPublicCert;
    }

}
