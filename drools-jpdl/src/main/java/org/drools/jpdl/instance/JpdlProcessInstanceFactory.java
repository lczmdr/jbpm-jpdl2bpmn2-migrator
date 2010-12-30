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

package org.drools.jpdl.instance;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.drools.process.instance.AbstractProcessInstanceFactory;
import org.drools.process.instance.ProcessInstance;

public class JpdlProcessInstanceFactory extends AbstractProcessInstanceFactory implements Externalizable {

    private static final long serialVersionUID = 510l;

    public ProcessInstance createProcessInstance() {
        return new JpdlProcessInstance();
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    }

    public void writeExternal(ObjectOutput out) throws IOException {
    }

}
