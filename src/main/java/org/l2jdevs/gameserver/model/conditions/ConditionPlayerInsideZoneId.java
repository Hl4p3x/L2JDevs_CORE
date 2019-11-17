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
package org.l2jdevs.gameserver.model.conditions;

import java.util.List;

import org.l2jdevs.gameserver.instancemanager.ZoneManager;
import org.l2jdevs.gameserver.model.actor.L2Character;
import org.l2jdevs.gameserver.model.items.L2Item;
import org.l2jdevs.gameserver.model.skills.Skill;
import org.l2jdevs.gameserver.model.zone.L2ZoneType;

/**
 * @author UnAfraid
 */
public class ConditionPlayerInsideZoneId extends Condition
{
	private final List<Integer> _zones;
	
	public ConditionPlayerInsideZoneId(List<Integer> zones)
	{
		_zones = zones;
	}
	
	@Override
	public boolean testImpl(L2Character effector, L2Character effected, Skill skill, L2Item item)
	{
		if (effector.getActingPlayer() == null)
		{
			return false;
		}
		
		for (L2ZoneType zone : ZoneManager.getInstance().getZones(effector))
		{
			if (_zones.contains(zone.getId()))
			{
				return true;
			}
		}
		return false;
	}
}
