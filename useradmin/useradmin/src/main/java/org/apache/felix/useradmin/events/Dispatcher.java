/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.felix.useradmin.events;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.useradmin.EventDispatcher;
import org.apache.felix.useradmin.ServiceUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdminEvent;

@Component(service=EventDispatcher.class)
public class Dispatcher implements EventDispatcher {

	@Reference
	private ComponentServiceObjects<EventAdmin> eventAdmin;
	
	@Override
	public void deliverEventSynchronously(UserAdminEvent event) {
		ServiceUtils.useService(eventAdmin, new PostEventTask(event));
	}
	
	private static class PostEventTask implements ServiceUtils.Consumer<EventAdmin> {
    	
   	 /**
        * Converts a given {@link UserAdminEvent} to a {@link Event} that can be
        * dispatched through the {@link EventAdmin} service.
        * 
        * @param event
        *            the event to convert, cannot be <code>null</code>.
        * @return a new {@link Event} instance containing the same set of
        *         information as the given event, never <code>null</code>.
        */
       private static Event convertEvent(UserAdminEvent event) {
           String topic = ServiceUtils.getTopicName(event.getType());
           Role role = event.getRole();
           ServiceReference serviceRef = event.getServiceReference();

           final Map<String, Object> props = new HashMap<>();
           props.put(EventConstants.EVENT_TOPIC, ServiceUtils.TOPIC_BASE.concat(topic));
           props.put(EventConstants.EVENT, event);
           props.put("role", role);
           props.put("role.name", role.getName());
           props.put("role.type", new Integer(role.getType()));
           if (serviceRef != null) {
               props.put(EventConstants.SERVICE, serviceRef);
               Object property;
               
               property = serviceRef.getProperty(Constants.SERVICE_ID);
               if (property != null) {
                   props.put(EventConstants.SERVICE_ID, property);
               }
               property = serviceRef.getProperty(Constants.OBJECTCLASS);
               if (property != null) {
                   props.put(EventConstants.SERVICE_OBJECTCLASS, property);
               }
               property = serviceRef.getProperty(Constants.SERVICE_PID);
               if (property != null) {
                   props.put(EventConstants.SERVICE_PID, property);
               }
           }

           return new Event(topic, props);
           
	    }
	   	
	   	private final Event event;
	   	
	   	public PostEventTask(UserAdminEvent event) {
	   		this.event = convertEvent(event);
	   	}

		@Override
		public void accept(EventAdmin eventAdmin) {
			eventAdmin.postEvent(event);
		}
	}
}
