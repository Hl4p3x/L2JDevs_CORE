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
package org.l2jdevs.gameserver.model.actor.instance;

import org.l2jdevs.gameserver.ai.L2BoatAI;
import org.l2jdevs.gameserver.enums.InstanceType;
import org.l2jdevs.gameserver.model.Location;
import org.l2jdevs.gameserver.model.actor.L2Vehicle;
import org.l2jdevs.gameserver.model.actor.templates.L2CharTemplate;
import org.l2jdevs.gameserver.network.serverpackets.VehicleDeparture;
import org.l2jdevs.gameserver.network.serverpackets.VehicleInfo;
import org.l2jdevs.gameserver.network.serverpackets.VehicleStarted;

/**
 * @author Maktakien, DS
 */
public class L2BoatInstance extends L2Vehicle
{
	/**
	 * Creates a boat.
	 * @param template the boat template
	 */
	public L2BoatInstance(L2CharTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2BoatInstance);
		setAI(new L2BoatAI(this));
	}
	
	@Override
	public int getId()
	{
		return 0;
	}
	
	@Override
	public boolean isBoat()
	{
		return true;
	}
	
	@Override
	public boolean moveToNextRoutePoint()
	{
		final boolean result = super.moveToNextRoutePoint();
		if (result)
		{
			broadcastPacket(new VehicleDeparture(this));
		}
		
		return result;
	}
	
	@Override
	public void oustPlayer(L2PcInstance player)
	{
		super.oustPlayer(player);
		
		final Location loc = getOustLoc();
		if (player.isOnline())
		{
			player.teleToLocation(loc.getX(), loc.getY(), loc.getZ());
		}
		else
		{
			player.setXYZInvisible(loc.getX(), loc.getY(), loc.getZ()); // disconnects handling
		}
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		activeChar.sendPacket(new VehicleInfo(this));
	}
	
	@Override
	public void stopMove(Location loc, boolean updateKnownObjects)
	{
		super.stopMove(loc, updateKnownObjects);
		
		broadcastPacket(new VehicleStarted(this, 0));
		broadcastPacket(new VehicleInfo(this));
	}
}
