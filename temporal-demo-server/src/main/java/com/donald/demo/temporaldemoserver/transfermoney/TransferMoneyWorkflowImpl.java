package com.donald.demo.temporaldemoserver.transfermoney;

import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.TimerOptions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;

import com.donald.demo.model.moneytransfer.ExecutionScenario;
import com.donald.demo.model.moneytransfer.MoneyTransfer;
import com.donald.demo.model.moneytransfer.MoneyTransferResponse;
import com.donald.demo.model.moneytransfer.MoneyTransferState;
import com.donald.demo.model.moneytransfer.TransferState;
import com.donald.demo.temporaldemoserver.transfermoney.util.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.spring.boot.autoconfigure.properties.TemporalProperties;
import io.temporal.spring.boot.autoconfigure.properties.WorkerProperties;
import io.temporal.workflow.Workflow;

@WorkflowImpl(taskQueues = "TransferMoneyDemoTaskQueue")
public class TransferMoneyWorkflowImpl implements TransferMoneyWorkflow, ApplicationContextAware {
    private MoneyTransferState moneyTransferState = new MoneyTransferState();

    public static final Logger logger = Workflow.getLogger(TransferMoneyWorkflowImpl.class);
    private ApplicationContext ctx;

    @Override
    public MoneyTransferResponse transfer(MoneyTransfer moneyTransfer) {
        logger.debug(("Entered - transfer method started."));
        moneyTransferState.setProgressPercentage(10);
        moneyTransferState.setWorkflowStatus("RUNNING");
        moneyTransferState.setWorkflowId(Workflow.getInfo().getWorkflowId());

        // Parse the config to pick out the task queue for the activity. (Will be simpler once issue #1647 implemented)
        TemporalProperties props = ctx.getBean(TemporalProperties.class);
        Optional<WorkerProperties> wp =
                props.getWorkers().stream().filter(w -> w.getName().equals("TransferMoneyActivityWorker")).findFirst();
        String taskQueue = wp.get().getTaskQueue();


        Workflow.sleep(Duration.ofSeconds(5));

        //  ***************************
        //  ***     VALIDATION      ***
        //  ***************************
        if (this.getActivity(null, taskQueue).validate(moneyTransfer) == false) {
            moneyTransferState.setProgressPercentage(100);
            moneyTransferState.setTransferState(TransferState.VALIDATION_FAILED);
            moneyTransferState.setWorkflowStatus("FAILED");

            throw ApplicationFailure.newFailure("The transfer failed to validate.", "ValidateionFailure");
        }
        moneyTransferState.setTransferState(TransferState.VALIDATED);
        moneyTransferState.setProgressPercentage(40);


        //  ***************************
        //  ***     APPROVAL        ***
        //  ***************************
        if ((moneyTransfer.getWorkflowOption() == ExecutionScenario.HUMAN_IN_LOOP) | (Long.parseLong(moneyTransfer.getAmount()) > 10000)) {
            moneyTransferState.setApprovalRequired(true);
            TimerOptions.Builder approveTimerOptions = TimerOptions.newBuilder().setSummary("Timer - Waiting for approval");
            Promise<Void> approvalTimer = Workflow.newTimer(Duration.ofSeconds(moneyTransferState.getApprovalTime()), approveTimerOptions.build());

//            boolean receivedSignal = Workflow.await(Duration.ofSeconds(moneyTransferState.getApprovalTime()), () -> moneyTransferState.getApprovedTime() != "");
            Workflow.await(() -> !Objects.equals(moneyTransferState.getApprovedTime(), "") || approvalTimer.isCompleted());

            boolean receivedSignal;
            if (moneyTransferState.getApprovedTime() != "")
                receivedSignal = true;
            else
                receivedSignal = false;

            if (!receivedSignal) {
                logger.error("Approval not received within the time limit.  Failing the workflow");
                moneyTransferState.setTransferState(TransferState.APPROVAL_TIMED_OUT);
                throwApplicationFailure("Transfer not approved within timelimit.", "ApprovalTimeout", null);
            } else
                moneyTransferState.setTransferState(TransferState.APPROVED);
        }

        //  ***************************
        //  ***     WITHDRAWAL      ***
        //  ***************************
        if (this.getActivity(null, taskQueue).withdraw(moneyTransfer))
            moneyTransferState.getMoneyTransferResponse().setWithdrawId(IdGenerator.generateTransferId());
        else {
            moneyTransferState.setTransferState(TransferState.WITHDRAW_FAILED);
            throwApplicationFailure("Withdrawal Failed to complete successsfully.", "WithdrawalFailure", null);
        }

        moneyTransferState.setTransferState(TransferState.FUNDS_WITHDRAWN);
        moneyTransferState.setProgressPercentage(50);

        Workflow.sleep(Duration.ofSeconds(4));

        //  ***************************
        //  ***      DEPOSIT        ***
        //  ***************************
        try {
            if (this.getActivity(null, taskQueue).deposit(moneyTransfer))
                moneyTransferState.getMoneyTransferResponse().setChargeId(IdGenerator.generateTransferId());
            moneyTransferState.setTransferState(TransferState.FUNDS_DEPOSITED);
        } catch (ActivityFailure activityEx) {
            moneyTransferState.setTransferState(TransferState.DEPOSIT_FAILED);
            this.getActivity(null, taskQueue).undoWithdraw(moneyTransfer);
        }


        moneyTransferState.setProgressPercentage(70);

        Workflow.sleep(Duration.ofSeconds(2));

        // *****************************
        // ***      Notification     ***
        // *****************************
        List<Promise<Boolean>> promiseNotifictions = new ArrayList<>();
        String actName;
        for (int i = 0; i < 3; i++) {
            switch (i) {
                case 0:
                    actName = "Send notification via e-mail";
                    break;
                case 1:
                    actName = "Send notification via slack";
                    break;
                case 2:
                    actName = "Send notifiation via SMS";
                    break;
                default:
                    actName = null;
                    break;
            }
            promiseNotifictions.add(Async.function(this.getActivity(actName, taskQueue)::sendNotification, moneyTransfer));
        }

        Promise.allOf(promiseNotifictions).get();
        moneyTransferState.setProgressPercentage(80);

        Workflow.sleep(Duration.ofSeconds(2));

        moneyTransferState.setProgressPercentage(100);
        if (moneyTransferState.getTransferState().toString().contains("FAIL"))
            moneyTransferState.setTransferState(TransferState.COMPLETED_WITH_FAILURE);
        else
            moneyTransferState.setTransferState(TransferState.COMPLETED);

        moneyTransferState.setWorkflowStatus("COMPLETED");
        return moneyTransferState.getMoneyTransferResponse();
    }  // End transfer

