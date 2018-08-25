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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class UserAdminConfigImpl {

	private static final long DEFAULT_WRITE_DELAY_VALUE = 2000; // millis
	private static final String KEY_WRITE_DISABLED = "background.write.disabled";
    private static final String KEY_WRITE_DELAY_VALUE = "background.write.delay.value";
    private static final String KEY_WRITE_DELAY_TIMEUNIT = "background.write.delay.timeunit";
    private static final String KEY_FILENAME = "filename";
    private static final String PREFIX = RoleRepositoryFileStore.PID.concat(".");
//
//    private static final String FILE_NAME = "ua_repo.dat";
	
	private final boolean backgroundWriteDisabled;
	private final long writeDelayValue;
	private final Path baseFolder;
	private final TimeUnit writeDelayTimeunit;
	
	UserAdminConfigImpl(boolean backgroundWriteDisabled, long writeDelayValue, String writeDelayTimeunit, String baseFolder) {
		this.backgroundWriteDisabled = backgroundWriteDisabled;
		this.writeDelayValue = writeDelayValue >= 0 ? writeDelayValue : 0;
		TimeUnit unit = null;
		try {
			unit = TimeUnit.valueOf(writeDelayTimeunit.toUpperCase());
		} catch (IllegalArgumentException | NullPointerException e) {}
		this.writeDelayTimeunit = unit != null ? unit : TimeUnit.MILLISECONDS;
		this.baseFolder = baseFolder == null ? null : Paths.get(baseFolder);
	}
	
	UserAdminConfigImpl(UserAdminFilestoreConfig cfg) {
		this(cfg.backgroundWriteDisabled(), cfg.writeDelayValue(), cfg.writeDelayTimeunit(), cfg.baseFolder());
	}
	
	public static UserAdminConfigImpl getDefault() {
		return new UserAdminConfigImpl(
				Boolean.getBoolean(PREFIX.concat(KEY_WRITE_DISABLED)),
				Long.getLong(PREFIX.concat(KEY_WRITE_DELAY_VALUE), DEFAULT_WRITE_DELAY_VALUE),
				System.getProperty(PREFIX.concat(KEY_WRITE_DELAY_TIMEUNIT), "MILLISECONDS"),
				System.getProperty(PREFIX.concat(KEY_FILENAME)));
	}
	
	public static UserAdminConfigImpl getDefault(String folder) {
		return new UserAdminConfigImpl(
				Boolean.getBoolean(PREFIX.concat(KEY_WRITE_DISABLED)),
				Long.getLong(PREFIX.concat(KEY_WRITE_DELAY_VALUE), DEFAULT_WRITE_DELAY_VALUE),
				System.getProperty(PREFIX.concat(KEY_WRITE_DELAY_TIMEUNIT), "MILLISECONDS"),
				System.getProperty(folder));
	}

	public boolean isBackgroundWriteDisabled() {
		return backgroundWriteDisabled;
	}

	public long getWriteDelayValue() {
		return writeDelayValue;
	}

	public TimeUnit getWriteDelayTimeunit() {
		return writeDelayTimeunit;
	}

	public Path getBaseFolder() {
		return baseFolder;
	}
	
}
