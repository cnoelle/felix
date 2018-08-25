package org.apache.felix.useradmin;

import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.useradmin.UserAdminEvent;

public class ServiceUtils {
	
	// TODO move to dedicated Constants class? configurable?
	public static final String TOPIC_BASE = "org/osgi/service/useradmin/UserAdmin/";
	
    /**
     * Converts a topic name for the given event-type.
     * 
     * @param type the type of event to get the topic name for.
     * @return a topic name, never <code>null</code>.
     */
    public static String getTopicName(int type) {
        switch (type) {
            case UserAdminEvent.ROLE_CREATED:
                return "ROLE_CREATED";
            case UserAdminEvent.ROLE_CHANGED:
                return "ROLE_CHANGED";
            case UserAdminEvent.ROLE_REMOVED:
                return "ROLE_REMOVED";
            default:
                return null;
        }
    }
	
	public static interface Consumer<S> {
	    	
	    void accept(S s);
	    	
	}

    public static <S> void useService(final ComponentServiceObjects<S> service, final Consumer<S> task) {
    	try {
	    	final S instance = service.getService();
	    	if (instance == null)
	    		return;
	    	try {
	    		task.accept(instance);
	    	} finally {
	    		service.ungetService(instance);
	    	}
    	} catch (IllegalStateException expected) {}
    }
	
}
