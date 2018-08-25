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
package org.apache.felix.useradmin.impl;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.felix.useradmin.EventDispatcher;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.useradmin.UserAdminEvent;

/**
 * Provides an event dispatcher for delivering {@link UserAdminEvent}s asynchronously. 
 */
final class EventDispatcherImpl implements Runnable {
    
	private final Set<EventDispatcher> m_dispatchers;
    private final BlockingQueue<Object> m_eventQueue;
    private final Thread m_backgroundThread;

    /**
     * Creates a new {@link EventDispatcherImpl} instance, and starts a background thread to deliver all events.
     * 
     * 
     */
    EventDispatcherImpl(Set<EventDispatcher> dispatchers) {
    	this.m_dispatchers = dispatchers;
        m_eventQueue = new LinkedBlockingQueue<>();
        m_backgroundThread = new Thread(this, "UserAdmin event dispatcher");
    }

    /**
     * Dispatches a given event for asynchronous delivery to all interested listeners, 
     * including those using the {@link EventAdmin} service.
     * <p>
     * This method will perform a best-effort to dispatch the event to all listeners, i.e., 
     * there is no guarantee that the listeners will actually obtain the event, nor any
     * notification is given in case delivery fails.
     * </p>
     * 
     * @param event the event to dispatch, cannot be <code>null</code>.
     * @throws IllegalStateException in case this dispatcher is already stopped.
     */
    public void dispatch(UserAdminEvent event) {
        if (!isRunning()) {
            return;
        }

        try {
            m_eventQueue.put(event);
        } catch (InterruptedException e) {
            // Restore interrupt flag...
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Starts this event dispatcher, allowing it to pick up events and deliver them.
     */
    public void start() {
        if (!isRunning()) {
            m_backgroundThread.start();
        }
    }

    /**
     * Signals this event dispatcher to stop its work and clean up all running threads.
     */
    public void stop() {
        if (!isRunning()) {
            return;
        }

        // Add poison object to queue to let the background thread terminate...
        m_eventQueue.add(EventDispatcherImpl.this);

        try {
            m_backgroundThread.join(2000);
        } catch (InterruptedException e) {
        	Thread.currentThread().interrupt();
            // We're already stopping; so don't bother... 
        }
    }

    /**
     * Returns whether or not the background thread is running.
     * 
     * @return <code>true</code> if the background thread is running (alive), <code>false</code> otherwise.
     */
    final boolean isRunning() {
        return m_backgroundThread.isAlive();
    }
    
    /**
     * Provides the main event loop, which waits until an event is enqueued in order 
     * to deliver it to any interested listener.
     */
    public void run() {
        try {
            while (true) {
                // Blocks until a event is dispatched...
                Object event = m_eventQueue.take();

                if (event instanceof UserAdminEvent) {
                    // Got a "normal" user admin event; lets dispatch it further...
                    deliverEventSynchronously((UserAdminEvent) event);
                } else {
                    // Got a "poison" object; this means we must stop running...
                    return;
                }
            }
        } catch (InterruptedException e) {
            // Restore interrupt flag, and terminate thread...
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Delivers the given event synchronously to all interested listeners.
     * 
     * @param event the event to deliver, cannot be <code>null</code>.
     */
    private void deliverEventSynchronously(UserAdminEvent event) {
    	for (EventDispatcher dispatcher : m_dispatchers) {
    		dispatcher.deliverEventSynchronously(event);
    	}
    }
    


}
