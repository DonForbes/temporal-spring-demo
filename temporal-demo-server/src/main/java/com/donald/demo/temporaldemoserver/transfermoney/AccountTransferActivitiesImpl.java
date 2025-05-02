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

import java.util.Random;

import static java.util.Random.*;


//@ActivityImpl(taskQueues = "TransferMoneyDemoTaskQueue")
@Component  
@ActivityImpl
public class AccountTransferActivitiesImpl implements AccountTransferActivities {
  private static final Logger log = LoggerFactory.getLogger(AccountTransferActivitiesImpl.class);

    @Override
    public Boolean validate(MoneyTransfer moneyTransfer) {
        pauseOrFail(500);
        if (moneyTransfer.getWorkflowOption().equals(ExecutionScenario.BUG_IN_WORKFLOW))
            {
                // Doing something stupid.
                // Calculating the amount as a fraction of 100000
                int quantity = 10000;  //  Oops, should be 10000
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
        pauseOrFail(1000);
        return true;
    }


    @Override
    public boolean undoWithdraw(MoneyTransfer moneyTransfer) {
        log.debug("Reverting withdrawal for [" + moneyTransfer.toString() + "]");
        pauseOrFail(1000);
        return true;
    }

    @Override
    public boolean sendNotification(MoneyTransfer moneyTransfer) {
        log.info("Notification sent to account holder of transfer");
        pauseOrFail(1000);

        return true;
    }

    @Override
    public boolean deposit(MoneyTransfer moneyTransfer) {
        
        log.debug("Deposit for input details of [" + moneyTransfer.toString() + "]");
        pauseOrFail(1001);

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
        pauseOrFail(1000);
        return true;
    }

    private void pauseOrFail(int duration) throws ApplicationFailure
    {
        // Add in a random error once every 4 times this is called.
        Random random = new Random();
        if (random.nextInt(4) == 3)
            throw ApplicationFailure.newFailure("Exception raised randomly (1 in 4)", "Transient failure");

        if (duration == 0)
            duration = 1000;

        try {
            Thread.sleep(duration);  //Brief snooze to
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
