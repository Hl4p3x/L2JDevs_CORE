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
package org.l2jdevs.gameserver.taskmanager.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.l2jdevs.commons.database.pool.impl.ConnectionFactory;
import org.l2jdevs.gameserver.model.L2World;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.network.serverpackets.ExNevitAdventTimeChange;
import org.l2jdevs.gameserver.taskmanager.Task;
import org.l2jdevs.gameserver.taskmanager.TaskManager;
import org.l2jdevs.gameserver.taskmanager.TaskManager.ExecutedTask;
import org.l2jdevs.gameserver.taskmanager.TaskTypes;

/**
 * @author Janiko
 */
public class TaskNevit extends Task
{
	private static final String NAME = "nevit_system";
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", "06:30:00", "");
	}
	
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE character_variables SET val=? WHERE var='hunting_time'"))
		{
			ps.setInt(1, 0); // Refuel-reset hunting bonus time
			ps.execute();
		}
		catch (Exception e)
		{
			_log.warning(getClass().getSimpleName() + ": Could not reset Nevit System: " + e);
		}
		
		for (L2PcInstance player : L2World.getInstance().getPlayers())
		{
			if ((player != null) && player.isOnline() && !player.isInOfflineMode())
			{
				player.getNevitSystem().setAdventTime(0); // Refuel-reset hunting bonus time
				player.sendPacket(new ExNevitAdventTimeChange(player.getNevitSystem().getAdventTime(), true));
			}
		}
		
		_log.info("Nevit system reseted.");
	}
}