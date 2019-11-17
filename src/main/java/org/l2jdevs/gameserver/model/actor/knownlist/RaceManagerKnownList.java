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
package org.l2jdevs.gameserver.model.actor.knownlist;

import org.l2jdevs.gameserver.MonsterRace;
import org.l2jdevs.gameserver.model.L2Object;
import org.l2jdevs.gameserver.model.actor.instance.L2RaceManagerInstance;
import org.l2jdevs.gameserver.network.serverpackets.DeleteObject;

public class RaceManagerKnownList extends NpcKnownList
{
	public RaceManagerKnownList(L2RaceManagerInstance activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public L2RaceManagerInstance getActiveChar()
	{
		return (L2RaceManagerInstance) super.getActiveChar();
	}
	
	@Override
	protected boolean removeKnownObject(L2Object object, boolean forget)
	{
		if (!super.removeKnownObject(object, forget))
		{
			return false;
		}
		
		if (object.isPlayer())
		{
			for (int i = 0; i < 8; i++)
			{
				object.sendPacket(new DeleteObject(MonsterRace.getInstance().getMonsters()[i]));
			}
		}
		
		return true;
	}
}
