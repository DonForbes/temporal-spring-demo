package com.donald.demo.model.moneytransfer;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MoneyTransfer {
    private String transferID;
    private String fromAccount;
    private Account toAccount = new Account();
    private String toAccountAsString;
    private ExecutionScenario workflowOption;
    private String amount;
    private String idempotencyKey;

    public void setToAccountAsString(String toAccountDetails) {
        this.toAccountAsString = toAccountDetails;
        if (this.toAccountAsString != null) {
            String[] accountElements = toAccountAsString.split(",");
            for (String entry : accountElements) {
                if (entry.contains("firstName")) {
                    String[] firstNameEntries = entry.split(":");
                    toAccount.setFirstName(firstNameEntries[1]);

                } else if (entry.contains("lastName")) {
                    String[] lastNameEntries = entry.split(":");
                    toAccount.setLastName(lastNameEntries[1]);

                } else if (entry.contains("sortCode")) {
                    String[] sortCodeEntries = entry.split(":");
                    toAccount.setSortCode(sortCodeEntries[1]);
                } else {
                    String[] accountNumberEntries = entry.split(":");
                    toAccount.setAccountNumber(accountNumberEntries[1]);
                }
            }
        }

    }
}
