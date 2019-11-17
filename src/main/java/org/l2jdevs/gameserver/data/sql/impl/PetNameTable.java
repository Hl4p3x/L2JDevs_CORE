/*
 * Copyright © 2004-2019 L2JDevs
 * 
 * This file is part of L2JDevs.
 * 
 * L2JDevs is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2JDevs is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jdevs.gameserver.data.sql.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jdevs.Config;
import org.l2jdevs.commons.database.pool.impl.ConnectionFactory;

public class PetNameTable
{
	private static Logger LOGGER = Logger.getLogger(PetNameTable.class.getName());
	
	public static PetNameTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public boolean doesPetNameExist(String name)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT name FROM pets WHERE name=?"))
		{
			ps.setString(1, name);
			try (ResultSet rs = ps.executeQuery())
			{
				return rs.next();
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not check existing petname:" + e.getMessage(), e);
		}
		return false;
	}
	
	public boolean isValidPetName(String name)
	{
		return Config.PET_NAME_TEMPLATE.matcher(name).matches();
	}
	
	private static class SingletonHolder
	{
		protected static final PetNameTable _instance = new PetNameTable();
	}
}
