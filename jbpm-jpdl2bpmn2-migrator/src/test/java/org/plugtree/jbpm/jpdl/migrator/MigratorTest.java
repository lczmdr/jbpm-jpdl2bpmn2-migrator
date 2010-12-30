package org.plugtree.jbpm.jpdl.migrator;

import java.io.IOException;

import org.junit.Test;
import org.xml.sax.SAXException;

public class MigratorTest {

    @Test
    public void simpleOutputTest() throws SAXException, IOException{
        JPDL2BPMN2Migrator migrator = new JPDL2BPMN2Migrator();
        String convertion = migrator.convertToBpmn2("processdefinition.xml");
        System.out.println(convertion);
    }
}
