# Highlights of Temporal Primative (Code features) used

## Activities
[..transferMoney.AccountTransferActivites](/src/main/java/com/donald/demo/temporaldemoserver/transfermoney/AccountTransferActivities.java)

Showcase the different methods available as a Temporal Activity.  These are the method calls that are generally wrapping a call to an external system.  (Probably relatively unreliable due to network calls, messaging protocols etc.)

Once we understand activities then we can consider a workflow.

## Workflows
[..transferMoney.TransferMoneyWorkflow](/src/main/java/com/donald/demo/temporaldemoserver/transfermoney/TransferMoneyWorkflow.java)

Defines the interactions possible with this workflow.
* Workflow Method is the main process that will run 
* Query Method
* Signal Method
* Update Method
  * Update Validation Method

Now look at the [implementation class](/src/main/java/com/donald/demo/temporaldemoserver/transfermoney/TransferMoneyWorkflowImpl.java).

Lines of interest
* logger up front - specialised logger that does not output on replay.
* implementation of workflow method (transfer)
* The Workflow.sleep command 
* Calling of activity
  * Look at logic of approval section.  Has the wait for a timer and if not signaled within 30 seconds raise an appliation failure.
  * Check out the Deposit - catches the exception here and performs an undoWithdraw activity.  (Could have implemented using Saga capabilities.)
* Query method - gets the status of the app.  Used in the UI and also available from temporal UI.  (Works on closed workflows.)




