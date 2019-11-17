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
package org.l2jdevs.gameserver.model.itemcontainer;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.enums.ItemLocation;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public class PcFreight extends ItemContainer
{
	private final L2PcInstance _owner;
	private final int _ownerId;
	
	public PcFreight(int object_id)
	{
		_owner = null;
		_ownerId = object_id;
		restore();
	}
	
	public PcFreight(L2PcInstance owner)
	{
		_owner = owner;
		_ownerId = owner.getObjectId();
	}
	
	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.FREIGHT;
	}
	
	@Override
	public String getName()
	{
		return "Freight";
	}
	
	@Override
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	
	@Override
	public int getOwnerId()
	{
		return _ownerId;
	}
	
	@Override
	public void refreshWeight()
	{
	}
	
	@Override
	public boolean validateCapacity(long slots)
	{
		int curSlots = _owner == null ? Config.ALT_FREIGHT_SLOTS : Config.ALT_FREIGHT_SLOTS + (int) _owner.getStat().calcStat(Stats.FREIGHT_LIM, 0, null, null);
		return ((getSize() + slots) <= curSlots);
	}
}