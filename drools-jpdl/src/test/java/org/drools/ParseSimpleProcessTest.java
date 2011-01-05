/**
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools;

import junit.framework.TestCase;

import org.drools.jpdl.EpdlWriter;
import org.drools.jpdl.JpdlParser;
import org.drools.jpdl.core.JpdlProcess;
import org.jbpm.process.core.validation.ProcessValidationError;
import org.jbpm.process.instance.ProcessInstance;
import org.drools.process.instance.WorkItemHandler;
import org.drools.rule.Package;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemManager;

public class ParseSimpleProcessTest extends TestCase {

    
    public void FIXtestSimpleProcess2() throws Exception {
        JpdlParser parser = new JpdlParser();
        JpdlProcess process = parser.loadJpdlProcess("simple2/processdefinition.xml");
        ProcessValidationError[] errors = parser.getErrors();
        for (ProcessValidationError error: errors) {
            System.err.println(error);
        }
        assertEquals(0, errors.length);

        RuleBase ruleBase = RuleBaseFactory.newRuleBase();
        Package p = new Package("com.sample");
        p.addProcess(process);
        ruleBase.addPackage( p );

        WorkingMemory workingMemory = ruleBase.newStatefulSession();
        TestWorkItemHandler handler = new TestWorkItemHandler();
        workingMemory.getWorkItemManager().registerWorkItemHandler(
                "Email", handler);
        assertTrue(handler.getWorkItemId() == -1);
        ProcessInstance processInstance = (ProcessInstance) workingMemory.startProcess("simple");
        assertEquals(ProcessInstance.STATE_ACTIVE, processInstance.getState());
        processInstance.signalEvent("signal", null);
        assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
    }

    public void FIXtestSimpleProcess3() throws Exception {
        JpdlParser parser = new JpdlParser();
        JpdlProcess process = parser.loadJpdlProcess("simple3/processdefinition.xml");
        ProcessValidationError[] errors = parser.getErrors();
        for (ProcessValidationError error: errors) {
            System.err.println(error);
        }
        assertEquals(0, errors.length);

        RuleBase ruleBase = RuleBaseFactory.newRuleBase();
        Package p = new Package("com.sample");
        p.addProcess(process);
        ruleBase.addPackage( p );

        WorkingMemory workingMemory = ruleBase.newStatefulSession();
        ProcessInstance processInstance = (ProcessInstance) workingMemory.startProcess("simple");
        assertEquals(ProcessInstance.STATE_ACTIVE, processInstance.getState());
        processInstance.signalEvent("signal", null);
        assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
    }

    private static class TestWorkItemHandler implements WorkItemHandler {
        private long workItemId = -1;
        public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
            workItemId = workItem.getId();
        }
        public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        }
        public long getWorkItemId() {
            return workItemId;
        }
    }

    public void testWriteEPDLStateNodes() throws Exception {
        JpdlParser parser = new JpdlParser();
        JpdlProcess process = parser.loadJpdlProcess("simple2states/processdefinition.xml");
        ProcessValidationError[] errors = parser.getErrors();
        assertEquals(0, errors.length);
        EpdlWriter.write(process);
    }

    public void testWriteEPDLDecisionNodes() throws Exception {
        JpdlParser parser = new JpdlParser();
        JpdlProcess process = parser.loadJpdlProcess("simple2decision/processdefinition.xml");
        ProcessValidationError[] errors = parser.getErrors();
        assertEquals(0, errors.length);
        EpdlWriter.write(process);
    }

    public void testWriteEPDLSuggestJoinNode() throws Exception {
        JpdlParser parser = new JpdlParser();
        JpdlProcess process = parser.loadJpdlProcess("simple2suggestJoinComplex/processdefinition.xml");
        ProcessValidationError[] errors = parser.getErrors();
        assertEquals(0, errors.length);
        EpdlWriter.write(process);
    }

    public void testWriteEPDLSuggestSplitNode() throws Exception {
        JpdlParser parser = new JpdlParser();
        JpdlProcess process = parser.loadJpdlProcess("simple2suggestSplitInActionNode/processdefinition.xml");
        ProcessValidationError[] errors = parser.getErrors();
        assertEquals(0, errors.length);
        EpdlWriter.write(process);
    }

    public void testWriteEPDLSuperState() throws Exception {
        JpdlParser parser = new JpdlParser();
        JpdlProcess process = parser.loadJpdlProcess("simple2superState/processdefinition.xml");
        ProcessValidationError[] errors = parser.getErrors();
        assertEquals(0, errors.length);
        EpdlWriter.write(process);
    }

    public void FIXtestWriteEPDLNestedForksWithSuperState() throws Exception {
        JpdlParser parser = new JpdlParser();
        JpdlProcess process = parser.loadJpdlProcess("simpleNestedForkWithSuperState/processdefinition.xml");
        ProcessValidationError[] errors = parser.getErrors();

        assertEquals(0, errors.length);

        EpdlWriter.write(process);

        for (ProcessValidationError error: errors) {
            System.err.println(error);
        }
        assertEquals(0, errors.length);

        RuleBase ruleBase = RuleBaseFactory.newRuleBase();
        Package p = new Package("com.sample");
        p.addProcess(process);
        ruleBase.addPackage( p );

        WorkingMemory workingMemory = ruleBase.newStatefulSession();
        ProcessInstance processInstance = (ProcessInstance) workingMemory.startProcess("simple");
        assertNotNull(processInstance);
    }

}