    private void throwApplicationFailure(String message, String type, Object details) throws ApplicationFailure {
        moneyTransferState.setWorkflowStatus("FAILED");
        throw ApplicationFailure.newFailure(message, type, details);
    }// End throwApplicationFailure


    @Override
    public MoneyTransferState getStateQuery() throws JsonProcessingException {
        logger.debug("Querying workflow - " + this.moneyTransferState.toString());
        return this.moneyTransferState;
    }


    @Override
    public void approveTransfer() {
        logger.info("approveTransferSignal with no arguments sent.");
        this.moneyTransferState.setApprovedTime(IdGenerator.returnFormattedWorkflowDate("dd MMM yyyy HH:mm:ss"));
    }

    @Override
    public void approveTransfer(boolean approvalResult) {
        logger.info("approveTransferSignal with boolean sent.");
        this.moneyTransferState.setApprovedTime(IdGenerator.returnFormattedWorkflowDate("dd MMM yyyy HH:mm:ss"));
    }

    @Override
    public String approveTransferUpdate() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'approveTransferUpdate'");
    }

    @Override
    public void approveTransferUpdateValidator() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'approveTransferUpdateValidator'");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ctx = applicationContext;
    }


    private AccountTransferActivities getActivity(String activityName, String taskQueue) {

        ActivityOptions.Builder actOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setTaskQueue(taskQueue);

        if (activityName != null && activityName.length() > 0)
            actOptions.setSummary(activityName);

        AccountTransferActivities activity = Workflow.newActivityStub(
                AccountTransferActivities.class, actOptions.build());

        return activity;

    }


}
