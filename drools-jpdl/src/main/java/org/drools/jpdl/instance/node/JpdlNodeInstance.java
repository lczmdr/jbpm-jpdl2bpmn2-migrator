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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.common.InternalWorkingMemory;
import org.drools.jpdl.core.JpdlConnection;
import org.drools.jpdl.core.node.JpdlNode;
import org.jbpm.process.core.context.exception.ExceptionScope;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.instance.InternalProcessRuntime;
import org.jbpm.process.instance.ProcessInstance;
import org.jbpm.process.instance.context.exception.ExceptionScopeInstance;
import org.jbpm.process.instance.context.variable.VariableScopeInstance;
import org.jbpm.process.instance.timer.TimerInstance;
import org.jbpm.process.instance.timer.TimerManager;
import org.drools.runtime.process.EventListener;
import org.drools.runtime.process.NodeInstance;
import org.jbpm.workflow.core.Node;
import org.jbpm.workflow.instance.NodeInstanceContainer;
import org.jbpm.workflow.instance.impl.NodeInstanceImpl;
import org.jbpm.workflow.instance.node.EventBasedNodeInstanceInterface;
import org.jbpm.JbpmException;
import org.jbpm.calendar.BusinessCalendar;
import org.jbpm.calendar.Duration;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.DelegationException;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.ExceptionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.scheduler.def.CancelTimerAction;
import org.jbpm.scheduler.def.CreateTimerAction;

public class JpdlNodeInstance extends NodeInstanceImpl implements EventBasedNodeInstanceInterface, EventListener {

	private static final long serialVersionUID = 510l;
	private static final BusinessCalendar BUSINESS_CALENDAR = new BusinessCalendar();
	
    private Map<Long, Action> timerActions = new HashMap<Long, Action>();
	private Map<String, List<TimerInstance>> timers = new HashMap<String, List<TimerInstance>>();
	
	public JpdlNode getJpdlNode() {
		return (JpdlNode) getNode();
	}
	
	private Action getAction() {
		return getJpdlNode().getAction();
	}

	public void internalTrigger(NodeInstance from, String type) {
		fireEvent(Event.EVENTTYPE_NODE_ENTER);
		execute(from, type);
	}
	
	public void execute(NodeInstance from, String type) {
	    Action action = getAction();
        if (action != null) {
            try {
                action.execute(new JpdlExecutionContext());
            } catch (Exception exception) {
                raiseException(exception);
            }
        } else {
            leave();
        }
    }

	public void leave() {
		if (getNode().getOutgoingConnections(Node.CONNECTION_DEFAULT_TYPE) != null) {
			leave(Node.CONNECTION_DEFAULT_TYPE);
		} else if (getNode().getOutgoingConnections().size() == 1) {
			String type = getNode().getOutgoingConnections().keySet().iterator().next() ;
			leave(type);
		} else {
			throw new IllegalArgumentException(
				"Could not find default leave transition: " + this);
		}
	}

