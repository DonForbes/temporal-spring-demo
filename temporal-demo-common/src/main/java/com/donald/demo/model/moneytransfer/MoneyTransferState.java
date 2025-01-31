package com.donald.demo.model.moneytransfer;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MoneyTransferState {

    private int approvalTime = 300;
    private String approvedTime = "";
    private boolean approvalRequired = false;
    private int progressPercentage = 0;
    private TransferState transferState = TransferState.NEW;
    private String workflowStatus = "NEW";

    private MoneyTransferResponse moneyTransferResponse = new MoneyTransferResponse();
    private String workflowId;
}
