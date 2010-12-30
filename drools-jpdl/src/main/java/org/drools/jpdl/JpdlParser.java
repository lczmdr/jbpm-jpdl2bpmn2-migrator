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

package org.drools.jpdl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.drools.jpdl.core.JpdlConnection;
import org.drools.jpdl.core.JpdlProcess;
import org.drools.jpdl.core.node.Decision;
import org.drools.jpdl.core.node.EndState;
import org.drools.jpdl.core.node.Fork;
import org.drools.jpdl.core.node.Join;
import org.drools.jpdl.core.node.JpdlNode;
import org.drools.jpdl.core.node.MailNode;
import org.drools.jpdl.core.node.ProcessState;
import org.drools.jpdl.core.node.StartState;
import org.drools.jpdl.core.node.State;
import org.drools.jpdl.core.node.SuperState;
import org.drools.jpdl.core.node.TaskNode;
import org.drools.process.core.ParameterDefinition;
import org.drools.process.core.context.swimlane.Swimlane;
import org.drools.process.core.context.swimlane.SwimlaneContext;
import org.drools.process.core.datatype.impl.type.StringDataType;
import org.drools.process.core.impl.ParameterDefinitionImpl;
import org.drools.process.core.validation.ProcessValidationError;
import org.drools.workflow.core.Node;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.ExceptionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;

public class JpdlParser {

    private static final Set<ParameterDefinition> EMAIL_PARAMETER_DEFINITIONS = new HashSet<ParameterDefinition>();
    private static final Pattern MAIL_TEMPLATE_PATTERN = Pattern.compile("<template>(.*)</template>", Pattern.DOTALL);
    private static final Pattern MAIL_ACTORS_PATTERN = Pattern.compile("<actors>(.*)</actors>", Pattern.DOTALL);
    private static final Pattern MAIL_TO_PATTERN = Pattern.compile("<to>(.*)</to>", Pattern.DOTALL);
    private static final Pattern MAIL_SUBJECT_PATTERN = Pattern.compile("<subject>(.*)</subject>", Pattern.DOTALL);
    private static final Pattern MAIL_TEXT_PATTERN = Pattern.compile("<text>(.*)</text>", Pattern.DOTALL);
    private int nodeId = 0;

    static {
        EMAIL_PARAMETER_DEFINITIONS.add(new ParameterDefinitionImpl("From", new StringDataType()));
        EMAIL_PARAMETER_DEFINITIONS.add(new ParameterDefinitionImpl("To", new StringDataType()));
        EMAIL_PARAMETER_DEFINITIONS.add(new ParameterDefinitionImpl("Subject", new StringDataType()));
        EMAIL_PARAMETER_DEFINITIONS.add(new ParameterDefinitionImpl("Text", new StringDataType()));
    }
    private ProcessValidationError[] errors;

    public JpdlProcess loadJpdlProcess(String name) {
        org.jbpm.graph.def.ProcessDefinition processDefinition =
                org.jbpm.graph.def.ProcessDefinition.parseXmlResource(name);
        return loadJpdlProcess(processDefinition);
    }

    public JpdlProcess loadJpdlProcess(org.jbpm.graph.def.ProcessDefinition processDefinition) {
        JpdlProcess process = new JpdlProcess();
        process.setId(processDefinition.getName());
        process.setName(processDefinition.getName());
        process.setPackageName("org.drools");
        SwimlaneContext swimlaneContext = (SwimlaneContext) process.getDefaultContext(SwimlaneContext.SWIMLANE_SCOPE);
        process.setDefaultContext(swimlaneContext);
        org.jbpm.graph.def.Node startState = processDefinition.getStartState();
        String startStateName = startState == null ? null : startState.getName();

        List<org.jbpm.graph.def.Node> nodes = processDefinition.getNodes();

        Map<org.jbpm.graph.def.Node, Node> mapping = new HashMap<org.jbpm.graph.def.Node, Node>();

        for (org.jbpm.graph.def.Node jPDLnode : nodes) {
            JpdlNode node = classifyNode(processDefinition, jPDLnode, swimlaneContext);
            //@TODO: see how to include this following code into the classifyNode method

            if (node == null) {
                throw new IllegalArgumentException(
                        "Unknown node type: " + jPDLnode.getClass().getName() + " " + jPDLnode);
            }
            setDefaultNodeProperties(jPDLnode, (JpdlNode) node);
            node.setId(++nodeId);
            mapping.put(jPDLnode, node);
            process.addNode(node);
            if (startStateName != null && startStateName.equals(node.getName())) {
                process.setStartState(node);
            }
        }

        generateConnections(mapping);

        errors = JpdlProcessValidator.getInstance().validateProcess(process);
        return process;
    }

