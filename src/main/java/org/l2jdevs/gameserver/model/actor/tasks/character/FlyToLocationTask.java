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
package org.l2jdevs.gameserver.model.actor.tasks.character;

import org.l2jdevs.gameserver.model.L2Object;
import org.l2jdevs.gameserver.model.Location;
import org.l2jdevs.gameserver.model.actor.L2Character;
import org.l2jdevs.gameserver.network.serverpackets.FlyToLocation;
import org.l2jdevs.gameserver.network.serverpackets.FlyToLocation.FlyType;

/**
 * Task dedicated to fly a player to the location
 * @author xban1x
 */
public final class FlyToLocationTask implements Runnable
{
	private final L2Character _character;
	private final Location _targetLocation;
	private final FlyType _type;
	
	public FlyToLocationTask(L2Character character, L2Object target, FlyType type)
	{
		_character = character;
		_targetLocation = target.getLocation();
		_type = type;
	}
	
	@Override
	public void run()
	{
		if (_character != null)
		{
			_character.broadcastPacket(new FlyToLocation(_character, _targetLocation, _type));
			_character.setLocation(_targetLocation);
		}
	}
}
