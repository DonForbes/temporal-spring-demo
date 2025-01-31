package com.donald.demo.temporaldemoserver.transfermoney;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.donald.demo.model.moneytransfer.ExecutionScenario;
import com.donald.demo.model.moneytransfer.MoneyTransfer;

import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import io.temporal.activity.Activity;
import io.temporal.activity.ActivityInfo;


//@ActivityImpl(taskQueues = "TransferMoneyDemoTaskQueue")
@Component  
@ActivityImpl
public class AccountTransferActivitiesImpl implements AccountTransferActivities {
  private static final Logger log = LoggerFactory.getLogger(AccountTransferActivitiesImpl.class);

    @Override
    public Boolean validate(MoneyTransfer moneyTransfer) {

        if (moneyTransfer.getWorkflowOption().equals(ExecutionScenario.BUG_IN_WORKFLOW))
            {
                // Doing something stupid.
                // Calculating the amount as a fraction of 100000
                int quantity = 0000;  //  Oops, should be 10000
                int percentage = 100 * Integer.parseInt(moneyTransfer.getAmount()) / quantity;
                log.debug("The percentage is " + percentage);
            }

        if (moneyTransfer.getWorkflowOption() == ExecutionScenario.INVALID_ACCOUNT)
            return Boolean.valueOf(false);
        else
            return Boolean.valueOf(true);
    }  //End validate

    @Override
    public boolean withdraw(MoneyTransfer moneyTransfer) {
  
        log.debug("Witdraw for input details of [" + moneyTransfer.toString() + "]");

        if (moneyTransfer.getWorkflowOption().equals(ExecutionScenario.API_DOWNTIME))  //  Going to try things 5 times.
        {
            ActivityInfo info = Activity.getExecutionContext().getInfo();
            if (info.getAttempt() < 5)
                throw ApplicationFailure.newFailure("Simulate activity Failure", "Withdraw-activity-failure");
            else 
                log.debug("Tried 5 times so simply continuing activity now which will complete successfully.");
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }


    @Override
    public boolean undoWithdraw(MoneyTransfer moneyTransfer) {
        log.debug("Reverting withdrawal for [" + moneyTransfer.toString() + "]");
        return true;
    }

    @Override
    public boolean deposit(MoneyTransfer moneyTransfer) {
        
        log.debug("Deposit for input details of [" + moneyTransfer.toString() + "]");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        if (moneyTransfer.getWorkflowOption() == ExecutionScenario.FAIL_DEPOSIT)
            {
                log.debug("Scenario is to fail the deposit, meaning we will have to revert the withdrawal)");
                throw ApplicationFailure.newNonRetryableFailure("Deposit failed irrecoverably.", "Deposit-Failure");
            }
        return true;
    }

    @Override
    public boolean compensate(MoneyTransfer moneyTransfer) {
        log.debug("Applying compensation transaction.");

        try {
            Thread.sleep(1000);  //Brief snooze to 
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return true;
    }

}
