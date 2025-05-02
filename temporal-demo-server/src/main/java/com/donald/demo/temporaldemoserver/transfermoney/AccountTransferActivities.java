package com.donald.demo.temporaldemoserver.transfermoney;


import com.donald.demo.model.moneytransfer.MoneyTransfer;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface AccountTransferActivities {

    Boolean validate(MoneyTransfer moneyTransfer);
 
    boolean withdraw(MoneyTransfer moneyTransfer);

    boolean deposit (MoneyTransfer moneyTransfer);

    boolean undoWithdraw(MoneyTransfer moneyTransfer);

    boolean sendNotification(MoneyTransfer moneyTransfer);

    boolean compensate(MoneyTransfer moneyTransfer);

}
