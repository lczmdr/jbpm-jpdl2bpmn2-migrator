package org.plugtree.jbpm.jpdl.migrator;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.drools.builder.KnowledgeBuilderConfiguration;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.compiler.PackageBuilderConfiguration;
import org.drools.jpdl.EpdlWriter;
import org.drools.jpdl.JpdlParser;
import org.drools.jpdl.core.JpdlProcess;
import org.drools.process.core.validation.ProcessValidationError;
import org.drools.xml.SemanticModules;
import org.jbpm.bpmn2.xml.XmlBPMNProcessDumper;
import org.jbpm.compiler.xml.ProcessSemanticModule;
import org.jbpm.compiler.xml.XmlProcessReader;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.xml.sax.SAXException;

public class JPDL2BPMN2Migrator {

    public String convertToBpmn2(String file) throws SAXException, IOException {
        JpdlParser parser = new JpdlParser();
        JpdlProcess process = parser.loadJpdlProcess(file);
        ProcessValidationError[] errors = parser.getErrors();
        if (errors.length > 0) {
            throw new RuntimeException("error on conversion: " + errors);
        }
        String processConverted = EpdlWriter.write(process);
        KnowledgeBuilderConfiguration conf = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration();
        ((PackageBuilderConfiguration) conf).initSemanticModules();
        ((PackageBuilderConfiguration) conf).addSemanticModule(new ProcessSemanticModule());
        SemanticModules semanticModules = ((PackageBuilderConfiguration) conf).getSemanticModules();
        XmlProcessReader processReader = new XmlProcessReader(semanticModules);
        RuleFlowProcess p = (RuleFlowProcess) processReader.read(new ByteArrayInputStream(processConverted.getBytes()));
        return XmlBPMNProcessDumper.INSTANCE.dump(p);
    }
}
