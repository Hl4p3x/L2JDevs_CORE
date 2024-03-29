/*
 * Copyright © 2004-2019 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jdevs.commons.database.pool.impl;

import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.jolbox.bonecp.BoneCPDataSource;
import org.l2jdevs.Config;
import org.l2jdevs.commons.database.pool.IConnectionFactory;

/**
 * BoneCP Connection Factory implementation.<br>
 * <b>Note that this class is not public to prevent external initialization.</b><br>
 * <b>Access it through {@link ConnectionFactory} and proper configuration.</b><br>
 * <b><font color="RED" size="3">Totally BETA and untested feature!</font></b>
 * @author Zoey76
 */
enum BoneCPConnectionFactory implements IConnectionFactory
{
	INSTANCE;
	
	private static final int PARTITION_COUNT = 5;
	
	private final BoneCPDataSource _dataSource;
	
	BoneCPConnectionFactory()
	{
		_dataSource = new BoneCPDataSource();
		_dataSource.setJdbcUrl(Config.DATABASE_URL);
		_dataSource.setUsername(Config.DATABASE_LOGIN);
		_dataSource.setPassword(Config.DATABASE_PASSWORD);
		_dataSource.setPartitionCount(PARTITION_COUNT);
		_dataSource.setMaxConnectionsPerPartition(Config.DATABASE_MAX_CONNECTIONS);
		_dataSource.setIdleConnectionTestPeriod(Config.DATABASE_MAX_IDLE_TIME, TimeUnit.SECONDS);
	}
	
	@Override
	public void close()
	{
		try
		{
			_dataSource.close();
		}
		catch (Exception e)
		{
			LOG.warn("There has been a problem closing the data source!", e);
		}
	}
	
	@Override
	public DataSource getDataSource()
	{
		return _dataSource;
	}
}
