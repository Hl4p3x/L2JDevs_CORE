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
package org.l2jdevs.gameserver.model.events.impl.item;

import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.events.EventType;
import org.l2jdevs.gameserver.model.events.impl.IBaseEvent;
import org.l2jdevs.gameserver.model.items.instance.L2ItemInstance;

/**
 * @author UnAfraid
 */
public class OnItemBypassEvent implements IBaseEvent
{
	private final L2ItemInstance _item;
	private final L2PcInstance _activeChar;
	private final String _event;
	
	public OnItemBypassEvent(L2ItemInstance item, L2PcInstance activeChar, String event)
	{
		_item = item;
		_activeChar = activeChar;
		_event = event;
	}
	
	public L2PcInstance getActiveChar()
	{
		return _activeChar;
	}
	
	public String getEvent()
	{
		return _event;
	}
	
	public L2ItemInstance getItem()
	{
		return _item;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_ITEM_BYPASS_EVENT;
	}
	
}
