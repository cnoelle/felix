package org.apache.felix.useradmin.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.useradmin.EventDispatcher;
import org.apache.felix.useradmin.ServiceUtils;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;

class ListenerDispatch implements EventDispatcher {
	
	private final AtomicReference<ComponentServiceObjects<?>> loggerRef;
	
	private final Collection<ComponentServiceObjects<UserAdminListener>> listeners 
		= Collections.newSetFromMap(new ConcurrentHashMap<ComponentServiceObjects<UserAdminListener>, Boolean>(8));

	public ListenerDispatch(AtomicReference<ComponentServiceObjects<?>> loggerRef) {
		this.loggerRef = loggerRef;
	}
	
	void addListener(ComponentServiceObjects<UserAdminListener> listener) {
		listeners.add(listener);
	}

	void removeListener(ComponentServiceObjects<UserAdminListener> listener) {
		listeners.remove(listener);
	}

	@Override
	public void deliverEventSynchronously(UserAdminEvent event) {
		// Synchronously call all UserAdminListeners to deliver the event...
    	final RoleChangedTask roleChanged = new RoleChangedTask(event);
    	for (ComponentServiceObjects<UserAdminListener> listener : listeners) {
    		try {
    			ServiceUtils.useService(listener, roleChanged);
    		} catch (Throwable e) { // don't trust external code
    			LogUtil.doLog(loggerRef, "Exception in listener", e, 2);
    		}
    	}
	}
	
	private static class RoleChangedTask implements ServiceUtils.Consumer<UserAdminListener> {

    	private final UserAdminEvent event;
    	
    	public RoleChangedTask(UserAdminEvent event) {
    		this.event = event;
		}
    	
		@Override
		public void accept(UserAdminListener listener) {
			listener.roleChanged(event);
		}
    	
    }

}
