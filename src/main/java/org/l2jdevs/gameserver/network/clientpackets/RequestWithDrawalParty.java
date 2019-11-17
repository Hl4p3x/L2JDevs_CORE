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
package org.l2jdevs.gameserver.network.clientpackets;

import org.l2jdevs.gameserver.datatables.LanguageData;
import org.l2jdevs.gameserver.model.L2Party;
import org.l2jdevs.gameserver.model.L2Party.messageType;
import org.l2jdevs.gameserver.model.PartyMatchRoom;
import org.l2jdevs.gameserver.model.PartyMatchRoomList;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.network.serverpackets.ExClosePartyRoom;
import org.l2jdevs.gameserver.network.serverpackets.ExPartyRoomMember;
import org.l2jdevs.gameserver.network.serverpackets.PartyMatchDetail;

/**
 * This class ...
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestWithDrawalParty extends L2GameClientPacket
{
	private static final String _C__44_REQUESTWITHDRAWALPARTY = "[C] 44 RequestWithDrawalParty";
	
	@Override
	public String getType()
	{
		return _C__44_REQUESTWITHDRAWALPARTY;
	}
	
	@Override
	protected void readImpl()
	{
		// trigger
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		L2Party party = player.getParty();
		
		if (party != null)
		{
			if (party.isInDimensionalRift() && !party.getDimensionalRift().getRevivedAtWaitingRoom().contains(player))
			{
				player.sendMessage(LanguageData.getInstance().getMsg(player, "no_dismiss_player_in_rift"));
			}
			else
			{
				party.removePartyMember(player, messageType.Left);
				
				if (player.isInPartyMatchRoom())
				{
					PartyMatchRoom _room = PartyMatchRoomList.getInstance().getPlayerRoom(player);
					if (_room != null)
					{
						player.sendPacket(new PartyMatchDetail(player, _room));
						player.sendPacket(new ExPartyRoomMember(player, _room, 0));
						player.sendPacket(new ExClosePartyRoom());
						
						_room.deleteMember(player);
					}
					player.setPartyRoom(0);
					// player.setPartyMatching(0);
					player.broadcastUserInfo();
				}
			}
		}
	}
}
