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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.drools.definition.process.Connection;
import org.drools.definition.process.Node;
import org.drools.jpdl.core.JpdlProcess;
import org.jbpm.graph.def.Action;
import org.jbpm.instantiation.Delegation;

/**
 *
 * @author salaboy
 */
public class EpdlWriter {

    private static final String EOL = "\n";

    private static int suggestedNodeId = 99;

    public static String write(JpdlProcess process) {
        Node[] nodes = process.getNodes();

        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append("<process xmlns=\"http://drools.org/drools-5.0/process\""+
                " xmlns:xs=\"http://www.w3.org/2001/XMLSchema-instance\" "+
                "xs:schemaLocation=\"http://drools.org/drools-5.0/process drools-processes-5.0.xsd\" " +
                "type=\"RuleFlow\" name=\"flow\" id=\""+process.getName()+"\" package-name=\"org.drools.examples\" >" + EOL);
        stringBuffer.append("<header>" + EOL);
        stringBuffer.append("</header>" + EOL);
        //Print nodes and Connections
        printNodesAndConnections(nodes, stringBuffer);
        stringBuffer.append("</process>");
        return stringBuffer.toString();
    }

    private static void printNodesAndConnections(Node[] nodes, StringBuffer stringBuffer) {
        String generatedConnections = "";
        stringBuffer.append("<nodes>" + EOL);
        for (Node node : nodes) {
            if (node instanceof org.drools.jpdl.core.node.StartState) {
                stringBuffer.append("<start id=\"" + node.getId() + "\" name=\"" + node.getName() + "\" />" + EOL);
            } else if (node instanceof org.drools.jpdl.core.node.Fork) {
                stringBuffer.append("<split id=\"" + node.getId() + "\" name=\"" + node.getName() + "\" type=\"1\" />" + EOL);
            } else if (node instanceof org.drools.jpdl.core.node.Join) {
                stringBuffer.append("<join id=\"" + node.getId() + "\" name=\"" + node.getName() + "\" type=\"1\" />" + EOL);
            } else if (node instanceof org.drools.jpdl.core.node.SuperState) {
                generatedConnections += suggestSplitNode(node, stringBuffer);
                generatedConnections += suggestJoinNode(node, stringBuffer);
                stringBuffer.append("<composite id=\""+ node.getId() +"\" name=\""+ node.getName() +"\">" + EOL);
                List<org.drools.jpdl.core.node.JpdlNode> nodesList = ((org.drools.jpdl.core.node.SuperState)node).getNodes();
                printNodesAndConnections(nodesList.toArray(new Node[nodesList.size()]), stringBuffer);
                printInPortsAndOutPorts(nodesList.toArray(new Node[nodesList.size()]), stringBuffer);
                stringBuffer.append("</composite>" + EOL);

            } else if (node instanceof org.drools.jpdl.core.node.State) {
                generatedConnections += suggestJoinNode(node, stringBuffer);
                stringBuffer.append("<state id=\"" + node.getId() + "\" name=\"" + node.getName() + "\" >" + EOL);
                stringBuffer.append("    <constraints>" + EOL);
                Set<String> keys = node.getOutgoingConnections().keySet();
                for (String key : keys) {
                    for (Connection connection : node.getOutgoingConnections(key)) {
                        stringBuffer.append("        <constraint toNodeId=\"" + connection.getTo().getId() + "\" name=\"signalTo" + connection.getTo().getName() + "\" />" + EOL);
                    }
                }
                stringBuffer.append("</constraints>" + EOL);
                stringBuffer.append("</state>" + EOL);
            } else if (node instanceof org.drools.jpdl.core.node.Decision) {
                stringBuffer.append("<split id=\"" + node.getId() + "\" name=\"" + node.getName() + "\" type=\"2\" >" + EOL);
                stringBuffer.append("    <constraints>" + EOL);
                Set<String> keys = node.getOutgoingConnections().keySet();
                for (String key : keys) {
                    for (Connection connection : node.getOutgoingConnections(key)) {
                        stringBuffer.append("        <constraint toNodeId=\"" + connection.getTo().getId() + "\" name=\"signalTo" + connection.getTo().getName() + "\" toType=\"DROOLS_DEFAULT\" type=\"rule\" dialect=\"java\" >" + EOL);
                        stringBuffer.append("        </constraint>" + EOL);
                    }
                }
                stringBuffer.append("    </constraints>" + EOL);
                stringBuffer.append("</split>" + EOL);
            } else if (node instanceof org.drools.jpdl.core.node.EndState) {
                generatedConnections += suggestJoinNode(node, stringBuffer);
                stringBuffer.append("<end id=\"" + node.getId() + "\" name=\"" + node.getName() + "\" />" + EOL);
            } else if (node instanceof org.drools.jpdl.core.node.JpdlNode) {
                generatedConnections += suggestSplitNode(node, stringBuffer);
                generatedConnections += suggestJoinNode(node, stringBuffer);
                stringBuffer.append("<actionNode id=\"" + node.getId() + "\" name=\"" + node.getName() + "\">" + EOL);
                stringBuffer.append("    <action type=\"expression\" dialect=\"java\" >" + EOL);
                Action action = ((org.drools.jpdl.core.node.JpdlNode) node).getAction();
                if (action != null) {
                    Delegation delegation = action.getActionDelegation();
                    if (delegation != null) {
                        // System.out.println("Introspect = "+delegation.getClassName());
                        // System.out.println("replaced"+delegation.getClassName().replace(".","/"));
                        // Resource resource = ResourceFactory.newInputStreamResource(EpdlWriter.class.getResourceAsStream(delegation.getClassName().replace(".","/")));
                        // System.out.println(""+resource);
                        //  System.out.println("Paste the content of the execute() method of the class"+delegation.getClassName());
                    }
                }
                stringBuffer.append("    </action>" + EOL);
                stringBuffer.append("</actionNode>" + EOL);
            }
        }
        stringBuffer.append("</nodes>" + EOL);
        stringBuffer.append("<connections>" + EOL);
        for (Node node : nodes) {
            Map<String, List<Connection>> outConnections = node.getOutgoingConnections();
            Set<String> keys = outConnections.keySet();
            if (keys.size() == 0) {
                break;
            }
            for (String key : keys) {
                List<Connection> connections = outConnections.get(key);
                for (Connection connection : connections) {
                    stringBuffer.append("    <connection from=\"" + node.getId() + "\" to=\"" + connection.getTo().getId() + "\" />" + EOL);
                }
            }
        }
        if (!generatedConnections.equals("")) {
            stringBuffer.append("<!-- Generated Connection for suggested nodes -->" + EOL);
            stringBuffer.append(generatedConnections + EOL);
            stringBuffer.append("<!-- END - Generated Connection for suggested nodes -->" + EOL);
        }
        stringBuffer.append("</connections>" + EOL);
    }

