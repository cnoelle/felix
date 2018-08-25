package org.apache.felix.useradmin;

import org.osgi.service.useradmin.UserAdminEvent;

/**
 * Internal interface
 */
public interface EventDispatcher {

	void deliverEventSynchronously(UserAdminEvent event);
	
}
