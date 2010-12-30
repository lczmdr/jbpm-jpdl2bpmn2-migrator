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

package org.drools.jpdl.core.node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.definition.process.Connection;
import org.drools.process.core.context.exception.ExceptionScope;
import org.drools.workflow.core.impl.NodeImpl;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.ExceptionHandler;

public class JpdlNode extends NodeImpl {

	private static final long serialVersionUID = 510l;

	private Map<String, Event> events;
	private Action action;
	
	public JpdlNode() {
		setContext(ExceptionScope.EXCEPTION_SCOPE, new ExceptionScope());
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}
	
	public Connection getOutgoingConnection(String type) {
		List<Connection> connections = getOutgoingConnections(type);
		if (connections == null || connections.size() == 0) {
			return null;
		}
		return connections.get(0);
	}

	public Map<String, Event> getEvents() {
		return events;
	}

	public boolean hasEvents() {
		return (events != null) && (events.size() > 0);
	}

	public Event getEvent(String eventType) {
		Event event = null;
		if (events != null) {
			event = (Event) events.get(eventType);
		}
		return event;
	}

	public boolean hasEvent(String eventType) {
		boolean hasEvent = false;
		if (events != null) {
			hasEvent = events.containsKey(eventType);
		}
		return hasEvent;
	}

	public Event addEvent(Event event) {
		if (event == null) {
			throw new IllegalArgumentException(
				"can't add a null event to a graph element");
		}
		if (event.getEventType() == null) {
			throw new IllegalArgumentException(
				"can't add an event without an eventType to a graph element");
		}
		if (events == null) {
			events = new HashMap<String, Event>();
		}
		events.put(event.getEventType(), event);
		return event;
	}

	public Event removeEvent(Event event) {
		Event removedEvent = null;
		if (event == null) {
			throw new IllegalArgumentException(
				"can't remove a null event from a graph element");
		}
		if (event.getEventType() == null) {
			throw new IllegalArgumentException(
				"can't remove an event without an eventType from a graph element");
		}
		if (events != null) {
			removedEvent = (Event) events.remove(event.getEventType());
		}
		return removedEvent;
	}
	
	public ExceptionScope getExceptionScope() {
		return (ExceptionScope) getContext(ExceptionScope.EXCEPTION_SCOPE);
	}

	public ExceptionHandler addExceptionHandler(ExceptionHandler exceptionHandler) {
		if (exceptionHandler == null) {
			throw new IllegalArgumentException(
				"can't add a null exceptionHandler to a graph element");
		}
		getExceptionScope().setExceptionHandler(
			exceptionHandler.getExceptionClassName(),
			new JpdlExceptionHandler(exceptionHandler));
		return exceptionHandler;
	}

	public void removeExceptionHandler(ExceptionHandler exceptionHandler) {
		if (exceptionHandler == null) {
			throw new IllegalArgumentException(
				"can't remove a null exceptionHandler from an graph element");
		}
		getExceptionScope().removeExceptionHandler(
			exceptionHandler.getExceptionClassName());
	}

	private class JpdlExceptionHandler implements org.drools.process.core.context.exception.ExceptionHandler {

		private ExceptionHandler exceptionHandler;
		private String faultVariable;
		
		private JpdlExceptionHandler(ExceptionHandler exceptionHandler) {
			this.exceptionHandler = exceptionHandler;
		}
		
		public ExceptionHandler getExceptionHandler() {
			return exceptionHandler;
		}
		
		public String getFaultVariable() {
			return faultVariable;
		}
		
		public void setFaultVariable(String faultVariable) {
			this.faultVariable = faultVariable;
		}
		
	}

}
