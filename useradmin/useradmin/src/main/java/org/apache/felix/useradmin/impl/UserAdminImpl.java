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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.useradmin.EventDispatcher;
import org.apache.felix.useradmin.RoleRepositoryStore;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;

/**
 * Provides the implementation for {@link UserAdmin}.
 */
// TODO configuration?
@Component(service=UserAdmin.class)
public class UserAdminImpl implements UserAdmin, RoleChangeListener 
//		,ServiceFactory 
						{
	@Reference
	private RoleRepositoryStore m_roleRepositoryStore;
	
	private final Set<EventDispatcher> m_dispatchers = 
				Collections.newSetFromMap(new ConcurrentHashMap<EventDispatcher,Boolean>());
	
	// using this mechanism in order to avoid mandatory EventAdmin imports
	@Reference(
			service=EventDispatcher.class,
			cardinality=ReferenceCardinality.MULTIPLE,
			policy=ReferencePolicy.DYNAMIC,
			policyOption=ReferencePolicyOption.GREEDY,
			bind="setDispatcher",
			unbind="removeDispatcher"
	)
	protected void setDispatcher(EventDispatcher eventAdmin) {
		m_dispatchers.add(eventAdmin);
	}
	
	protected void removeDispatcher(EventDispatcher eventAdmin) {
		m_dispatchers.remove(eventAdmin);
	}
	
	@Reference(
			service=UserAdminListener.class,
			policy=ReferencePolicy.DYNAMIC,
			cardinality=ReferenceCardinality.MULTIPLE,
			bind="addUserAdminListener",
			unbind="removeUserAdminListener"
	)
	protected void addUserAdminListener(ComponentServiceObjects<UserAdminListener> listener) {
		m_listenerDispatch.addListener(listener);
	}
	
	protected void removeUserAdminListener(ComponentServiceObjects<UserAdminListener> listener) {
		m_listenerDispatch.removeListener(listener);
	}
	
	// LogService: optional dependency
	private final AtomicReference<ComponentServiceObjects<?>> loggerRef = new AtomicReference<ComponentServiceObjects<?>>(null);
	
	@Reference(
			service=org.osgi.service.log.LogService.class,
			policy=ReferencePolicy.DYNAMIC,
			policyOption=ReferencePolicyOption.GREEDY,
			cardinality=ReferenceCardinality.OPTIONAL,
			bind="addLogger",
			unbind="removeLogger"
	)
	protected void addLogger(ComponentServiceObjects<?> logger) {
		loggerRef.set(logger);
	}
	
	protected void removeLogger(ComponentServiceObjects<?> logger) {
		loggerRef.compareAndSet(logger, null);
	}
	
	private final ListenerDispatch m_listenerDispatch = new ListenerDispatch(loggerRef); 
    private EventDispatcherImpl m_eventDispatcher;
    private ServiceReference<UserAdmin> m_serviceRef;
    private RoleRepository m_roleRepository;
    
    {
    	m_dispatchers.add(m_listenerDispatch);
    }
    
    @SuppressWarnings("unchecked")
	@Activate
    protected void activate(ComponentContext ctx) {
    	m_serviceRef = (ServiceReference<UserAdmin>) ctx.getServiceReference();
    	if (m_serviceRef == null)
    		throw new ComponentException("Service reference is null");
    	
    	m_eventDispatcher = new EventDispatcherImpl(m_dispatchers);
    	m_roleRepository = new RoleRepository(m_roleRepositoryStore);
    	m_roleRepository.addRoleChangeListener(this);
    }
    
    @Deactivate
    protected void deactivate() {
    	if (m_eventDispatcher != null)
    		m_eventDispatcher.stop();
    }
    
    /**
     * {@inheritDoc}
     */
    public Role createRole(String name, int type) {
        return m_roleRepository.addRole(name, type);
    }

    /**
     * {@inheritDoc}
     */
    public Authorization getAuthorization(User user) {
        return new AuthorizationImpl(user, m_roleRepository);
    }

    /**
     * {@inheritDoc}
     */
    public Role getRole(String name) {
        return m_roleRepository.getRoleByName(name);
    }

    /**
     * {@inheritDoc}
     */
    public Role[] getRoles(String filter) throws InvalidSyntaxException {
        // Do a sanity check on the given filter...
        if (filter != null && !"".equals(filter.trim())) {
            FrameworkUtil.createFilter(filter);
        }

        List roles = m_roleRepository.getRoles(filter);
        if (roles.isEmpty()) {
            return null;
        }
        return (Role[]) roles.toArray(new Role[roles.size()]);
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>Overridden in order to get hold of our service reference -> not nice... </p>
     */
    /*
    public Object getService(Bundle bundle, ServiceRegistration registration) {
        m_serviceRef = registration.getReference();
        return this;
    }
    */

    /**
     * {@inheritDoc}
     */
    public User getUser(String key, String value) {
        User result = null;
        List roles = m_roleRepository.getRoles(key, value);
        if (roles.size() == 1) {
            Role foundRole = (Role) roles.get(0);
            if (foundRole.getType() == Role.USER) {
                result = (User) foundRole;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void propertyAdded(Role role, Object key, Object value) {
        m_eventDispatcher.dispatch(createUserAdminEvent(UserAdminEvent.ROLE_CHANGED, role));
    }
    
    /**
     * {@inheritDoc}
     */
    public void propertyChanged(Role role, Object key, Object oldValue, Object newValue) {
        m_eventDispatcher.dispatch(createUserAdminEvent(UserAdminEvent.ROLE_CHANGED, role));
    }

    /**
     * {@inheritDoc}
     */
    public void propertyRemoved(Role role, Object key) {
        m_eventDispatcher.dispatch(createUserAdminEvent(UserAdminEvent.ROLE_CHANGED, role));
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean removeRole(String name) {
        return m_roleRepository.removeRole(name);
    }

    /**
     * {@inheritDoc}
     */
    public void roleAdded(Role role) {
        m_eventDispatcher.dispatch(createUserAdminEvent(UserAdminEvent.ROLE_CREATED, role));
    }

    /**
     * {@inheritDoc}
     */
    public void roleRemoved(Role role) {
        m_eventDispatcher.dispatch(createUserAdminEvent(UserAdminEvent.ROLE_REMOVED, role));
    }

    /**
     * {@inheritDoc}
     */
    /*
    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        // Nop; we leave the service as-is...
    }
    */

    /**
     * Creates a new {@link UserAdminEvent} instance for the given type and role.
     * 
     * @param type the type of event to create;
     * @param role the role to create the event for.
     * @return a new {@link UserAdminEvent} instance, never <code>null</code>.
     */
    private UserAdminEvent createUserAdminEvent(int type, Role role) {
        return new UserAdminEvent(m_serviceRef, type, role);
    }
    
}
