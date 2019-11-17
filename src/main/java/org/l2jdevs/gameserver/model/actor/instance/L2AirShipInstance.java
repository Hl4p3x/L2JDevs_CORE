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
package org.l2jdevs.gameserver.model.actor.instance;

import org.l2jdevs.gameserver.ai.L2AirShipAI;
import org.l2jdevs.gameserver.enums.InstanceType;
import org.l2jdevs.gameserver.instancemanager.AirShipManager;
import org.l2jdevs.gameserver.model.Location;
import org.l2jdevs.gameserver.model.actor.L2Vehicle;
import org.l2jdevs.gameserver.model.actor.templates.L2CharTemplate;
import org.l2jdevs.gameserver.network.serverpackets.ExAirShipInfo;
import org.l2jdevs.gameserver.network.serverpackets.ExGetOffAirShip;
import org.l2jdevs.gameserver.network.serverpackets.ExGetOnAirShip;
import org.l2jdevs.gameserver.network.serverpackets.ExMoveToLocationAirShip;
import org.l2jdevs.gameserver.network.serverpackets.ExStopMoveAirShip;

/**
 * Flying airships. Very similar to Maktakien boats (see L2BoatInstance) but these do fly :P
 * @author DrHouse, DS
 */
public class L2AirShipInstance extends L2Vehicle
{
	public L2AirShipInstance(L2CharTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2AirShipInstance);
		setAI(new L2AirShipAI(this));
	}
	
	@Override
	public boolean addPassenger(L2PcInstance player)
	{
		if (!super.addPassenger(player))
		{
			return false;
		}
		
		player.setVehicle(this);
		player.setInVehiclePosition(new Location(0, 0, 0));
		player.broadcastPacket(new ExGetOnAirShip(player, this));
		player.getKnownList().removeAllKnownObjects();
		player.setXYZ(getX(), getY(), getZ());
		player.revalidateZone(true);
		return true;
	}
	
	@Override
	public boolean deleteMe()
	{
		if (!super.deleteMe())
		{
			return false;
		}
		
		AirShipManager.getInstance().removeAirShip(this);
		return true;
	}
	
	public int getCaptainId()
	{
		return 0;
	}
	
	public int getFuel()
	{
		return 0;
	}
	
	public int getHelmItemId()
	{
		return 0;
	}
	
	public int getHelmObjectId()
	{
		return 0;
	}
	
	@Override
	public int getId()
	{
		return 0;
	}
	
	public int getMaxFuel()
	{
		return 0;
	}
	
	public int getOwnerId()
	{
		return 0;
	}
	
	@Override
	public boolean isAirShip()
	{
		return true;
	}
	
	public boolean isCaptain(L2PcInstance player)
	{
		return false;
	}
	
	public boolean isOwner(L2PcInstance player)
	{
		return false;
	}
	
	@Override
	public boolean moveToNextRoutePoint()
	{
		final boolean result = super.moveToNextRoutePoint();
		if (result)
		{
			broadcastPacket(new ExMoveToLocationAirShip(this));
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
			player.broadcastPacket(new ExGetOffAirShip(player, this, loc.getX(), loc.getY(), loc.getZ()));
			player.getKnownList().removeAllKnownObjects();
			player.setXYZ(loc.getX(), loc.getY(), loc.getZ());
			player.revalidateZone(true);
		}
		else
		{
			player.setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
		}
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		if (isVisibleFor(activeChar))
		{
			activeChar.sendPacket(new ExAirShipInfo(this));
		}
	}
	
	public boolean setCaptain(L2PcInstance player)
	{
		return false;
	}
	
	public void setFuel(int f)
	{
		
	}
	
	public void setMaxFuel(int mf)
	{
		
	}
	
	@Override
	public void stopMove(Location loc, boolean updateKnownObjects)
	{
		super.stopMove(loc, updateKnownObjects);
		
		broadcastPacket(new ExStopMoveAirShip(this));
	}
	
	@Override
	public void updateAbnormalEffect()
	{
		broadcastPacket(new ExAirShipInfo(this));
	}
}