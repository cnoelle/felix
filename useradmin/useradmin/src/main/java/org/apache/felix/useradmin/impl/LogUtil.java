package org.apache.felix.useradmin.impl;

import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.ComponentServiceObjects;

/**
 * Taking some care to deal with optional LogService dependency
 */
class LogUtil {
	
	@SuppressWarnings({ "unused", "unchecked" })
	static void log(final AtomicReference<ComponentServiceObjects<?>> logRef, final String msg, 
			final Throwable exception, final int level) {
		@SuppressWarnings("rawtypes")
		final ComponentServiceObjects serviceRef = logRef.get();
		if (serviceRef == null)
			return;
		final Object service = serviceRef.getService();
		try {
			doLog(service, msg, exception, level);
		} finally {
			serviceRef.ungetService(service);
		}
	}
	
	// this method should never be called when the log service is not available
	static void doLog(final Object o, final String msg, final Throwable exception, final int level) {
		((org.osgi.service.log.LogService) o).log(level, msg, exception);
	}

}