    private static String suggestJoinNode(Node node, StringBuffer stringBuffer) {
        String resultGeneratedConnection = "";
        Set<String> incomingConnectionsTypes = node.getIncomingConnections().keySet();
        //Probably we are inside a composite node or in a disconected node (??)
        if (incomingConnectionsTypes.size()== 0){
            return "";
        }
        String firstKey = incomingConnectionsTypes.iterator().next();
        boolean suggestJoinNode = false;
        if (incomingConnectionsTypes.size() > 1) {
            suggestJoinNode = true;
        } else if (incomingConnectionsTypes.size() == 1 && node.getIncomingConnections().get(firstKey).size() > 1) {
            suggestJoinNode = true;
        }
        if (suggestJoinNode) {
            stringBuffer.append("<!-- This is a suggested Join Node -->" + EOL);
            stringBuffer.append("<join id=\"" + (suggestedNodeId) + "\" name=\"Join XOR - "+suggestedNodeId+"\" type=\"2\" />" + EOL);

            for (String key : incomingConnectionsTypes) {
                Iterator<Connection> itConnections = node.getIncomingConnections(key).iterator();
                long fromNodeId = 0;
                while (itConnections.hasNext()) {
                    Connection connection = itConnections.next();
                    Node fromNode = connection.getFrom();

                    fromNodeId = connection.getTo().getId();
                    if(fromNode instanceof org.drools.jpdl.core.node.State){
                        stringBuffer.append("<!-- Take a look at the State Node that is pointing here, " +
                                "you will need to change the constraint for signal it to the new JoinNode id -->" + EOL);
                        stringBuffer.append("<!-- in node id: "+fromNode.getId()+ " / name: "+fromNode.getName()+" -->" + EOL);
                        stringBuffer.append("<!-- you should change the fromId ("+fromNodeId+") attribute to: "+suggestedNodeId+"-->" + EOL);
                        stringBuffer.append("<!-- you can also change the name for something that reference the JoinNode -->" + EOL);
                    }
                    fromNode.getOutgoingConnections(key).remove(connection);
                    resultGeneratedConnection += "  <connection from=\"" + fromNode.getId() + "\" to=\"" + suggestedNodeId + "\" />\n";
                }
                resultGeneratedConnection += "  <connection from=\"" + suggestedNodeId + "\" to=\"" + fromNodeId + "\" />\n";
            }
            stringBuffer.append("<!-- END - This is a suggested Join Node -->" + EOL);
            suggestedNodeId++;
        }
        return resultGeneratedConnection;
    }
    private static String suggestSplitNode(Node node, StringBuffer stringBuffer) {
        String resultGeneratedConnection = "";
        Set<String> outgoingConnectionsTypes = node.getOutgoingConnections().keySet();
        //Probably we are inside a composite node or in a disconected node (??)
        if (outgoingConnectionsTypes.size()== 0) {
            return "";
        }
        String firstKey = outgoingConnectionsTypes.iterator().next();
        boolean suggestSplitNode = false;
        if (outgoingConnectionsTypes.size() > 1) {
            suggestSplitNode = true;
        } else if (outgoingConnectionsTypes.size() == 1 && node.getOutgoingConnections().get(firstKey).size() > 1) {
            suggestSplitNode = true;
        }
        if (suggestSplitNode){
            stringBuffer.append("<!-- This is a suggested Split Node -->" + EOL);
            stringBuffer.append("<split id=\"" + (suggestedNodeId) + "\" name=\"Split XOR - "+suggestedNodeId+"\" type=\"2\" >" + EOL);
            stringBuffer.append("    <constraints>" + EOL);
            Set<String> keys = node.getOutgoingConnections().keySet();
            for(String key: keys){
                for (Connection connection: node.getOutgoingConnections(key)) {
                    stringBuffer.append("        <constraint toNodeId=\""+connection.getTo().getId()+"\" name=\"signalTo"+connection.getTo().getName()+
                            "\" toType=\"DROOLS_DEFAULT\" type=\"rule\" dialect=\"java\" >" + EOL);
                    stringBuffer.append("        </constraint>" + EOL);
                    //System.out.println("            "+"There is no way to get the conditions in each leavingTransition (depracated since 3.2 - http://docs.jboss.com/jbpm/v3.2/userguide/html_single/#condition.element)");
                    //System.out.println("            "+"There is no way to access the decision expresion or the decision delegation class through the APIs");
                }
            }
            stringBuffer.append("        </constraint>" + EOL);
            stringBuffer.append("</split>" + EOL);
            stringBuffer.append("<!-- END - This is a suggested Split Node -->" + EOL);
            List<Connection> removeConnections = new ArrayList<Connection>();
            for (String key : outgoingConnectionsTypes) {
                Iterator<Connection> itConnections = node.getOutgoingConnections(key).iterator();
                while (itConnections.hasNext()) {
                    Connection connection = itConnections.next();
                    Node toNode = connection.getTo();
                    removeConnections.add(connection);
                    resultGeneratedConnection += "  <connection from=\"" + suggestedNodeId + "\" to=\"" + toNode.getId() + "\" />\n";
                }
            }
            resultGeneratedConnection += "  <connection from=\"" + node.getId() + "\" to=\"" + suggestedNodeId + "\" />\n";
            suggestedNodeId++;
            for(Connection conn : removeConnections){
                node.getOutgoingConnections(conn.getFromType()).remove(conn);
            }

        }
        return resultGeneratedConnection;
    }

    private static void printInPortsAndOutPorts(Node[] toArray, StringBuffer stringBuffer) {
        int lastNode = 0;
        if (toArray.length > 1){
            lastNode = toArray.length -1;
        }
        stringBuffer.append("<in-ports>" + EOL);
        stringBuffer.append("    <in-port type=\"DROOLS_DEFAULT\" nodeId=\""+toArray[0].getId()+"\" nodeInType=\"DROOLS_DEFAULT\" />" + EOL);
        stringBuffer.append("</in-ports>" + EOL);
        stringBuffer.append("<out-ports>" + EOL);
        stringBuffer.append("    <out-port type=\"DROOLS_DEFAULT\" nodeId=\""+toArray[lastNode].getId()+"\" nodeOutType=\"DROOLS_DEFAULT\" />" + EOL);
        stringBuffer.append("</out-ports>" + EOL);
    }

}
