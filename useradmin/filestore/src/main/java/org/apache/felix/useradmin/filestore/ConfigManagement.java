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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

@Component(
		service= ConfigManagement.class,
		configurationPid=RoleRepositoryFileStore.PID, // TODO properties class?
		configurationPolicy=ConfigurationPolicy.OPTIONAL
)
@Designate(ocd=UserAdminFilestoreConfig.class)
public class ConfigManagement {
	
	private volatile UserAdminConfigImpl config;
	private volatile Path dataFile;

	@Activate
	@Modified
	protected void modified(final BundleContext ctx, final UserAdminFilestoreConfig config) throws IOException {
		final String newBaseStr = config == null ? null : config.baseFolder();
		final Path newBase;
		try {
			newBase = newBaseStr == null ? null : Paths.get(newBaseStr); 
		} catch (InvalidPathException e) {
			throw new ComponentException("Invalid path configured: " + newBaseStr,e);
		}
		if (this.config == null) {
			this.config = config == null ? UserAdminConfigImpl.getDefault() : new UserAdminConfigImpl(config);
			this.dataFile = getDataFile(ctx, newBase);
			return;
		}
		final Path oldBase = this.config.getBaseFolder();
		if (!Objects.equals(newBase, oldBase)) {
			// transfer storage file!
			final Path oldFile = getDataFile(ctx, oldBase);
			if (!Files.isRegularFile(oldFile))
				return;
			final Path newFile = getDataFile(ctx, newBase);
			Files.createDirectories(newFile.getParent());
			Files.copy(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING);
			this.dataFile = newFile;
		}
		this.config = config == null ? UserAdminConfigImpl.getDefault() : new UserAdminConfigImpl(config);
	}
	
	UserAdminConfigImpl getConfig() {
		return config;
	}

	Path getDataFile() {
		return dataFile;
	}
	
	static Path getDataFile(final BundleContext ctx, final Path baseFolder) {
		if (baseFolder == null)
			return ctx.getDataFile(RoleRepositoryFileStore.FILE_NAME).toPath();
		else
			return baseFolder.resolve(RoleRepositoryFileStore.FILE_NAME);
	}

	
	
}
