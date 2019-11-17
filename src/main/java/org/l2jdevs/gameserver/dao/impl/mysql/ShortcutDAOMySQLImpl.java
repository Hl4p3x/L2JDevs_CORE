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
package org.l2jdevs.gameserver.dao.impl.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.l2jdevs.commons.database.pool.impl.ConnectionFactory;
import org.l2jdevs.gameserver.dao.ShortcutDAO;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;

/**
 * Shortcut DAO MySQL implementation.
 * @author Zoey76
 */
public class ShortcutDAOMySQLImpl implements ShortcutDAO
{
	private static final Logger LOG = LoggerFactory.getLogger(ShortcutDAOMySQLImpl.class);
	
	private static final String DELETE = "DELETE FROM character_shortcuts WHERE charId=? AND class_index=?";
	
	@Override
	public boolean delete(L2PcInstance player, int classIndex)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE))
		{
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, classIndex);
			ps.execute();
		}
		catch (Exception e)
		{
			LOG.error("Could not modify sub class for {} to class index {}, {}", player, classIndex, e);
			return false;
		}
		return true;
	}
}