	public void leave(String type) {
		JpdlConnection connection = (JpdlConnection)
		    getJpdlNode().getOutgoingConnection(type);
		if (connection == null) {
			throw new JbpmException("transition '" + type
				+ "' is not a leaving transition of node '" + this + "'");
		}
		removeEventListeners();
		fireEvent(Event.EVENTTYPE_NODE_LEAVE);
        ((NodeInstanceContainer) getNodeInstanceContainer()).removeNodeInstance(this);
        Event event = connection.getEvent(Event.EVENTTYPE_TRANSITION);
        if (event != null) {
            List<Action> actions = event.getActions();
            if (actions != null) {
                for (Action action: actions) {
                    try {
                        action.execute(new JpdlExecutionContext());
                    } catch (Exception exception) {
                        boolean handled = false;
                        List<ExceptionHandler> exceptionHandlers = connection.getExceptionHandlers();
                        try {
                            for (ExceptionHandler exceptionHandler: exceptionHandlers) {
                                if (exceptionHandler.matches(exception)) {
                                    exceptionHandler.handleException(null, new JpdlExecutionContext());
                                    handled = true;
                                }
                            }
                        } catch (Exception e) {
                            exception = e;
                        }
                        if (!handled) {
                            if (exception instanceof JbpmException) {
                                throw (JbpmException) exception;
                            } else {
                                throw new DelegationException(exception, null);
                            }
                        }
                    }
                }
            }
        }
        String condition = connection.getCondition();
        if (condition != null) {
            Object result = JbpmExpressionEvaluator.evaluate(
                condition, new JpdlExecutionContext());
            if (result == null) {
                throw new JbpmException("connection condition " + condition
                        + " evaluated to null");
            } else if (!(result instanceof Boolean)) {
                throw new JbpmException("connection condition " + condition
                        + " evaluated to non-boolean: "
                        + result.getClass().getName());
            } else if (!((Boolean) result).booleanValue()) {
                throw new JbpmException("connection condition " + condition
                        + " evaluated to 'false'");
            }
        }
        ((NodeInstanceContainer) getNodeInstanceContainer())
        	.getNodeInstance(connection.getTo()).trigger(this, connection.getToType());
	}

    public void fireEvent(String eventType) {
        fireEvent(eventType, new JpdlExecutionContext());
    }

	public void fireEvent(String eventType, ExecutionContext executionContext) {
		Event event = getJpdlNode().getEvent(eventType);
		if (event != null) {
		    executeActions(event.getActions(), executionContext);
		}
		// TODO runtime actions?
	}

    public void executeActions(List<Action> actions, ExecutionContext executionContext) {
        if (actions != null) {
            for (Action action: actions) {
                executeAction(action, executionContext);
            }
        }
    }

	public void executeAction(Action action, ExecutionContext executionContext) {
	    if (action instanceof CreateTimerAction) {
	        CreateTimerAction createTimerAction = (CreateTimerAction) action; 
	        String timerName = createTimerAction.getTimerName();
	        TimerInstance timer = new TimerInstance();
	        long delay = BUSINESS_CALENDAR.add(new Date(0),
                new Duration(createTimerAction.getDueDate())).getTime();
            timer.setDelay(delay);
            if (createTimerAction.getRepeat() != null) {
                long period = BUSINESS_CALENDAR.add(new Date(0),
                    new Duration(createTimerAction.getRepeat())).getTime();
                timer.setPeriod(period);
            }
	        if (timerActions.isEmpty()) {
	            addTimerListener();
	        }
	        TimerManager timerManager = ((InternalProcessRuntime) ((InternalWorkingMemory) getProcessInstance().getKnowledgeRuntime()).getProcessRuntime()).getTimerManager();
	        timerManager.registerTimer(timer, getProcessInstance());
//	        getProcessInstance().getWorkingMemory().getTimerManager()
//	            .registerTimer(timer, getProcessInstance());
	        timerActions.put(timer.getId(), createTimerAction.getTimerAction());
	        List<TimerInstance> timerList = timers.get(timerName);
	        if (timerList == null) {
	            timerList = new ArrayList<TimerInstance>();
	            timers.put(timerName, timerList);
	        }
	        timerList.add(timer);
	    } else if (action instanceof CancelTimerAction) {
	        String timerName = ((CancelTimerAction) action).getTimerName();
	        List<TimerInstance> timerList = timers.get(timerName);
	        if (timerList != null) {
	            for (TimerInstance timer: timerList) {
	                timerActions.remove(timer.getId());
	                TimerManager timerManager = ((InternalProcessRuntime) ((InternalWorkingMemory) getProcessInstance().getKnowledgeRuntime()).getProcessRuntime()).getTimerManager();
	                timerManager.cancelTimer(timer.getId());
//	                getProcessInstance().getWorkingMemory().getTimerManager()
//	                    .cancelTimer(timer.getId());
	            }
                timers.remove(timerName);
                if (timerActions.isEmpty()) {
                    removeTimerListener();
                }
	        }
	    } else {
    		try {
    			action.execute(executionContext);
    		} catch (Exception exception) {
    			raiseException(exception);
    		}
	    }
	}

