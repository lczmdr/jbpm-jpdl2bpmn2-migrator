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

package org.drools.jpdl.xml;

import org.jbpm.compiler.xml.processes.ActionNodeHandler;
import org.jbpm.compiler.xml.processes.CompositeNodeHandler;
import org.jbpm.compiler.xml.processes.ConnectionHandler;
import org.jbpm.compiler.xml.processes.ConstraintHandler;
import org.jbpm.compiler.xml.processes.EndNodeHandler;
import org.jbpm.compiler.xml.processes.GlobalHandler;
import org.jbpm.compiler.xml.processes.ImportHandler;
import org.jbpm.compiler.xml.processes.InPortHandler;
import org.jbpm.compiler.xml.processes.JoinNodeHandler;
import org.jbpm.compiler.xml.processes.MappingHandler;
import org.jbpm.compiler.xml.processes.MilestoneNodeHandler;
import org.jbpm.compiler.xml.processes.OutPortHandler;
import org.jbpm.compiler.xml.processes.ParameterHandler;
import org.jbpm.compiler.xml.processes.ProcessHandler;
import org.jbpm.compiler.xml.processes.RuleSetNodeHandler;
import org.jbpm.compiler.xml.processes.SplitNodeHandler;
import org.jbpm.compiler.xml.processes.SubProcessNodeHandler;
import org.jbpm.compiler.xml.processes.TimerNodeHandler;
import org.jbpm.compiler.xml.processes.TypeHandler;
import org.jbpm.compiler.xml.processes.ValueHandler;
import org.jbpm.compiler.xml.processes.VariableHandler;
import org.jbpm.compiler.xml.processes.WorkHandler;
import org.jbpm.compiler.xml.processes.WorkItemNodeHandler;
import org.drools.xml.DefaultSemanticModule;
import org.drools.xml.SemanticModule;

public class JpdlProcessSemanticModule extends DefaultSemanticModule implements SemanticModule {    

    public JpdlProcessSemanticModule() {

        super ( "http://drools.org/drools-4.0/process" );

        addHandler( "process-definition",
                           new ProcessHandler() );
        addHandler( "start",
                           new StartStateHandler() );
        addHandler( "end",
                           new EndNodeHandler() );
        addHandler( "action",
                           new ActionNodeHandler() );
        addHandler( "ruleSet",
                           new RuleSetNodeHandler() );
        addHandler( "subProcess",
                           new SubProcessNodeHandler() );
        addHandler( "workItem",
                           new WorkItemNodeHandler() );
        addHandler( "split",
                           new SplitNodeHandler() );
        addHandler( "join",
                           new JoinNodeHandler() );
        addHandler( "milestone",
                           new MilestoneNodeHandler() );
        addHandler( "timer",
                           new TimerNodeHandler() );
        addHandler( "composite",
                           new CompositeNodeHandler() );
        addHandler( "connection",
                           new ConnectionHandler() );
        addHandler( "import",
                           new ImportHandler() );
        addHandler( "global",
                           new GlobalHandler() );        
        addHandler( "variable",
                           new VariableHandler() );        
        addHandler( "type",
                           new TypeHandler() );        
        addHandler( "value",
                           new ValueHandler() );        
        addHandler( "work",
                           new WorkHandler() );        
        addHandler( "parameter",
                           new ParameterHandler() );        
        addHandler( "mapping",
                           new MappingHandler() );        
        addHandler( "constraint",
                           new ConstraintHandler() );        
        addHandler( "in-port",
                           new InPortHandler() );        
        addHandler( "out-port",
                           new OutPortHandler() );        
    }
}
