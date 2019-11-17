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

import org.l2jdevs.gameserver.model.actor.L2Character;
import org.l2jdevs.gameserver.model.items.L2Item;
import org.l2jdevs.gameserver.model.skills.Skill;
import org.l2jdevs.gameserver.util.Util;

/**
 * Exist NPC condition.
 * @author UnAfraid, Zoey76
 */
public class ConditionPlayerRangeFromNpc extends Condition
{
	/** NPC Ids. */
	private final int[] _npcIds;
	/** Radius to check. */
	private final int _radius;
	/** Expected value. */
	private final boolean _val;
	
	public ConditionPlayerRangeFromNpc(int[] npcIds, int radius, boolean val)
	{
		_npcIds = npcIds;
		_radius = radius;
		_val = val;
	}
	
	@Override
	public boolean testImpl(L2Character effector, L2Character effected, Skill skill, L2Item item)
	{
		boolean existNpc = false;
		if ((_npcIds != null) && (_npcIds.length > 0) && (_radius > 0))
		{
			for (L2Character target : effector.getKnownList().getKnownCharactersInRadius(_radius))
			{
				if (target.isNpc() && Util.contains(_npcIds, target.getId()))
				{
					existNpc = true;
					break;
				}
			}
		}
		return existNpc == _val;
	}
}