    private JpdlNode classifyNode(org.jbpm.graph.def.ProcessDefinition processDefinition, org.jbpm.graph.def.Node jPDLnode, SwimlaneContext swimlaneContext) {
        JpdlNode node = null;
        if (jPDLnode instanceof org.jbpm.graph.node.StartState) {
            StartState newNode = new StartState();
            Task task = processDefinition.getTaskMgmtDefinition().getStartTask();
            if (task != null) {
                newNode.setTask(task);
                org.jbpm.taskmgmt.def.Swimlane jPDLswimlane = task.getSwimlane();
                if (jPDLswimlane != null) {
                    String swimlaneName = jPDLswimlane.getName();
                    if (swimlaneContext.getSwimlane(swimlaneName) == null) {
                        Swimlane swimlane = new Swimlane();
                        swimlane.setName(swimlaneName);
                        swimlane.setActorId(jPDLswimlane.getActorIdExpression());
                        // TODO support other types of actor expressions as well
                        swimlaneContext.addSwimlane(swimlane);
                    }
                }
            }
            node = newNode;
        } else if (jPDLnode instanceof org.jbpm.graph.node.EndState) {
            node = new EndState();
        } else if (org.jbpm.graph.def.Node.class.equals(jPDLnode.getClass())) {
            JpdlNode newNode = new JpdlNode();
            setDefaultNodeProperties(jPDLnode, newNode);
            node = newNode;
        } else if (jPDLnode instanceof org.jbpm.graph.node.Fork) {
            org.jbpm.graph.node.Fork jPDLfork =
                    (org.jbpm.graph.node.Fork) jPDLnode;
            Fork newNode = new Fork();
            newNode.setScript(jPDLfork.getScript());
            node = newNode;
        } else if (jPDLnode instanceof org.jbpm.graph.node.Join) {
            org.jbpm.graph.node.Join jPDLjoin =
                    (org.jbpm.graph.node.Join) jPDLnode;
            Join newNode = new Join();
            newNode.setDiscriminator(jPDLjoin.isDiscriminator());
            newNode.setTokenNames(jPDLjoin.getTokenNames());
            newNode.setScript(jPDLjoin.getScript());
            newNode.setNOutOfM(jPDLjoin.getNOutOfM());
            node = newNode;
        } else if (jPDLnode instanceof org.jbpm.graph.node.MailNode) {
            String config = jPDLnode.getAction().getActionDelegation().getConfiguration();

            MailNode newNode = new MailNode();
            Matcher matcher = MAIL_TEMPLATE_PATTERN.matcher(config);
            if (matcher.find()) {
                newNode.setTemplate(matcher.group(1));
            }
            matcher = MAIL_ACTORS_PATTERN.matcher(config);
            if (matcher.find()) {
                newNode.setActors(matcher.group(1));
            }
            matcher = MAIL_TO_PATTERN.matcher(config);
            if (matcher.find()) {
                newNode.setToEmail(matcher.group(1));
            }
            matcher = MAIL_SUBJECT_PATTERN.matcher(config);
            if (matcher.find()) {
                newNode.setSubject(matcher.group(1));
            }
            matcher = MAIL_TEXT_PATTERN.matcher(config);
            if (matcher.find()) {
                newNode.setText(matcher.group(1));
            }
            node = newNode;
        } else if (jPDLnode instanceof org.jbpm.graph.node.Decision) {
            org.jbpm.graph.node.Decision jPDLdecision =
                    (org.jbpm.graph.node.Decision) jPDLnode;
            Decision newNode = new Decision();
            newNode.setDecisionConditions(jPDLdecision.getDecisionConditions());
            // TODO: unable to access decisionDelegation
            // TODO: unable to access decisionExpression
            node = newNode;
        } else if (jPDLnode instanceof org.jbpm.graph.node.ProcessState) {
            org.jbpm.graph.node.ProcessState jPDLprocessState =
                    (org.jbpm.graph.node.ProcessState) jPDLnode;
            ProcessState newNode = new ProcessState();
            ProcessDefinition subProcessDefinition =
                    jPDLprocessState.getSubProcessDefinition();
            if (subProcessDefinition != null) {
                newNode.setSubProcessName(subProcessDefinition.getName());
                // TODO: parse sub process definition as well
            }
            // TODO: unable to access subProcessName
            // TODO: unable to access variableAccesses
            node = newNode;
        } else if (jPDLnode instanceof org.jbpm.graph.def.SuperState) {
            org.jbpm.graph.def.SuperState jPDLsuperState =
                    (org.jbpm.graph.def.SuperState) jPDLnode;
            SuperState newNode = new SuperState();
            List<org.jbpm.graph.def.Node> nodes = jPDLsuperState.getNodes();
            Map<org.jbpm.graph.def.Node, Node> mapping = new HashMap<org.jbpm.graph.def.Node, Node>();
            for (org.jbpm.graph.def.Node nodeInsideSuperState : nodes) {
                JpdlNode nodeToAdd = classifyNode(processDefinition, nodeInsideSuperState, swimlaneContext);
                if (nodeToAdd == null) {
                    throw new IllegalArgumentException(
                            "Unknown node type: " + jPDLnode.getClass().getName() + " " + jPDLnode);
                }
                setDefaultNodeProperties(nodeInsideSuperState, (JpdlNode) nodeToAdd);
                nodeToAdd.setId(++nodeId);
                mapping.put(nodeInsideSuperState, nodeToAdd);
                newNode.addNode(nodeToAdd);
            }
            generateConnections(mapping);
            node = newNode;
        } else if (jPDLnode instanceof org.jbpm.graph.node.TaskNode) {
            org.jbpm.graph.node.TaskNode jPDLtaskNode =
                    (org.jbpm.graph.node.TaskNode) jPDLnode;
            TaskNode newNode = new TaskNode();
            Set<Task> tasks = jPDLtaskNode.getTasks();
            newNode.setTasks(tasks);
            newNode.setSignal(jPDLtaskNode.getSignal());
            newNode.setCreateTasks(jPDLtaskNode.getCreateTasks());
            newNode.setEndTasks(jPDLtaskNode.isEndTasks());
            for (Task task : tasks) {
                org.jbpm.taskmgmt.def.Swimlane jPDLswimlane = task.getSwimlane();
                if (jPDLswimlane != null) {
                    String swimlaneName = jPDLswimlane.getName();
                    if (swimlaneContext.getSwimlane(swimlaneName) == null) {
                        Swimlane swimlane = new Swimlane();
                        swimlane.setName(swimlaneName);
                        swimlane.setActorId(jPDLswimlane.getActorIdExpression());
                        // TODO support other types of actor expressions as well
                        swimlaneContext.addSwimlane(swimlane);
                    }
                }
            }
            node = newNode;
        } else if (jPDLnode instanceof org.jbpm.graph.node.State) {
            node = new State();
        }
        return node;
    }

