package com.donald.demo.temporaldemoserver.namespace.model;

import lombok.Data;

@Data
public class WorkflowMetadata {
    private String apiKey;
    private Boolean isNewNamespace;
    private Boolean nsDataGathered;
    private int manageNamespaceTimeoutMins;
    private int pageDisplay = 1;
    private Boolean approved = false;

    public void setManageNamespaceTimeoutMins(int timeout)
    {
        if (timeout < 1)
        { 
            System.out.println("Timeout requested too short defaulting to 10 minutes.");
            this.manageNamespaceTimeoutMins=10;
        }
        else {
            this.manageNamespaceTimeoutMins=timeout;
        }
    }
}
