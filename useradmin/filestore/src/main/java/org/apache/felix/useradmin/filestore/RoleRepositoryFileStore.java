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
package org.apache.felix.useradmin.filestore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.useradmin.RoleRepositoryStore;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;


/**
 * Provides an implementation of {@link RoleRepositoryStore} using Java Serialization.
 */
@Component(service= {RoleRepositoryStore.class, UserAdminListener.class})
public class RoleRepositoryFileStore extends RoleRepositoryMemoryStore implements Runnable, UserAdminListener {

    /** The PID for this service to allow its configuration to be updated. */
    public static final String PID = "org.apache.felix.useradmin.filestore";
    public static final String FILE_NAME = "ua_repo.dat";

    private File m_file;
    private AtomicReference<ResettableTimer> m_timerRef;
    
    @Reference
    private ConfigManagement configService;
    
    private UserAdminConfigImpl config;
    
    @Activate
    protected void activate() {
    	this.config = configService.getConfig();
    	this.m_file = configService.getDataFile().toFile();
        m_timerRef = new AtomicReference<>();
        if (!config.isBackgroundWriteDisabled()) {
            m_timerRef.set(new ResettableTimer(this, config.getWriteDelayValue(), config.getWriteDelayTimeunit()));
        }
    }
    
    public RoleRepositoryFileStore() {}
    
    /*
     * Just for testing 
     */
    RoleRepositoryFileStore(File baseDir, boolean backgroundWrite) {
    	this.config = new UserAdminConfigImpl(!backgroundWrite, 1000, "MILLISECONDS", baseDir.toString());
    	this.m_file = ConfigManagement.getDataFile(null, baseDir.toPath()).toFile();
    	 m_timerRef = new AtomicReference<>();
         if (!config.isBackgroundWriteDisabled()) {
             m_timerRef.set(new ResettableTimer(this, config.getWriteDelayValue(), config.getWriteDelayTimeunit()));
         }
    }
    
    public void roleChanged(UserAdminEvent event) {
        scheduleTask();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Will be called by m_timer!</p>
     */
    public void run() {
        try {
            // Persist everything to disk...
            flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts this store by reading the latest version from disk.
     * 
     * @throws IOException in case of I/O problems retrieving the store.
     */
    public void start() throws IOException {
        m_entries.putAll(retrieve());
    }

    /**
     * Stops this store service.
     */
    public void stop() throws IOException {
        ResettableTimer timer = (ResettableTimer) m_timerRef.get();
        if (timer != null) {
            if (!timer.isShutDown()) {
                // Shutdown and await termination...
                timer.shutDown();
            }
            // Clear reference...
            m_timerRef.compareAndSet(timer, null);
        }

        // Write the latest version to disk...
        flush();
    }

    /**
     * Retrieves the serialized repository from disk.
     * 
     * @return the retrieved repository, never <code>null</code>.
     * @throws IOException in case the retrieval of the repository failed.
     */
    protected Map<String, Role> retrieve() throws IOException {
        InputStream is = null;

        try {
            is = new BufferedInputStream(new FileInputStream(m_file));

            return new RoleRepositorySerializer().deserialize(is);
        } catch (FileNotFoundException exception) {
            // Don't bother; file does not exist...
            return Collections.emptyMap();
        } catch (IOException exception) {
            exception.printStackTrace();
            throw exception;
        } finally {
            closeSafely(is);
        }
    }

    /**
     * Stores the given repository to disk as serialized objects.
     * 
     * @param roleRepository the repository to store, cannot be <code>null</code>.
     * @throws IOException in case storing the repository failed.
     */
    protected void store(Map roleRepository) throws IOException {
        OutputStream os = null;

        try {
            os = new BufferedOutputStream(new FileOutputStream(m_file));

            new RoleRepositorySerializer().serialize(roleRepository, os);
        } finally {
            closeSafely(os);
        }
    }

    /**
     * Closes a given resource, ignoring any exceptions that may come out of this.
     * 
     * @param resource the resource to close, can be <code>null</code>.
     */
    private void closeSafely(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * Flushes the current repository to disk.
     * 
     * @throws IOException in case of problems storing the repository.
     */
    private void flush() throws IOException {
        store(new HashMap(m_entries));
    }

    /**
     * Notifies the background timer to schedule a task for storing the 
     * contents of this store to disk.
     */
    private void scheduleTask() {
        ResettableTimer timer = (ResettableTimer) m_timerRef.get();
        if (timer != null && !timer.isShutDown()) {
            timer.schedule();
        } else {
        	run();
        }
    }
}
