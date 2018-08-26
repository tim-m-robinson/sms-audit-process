package org.ojas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.drools.core.time.impl.PseudoClockScheduler;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkflowProcessInstance;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.ojas.data.SmsMessageData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmsProcessTest extends JbpmJUnitBaseTestCase {
	
  private static final Logger logger = LoggerFactory.getLogger(SmsProcessTest.class);

  private RuntimeManager runtimeManager;
  private RuntimeEngine runtimeEngine;
  private KieSession kieSession;
  private TestWorkItemHandler testRestHandler;

  public SmsProcessTest() {
    // setup data source, enable persistence
    super(true, true);
  }
  
  @Before
  public void setup() {
    //Enable the PseudoClock using the following system property.
    System.setProperty("drools.clockType", "pseudo");
    // create runtime manager with single process
    runtimeManager = createRuntimeManager(Strategy.PROCESS_INSTANCE, "test", "org/ojas/processes/SmsAuditProcess.bpmn");
    // take RuntimeManager to work with process engine
    runtimeEngine = getRuntimeEngine();
    // get access to KieSession instance
    kieSession = runtimeEngine.getKieSession();
    // register a test handler for "Rest" WorkItem
    testRestHandler = getTestWorkItemHandler();
    kieSession.getWorkItemManager().registerWorkItemHandler("Rest", testRestHandler);
  }

  @After
  public void teardown() {
    runtimeManager.disposeRuntimeEngine(runtimeEngine);
    runtimeManager.close();
  }

  @Test
  public void testSuccess() {
    // start process
    ProcessInstance processInstance = kieSession.startProcess("SmsAuditProcess");

    // check whether the process instance has started successfully
    assertProcessInstanceActive(processInstance.getId());

    // check what nodes have been triggered
    assertNodeTriggered(processInstance.getId(), "Start", "SmsRestApi");
    
    // check whether the SMS has been requested
    WorkItem smsWorkItem = testRestHandler.getWorkItem();
    assertNotNull(smsWorkItem);
    assertEquals("Rest", smsWorkItem.getName());
    assertEquals("POST", smsWorkItem.getParameter("Method"));
    //assertEquals("you@mail.com", (SmsMessageData) smsWorkItem.getParameter("smsData"));

    // notify the engine the SMS has been sent
    HashMap<String, Object> smsResult = new HashMap<>();
    smsResult.put("Result", "{'result':'SUCCESS'}");
    smsResult.put("Status", 200);
    smsResult.put("StatusMsg", "OK");
    kieSession.getWorkItemManager().completeWorkItem(smsWorkItem.getId(), smsResult);

    // check that process completed
    assertNodeTriggered(processInstance.getId(), "End");      
    assertProcessInstanceCompleted(processInstance.getId());
  }

  @Test
  public void testFailure() {
    // setup instance data
    HashMap<String, Object> instanceData = new HashMap<>();
    SmsMessageData smsData = new SmsMessageData();
    smsData.setMsisdn("111222");
    smsData.setSmsMessage("Goodbye World!");
    instanceData.put("smsData", smsData);
    instanceData.put("smsResult", "");
    instanceData.put("smsStatus", -1);
    instanceData.put("smsStatusMsg", "");

    // start process
    ProcessInstance processInstance = kieSession.startProcess("SmsAuditProcess", instanceData);

    // check whether the process instance has started successfully
    assertProcessInstanceActive(processInstance.getId());

    // check what nodes have been triggered
    assertNodeTriggered(processInstance.getId(), "Start", "SmsRestApi");

    // check whether the SMS has been requested
    WorkItem smsWorkItem = testRestHandler.getWorkItem();
    assertNotNull(smsWorkItem);
    assertEquals("Rest", smsWorkItem.getName());
    assertEquals("POST", smsWorkItem.getParameter("Method"));
    //assertEquals("you@mail.com", (SmsMessageData) smsWorkItem.getParameter("smsData"));

    System.out.println("******* WORK ITEM STATE : "+workItemStateHelper(smsWorkItem.getState()));

    // notify the engine the SMS has been sent
    HashMap<String, Object> smsResult = new HashMap<>();
    smsResult.put("Result", "");
    smsResult.put("Status", 500);
    smsResult.put("StatusMsg", "Internal Server Error");
    kieSession.getWorkItemManager().completeWorkItem(smsWorkItem.getId(), smsResult);

    assertNodeTriggered(processInstance.getId(), "Wait");

    PseudoClockScheduler sessionClock = kieSession.getSessionClock();
    // Timer is set to 5 seconds, so advancing time 7 secondrs.
    sessionClock.advanceTime(7, TimeUnit.SECONDS);

    assertNodeTriggered(processInstance.getId(), "RepairData");

    System.out.println("******* PROCESS STATE: "+processStateHelper(processInstance.getState()));
    // check whether the SMS has been requested
    smsWorkItem = testRestHandler.getWorkItem();
    assertNotNull(smsWorkItem);
    System.out.println("******* WORK ITEM STATE : "+workItemStateHelper(smsWorkItem.getState()));

    // reset Rest Work Item
    smsResult.replace("Result", "{'result':'SUCCESS'}");
    smsResult.replace("Status", 200);
    smsResult.replace("StatusMsg", "OK");

    kieSession.getWorkItemManager().completeWorkItem(smsWorkItem.getId(), smsResult);

    // check that process completed
    assertNodeTriggered(processInstance.getId(), "End");
    assertProcessInstanceCompleted(processInstance.getId());
  }

  @Test
  public void testAbort() {
    // start process
    ProcessInstance processInstance = kieSession.startProcess("SmsAuditProcess");

    // check whether the process instance has started successfully
    assertProcessInstanceActive(processInstance.getId());

    // check what nodes have been triggered
    assertNodeTriggered(processInstance.getId(), "Start", "SmsRestApi");

    // check whether the SMS has been requested
    WorkItem smsWorkItem = testRestHandler.getWorkItem();
    assertNotNull(smsWorkItem);

    // abort the process
    kieSession.abortProcessInstance(processInstance.getId());

    // check the process state
    assertProcessInstanceAborted(processInstance.getId());

  }

  private String processStateHelper(int state) {
    String result = "UNKNOWN";
    switch(state) {
      case ProcessInstance.STATE_PENDING:
        return "PENDING";
      case ProcessInstance.STATE_ACTIVE:
        return "ACTIVE";
      case ProcessInstance.STATE_COMPLETED:
        return "COMPLETED";
      case ProcessInstance.STATE_ABORTED:
        return "ABORTED";
      case ProcessInstance.STATE_SUSPENDED:
        return "SUSPENDED";
    }
    return result;
  }

  private String workItemStateHelper(int state) {
    String result = "UNKNOWN";
    switch(state) {
      case WorkItem.PENDING:
        return "PENDING";
      case WorkItem.ACTIVE:
        return "ACTIVE";
      case WorkItem.COMPLETED:
        return "COMPLETED";
      case WorkItem.ABORTED:
        return "ABORTED";
    }
    return result;
  }

}
