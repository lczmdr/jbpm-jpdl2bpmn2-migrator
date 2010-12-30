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

package org.drools.jpdl.instance.node;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.drools.WorkingMemory;
import org.drools.event.RuleFlowGroupActivatedEvent;
import org.drools.event.RuleFlowGroupDeactivatedEvent;
import org.drools.event.process.ProcessCompletedEvent;
import org.drools.event.process.ProcessEventListener;
import org.drools.event.process.ProcessNodeLeftEvent;
import org.drools.event.process.ProcessNodeTriggeredEvent;
import org.drools.event.process.ProcessStartedEvent;
import org.drools.event.process.ProcessVariableChangedEvent;
import org.drools.jpdl.core.node.ProcessState;
import org.drools.process.core.context.variable.VariableScope;
import org.drools.process.instance.ProcessInstance;
import org.drools.process.instance.context.variable.VariableScopeInstance;
import org.drools.workflow.instance.NodeInstance;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.graph.def.Event;

public class ProcessStateInstance extends JpdlNodeInstance implements ProcessEventListener {

	private static final long serialVersionUID = 510l;
	
	private long processInstanceId;

	public ProcessState getProcessState() {
		return (ProcessState) getNode();
	}

	public void execute(NodeInstance from, String type) {
		Map<String, Object> parameters = null;
		Set<VariableAccess> variableAccesses = getProcessState().getVariableAccesses();
		if ((variableAccesses != null) && (!variableAccesses.isEmpty())) {
			parameters = new HashMap<String, Object>();
			// TODO: transient variables ?
			for (VariableAccess variableAccess : variableAccesses) {
				if (variableAccess.isReadable()) {
					String variableName = variableAccess.getVariableName();
					VariableScopeInstance variableScopeInstance = (VariableScopeInstance)
						resolveContextInstance(VariableScope.VARIABLE_SCOPE, variableName);
					Object value = variableScopeInstance.getVariable(variableName);
					if (value != null) {
						String mappedName = variableAccess.getMappedName();
						parameters.put(mappedName, value);
					}
				}
			}
		}
		addEventListeners();
		processInstanceId = getProcessInstance().getKnowledgeRuntime()
			.startProcess(getProcessState().getSubProcessName(), parameters).getId();
		fireEvent(Event.EVENTTYPE_SUBPROCESS_CREATED);
	}

    public void addEventListeners() {
        getProcessInstance().getKnowledgeRuntime().addEventListener(this);
    }

    public void removeEventListeners() {
        getProcessInstance().getKnowledgeRuntime().removeEventListener(this);
    }

    public void afterProcessCompleted(ProcessCompletedEvent event) {
        if ( event.getProcessInstance().getId() == processInstanceId ) {
            removeEventListeners();
    		Set<VariableAccess> variableAccesses = getProcessState().getVariableAccesses();
    		if ((variableAccesses != null) && (!variableAccesses.isEmpty())) {

    			for (VariableAccess variableAccess: variableAccesses) {
    				if (variableAccess.isWritable()) {
    					String mappedName = variableAccess.getMappedName();
    					VariableScopeInstance variableScopeInstance = (VariableScopeInstance)
							((ProcessInstance) event.getProcessInstance())
								.getContextInstance(VariableScope.VARIABLE_SCOPE);
    					Object value = variableScopeInstance.getVariable(mappedName);
    					if (value != null) {
        					String variableName = variableAccess.getVariableName();
        					variableScopeInstance = (VariableScopeInstance)
								resolveContextInstance(VariableScope.VARIABLE_SCOPE, mappedName);
        					variableScopeInstance.setVariable(variableName, value);
    					}
    				}
    			}
    		}
    		fireEvent(Event.EVENTTYPE_SUBPROCESS_END);
            leave();
        }
    }

    public void beforeProcessStarted(ProcessStartedEvent event) {
        // TODO Auto-generated method stub
        
    }

    public void afterProcessStarted(ProcessStartedEvent event) {
        // TODO Auto-generated method stub
        
    }

    public void beforeProcessCompleted(ProcessCompletedEvent event) {
        // TODO Auto-generated method stub
        
    }

//    public void afterProcessCompleted(ProcessCompletedEvent event) {
//        // TODO Auto-generated method stub
//        
//    }

    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
        // TODO Auto-generated method stub
        
    }

    public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
        // TODO Auto-generated method stub
        
    }

    public void beforeNodeLeft(ProcessNodeLeftEvent event) {
        // TODO Auto-generated method stub
        
    }

    public void afterNodeLeft(ProcessNodeLeftEvent event) {
        // TODO Auto-generated method stub
        
    }

    public void beforeVariableChanged(ProcessVariableChangedEvent event) {
        // TODO Auto-generated method stub
        
    }

    public void afterVariableChanged(ProcessVariableChangedEvent event) {
        // TODO Auto-generated method stub
        
    }


}