    private void generateConnections(Map<org.jbpm.graph.def.Node, Node> mapping) {
        for (Map.Entry<org.jbpm.graph.def.Node, Node> entry : mapping.entrySet()) {
            List<org.jbpm.graph.def.Transition> leavingTransitions = (List<org.jbpm.graph.def.Transition>) entry.getKey().getLeavingTransitions();
            if (leavingTransitions != null) {
                for (org.jbpm.graph.def.Transition transition : leavingTransitions) {
                    Node from = mapping.get(transition.getFrom());
                    Node to = mapping.get(transition.getTo());
                    String transitionName = transition.getName();
                    if (transitionName == null) {
                        transitionName = Node.CONNECTION_DEFAULT_TYPE;
                    }
                    // TODO: transition condition, events and exception handlers
                    JpdlConnection connection = new JpdlConnection(from, transitionName, to, Node.CONNECTION_DEFAULT_TYPE);
                    Map<String, Event> events = transition.getEvents();
                    if (events != null) {
                        for (Event event : events.values()) {
                            connection.addEvent(event);
                        }
                    }
                    List<ExceptionHandler> exceptionHandlers = transition.getExceptionHandlers();
                    if (exceptionHandlers != null) {
                        for (ExceptionHandler exceptionHandler : exceptionHandlers) {
                            connection.addExceptionHandler(exceptionHandler);
                        }
                    }
                    connection.setCondition(transition.getCondition());
                }
            }
        }
    }

    private void setDefaultNodeProperties(org.jbpm.graph.def.Node jPDLnode, JpdlNode newNode) {
        newNode.setName(jPDLnode.getName());
        newNode.setAction(jPDLnode.getAction());
        Map<String, Event> events = jPDLnode.getEvents();
        if (events != null) {
            for (Event event : events.values()) {
                newNode.addEvent(event);
                // TODO: extract timer actions and replace by our timer framework
            }
        }
        List<ExceptionHandler> exceptionHandlers = jPDLnode.getExceptionHandlers();
        if (exceptionHandlers != null) {
            for (ExceptionHandler exceptionHandler : exceptionHandlers) {
                newNode.addExceptionHandler(exceptionHandler);
            }
        }
    }

    public ProcessValidationError[] getErrors() {
        return this.errors;
    }
}