	public void raiseException(Throwable exception) throws DelegationException {
		Class<?> clazz = exception.getClass();
		while (clazz != null) {
			ExceptionScopeInstance exceptionScopeInstance = (ExceptionScopeInstance)
				resolveContextInstance(ExceptionScope.EXCEPTION_SCOPE, clazz.getName());
			if (exceptionScopeInstance != null) {
				exceptionScopeInstance.handleException(clazz.getName(), exception);
				return;
			}
			clazz = clazz.getSuperclass();
		}
		if (exception instanceof JbpmException) {
			throw (JbpmException) exception;
		} else {
			throw new DelegationException(exception, null);
		}
	}
	
    public String[] getEventTypes() {
    	return new String[] { "timerTriggered" };
    }
    
    public void signalEvent(String type, Object event) {
    	if ("timerTriggered".equals(type)) {
    		TimerInstance timer = (TimerInstance) event;
            Action action = timerActions.get(timer.getId());
            if (action != null) {
                executeAction(action, new JpdlExecutionContext());
            }
    	}
    }
        public class JpdlToken extends Token{

            public JpdlToken(NodeInstance node) {
                org.jbpm.graph.def.Node jBPMNode = new org.jbpm.graph.def.Node();
                jBPMNode.setName(node.getNodeName());
                this.setNode(jBPMNode);
            }

        }
	public class JpdlExecutionContext extends ExecutionContext {
	    public JpdlExecutionContext(NodeInstance node) {
	        super((Token) new JpdlToken(node));
	    }
             public JpdlExecutionContext() {
	        super((Token) null);
	    }
	    public void leaveNode() {
	        leave();
	    }
	    public void leaveNode(String transitionName) {
	        leave(transitionName);
	    }
	    public void leaveNode(Transition transition) {
	        leaveNode(transition.getName());
	    }
	    public void setVariable(String name, Object value) {
	        VariableScopeInstance variableScopeInstance = (VariableScopeInstance)
	            resolveContextInstance(VariableScope.VARIABLE_SCOPE, name);
	        variableScopeInstance.setVariable(name, value);
        }
        public Object getVariable(String name) {
            VariableScopeInstance variableScopeInstance = (VariableScopeInstance)
                resolveContextInstance(VariableScope.VARIABLE_SCOPE, name);
            if (variableScopeInstance == null) {
            	variableScopeInstance = (VariableScopeInstance) 
            		((ProcessInstance) JpdlNodeInstance.this.getProcessInstance())
            			.getContextInstance(VariableScope.VARIABLE_SCOPE);
            }
            return variableScopeInstance.getVariable(name);
        }
        public ContextInstance getContextInstance() {
        	ContextInstance contextInstance = new ContextInstance() {
        		public Object getVariable(String name) {
        			return JpdlExecutionContext.this.getVariable(name);
        		}
        	};
        	return contextInstance;
        }
        public ProcessInstance getDroolsProcessInstance() {
            return JpdlNodeInstance.this.getProcessInstance();
        }
        public NodeInstance getDroolsNodeInstance() {
            return JpdlNodeInstance.this;
        }
        public ProcessDefinition getProcessDefinition() {
        	return null;
        }
	}

	public void addEventListeners() {
        if (timers.size() > 0) {
        	addTimerListener();
        }
    }

	public void addTimerListener() {
    	getProcessInstance().addEventListener("timerTriggered", this, false);
	}
	
    public void removeEventListeners() {
        if (timers.size() > 0) {
        	removeTimerListener();
        }
    }
    
    public void removeTimerListener() {
    	getProcessInstance().removeEventListener("timerTriggered", this, false);
    }

}
