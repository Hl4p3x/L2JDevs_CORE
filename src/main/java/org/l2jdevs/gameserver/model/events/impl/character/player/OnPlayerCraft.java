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
package org.l2jdevs.gameserver.model.events.impl.character.player;

import org.l2jdevs.gameserver.model.L2RecipeList;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.events.EventType;
import org.l2jdevs.gameserver.model.events.impl.IBaseEvent;

/**
 * An instantly executed event when L2PcInstance craft.
 * @author U3Games
 */
public class OnPlayerCraft implements IBaseEvent
{
	private final L2PcInstance _activeChar;
	private final L2PcInstance _target;
	private final L2RecipeList _recipe;
	private final boolean _success;
	
	public OnPlayerCraft(L2PcInstance activeChar, L2PcInstance target, L2RecipeList recipe, boolean success)
	{
		_activeChar = activeChar;
		_target = target;
		_recipe = recipe;
		_success = success;
	}
	
	public L2PcInstance getActiveChar()
	{
		return _activeChar;
	}
	
	public L2RecipeList getRecipe()
	{
		return _recipe;
	}
	
	public boolean getSuccess()
	{
		return _success;
	}
	
	public L2PcInstance getTarget()
	{
		return _target;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_CRAFT;
	}
}