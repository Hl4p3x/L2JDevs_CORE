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
package org.l2jdevs.gameserver.network.clientpackets;

import org.l2jdevs.gameserver.model.PartyMatchRoom;
import org.l2jdevs.gameserver.model.PartyMatchRoomList;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Gnacik
 */
public class RequestDismissPartyRoom extends L2GameClientPacket
{
	private static final String _C__D0_0A_REQUESTDISMISSPARTYROOM = "[C] D0:0A RequestDismissPartyRoom";
	
	private int _roomid;
	@SuppressWarnings("unused")
	private int _data2;
	
	@Override
	public String getType()
	{
		return _C__D0_0A_REQUESTDISMISSPARTYROOM;
	}
	
	@Override
	protected void readImpl()
	{
		_roomid = readD();
		_data2 = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance _activeChar = getClient().getActiveChar();
		
		if (_activeChar == null)
		{
			return;
		}
		
		PartyMatchRoom _room = PartyMatchRoomList.getInstance().getRoom(_roomid);
		
		if (_room == null)
		{
			return;
		}
		
		PartyMatchRoomList.getInstance().deleteRoom(_roomid);
	}
	
}
