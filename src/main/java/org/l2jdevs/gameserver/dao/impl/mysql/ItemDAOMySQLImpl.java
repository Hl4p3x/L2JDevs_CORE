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
import java.sql.ResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.l2jdevs.commons.database.pool.impl.ConnectionFactory;
import org.l2jdevs.gameserver.dao.ItemDAO;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;

/**
 * Item DAO MySQL implementation.
 * @author Zoey76
 */
public class ItemDAOMySQLImpl implements ItemDAO
{
	private static final Logger LOG = LoggerFactory.getLogger(ItemDAOMySQLImpl.class);
	
	private static final String SELECT = "SELECT object_id FROM `items` WHERE `owner_id`=? AND (`loc`='PET' OR `loc`='PET_EQUIP') LIMIT 1;";
	
	@Override
	public void loadPetInventory(L2PcInstance player)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				player.setPetInvItems(rs.next() && (rs.getInt("object_id") > 0));
			}
		}
		catch (Exception e)
		{
			LOG.error("Could not check Items in Pet Inventory for {}, {}", player, e);
		}
	}
}
