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

import org.l2jdevs.gameserver.model.L2World;
import org.l2jdevs.gameserver.model.PartyMatchRoom;
import org.l2jdevs.gameserver.model.PartyMatchRoomList;
import org.l2jdevs.gameserver.model.PartyMatchWaitingList;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.network.SystemMessageId;
import org.l2jdevs.gameserver.network.serverpackets.ExManagePartyRoomMember;
import org.l2jdevs.gameserver.network.serverpackets.ExPartyRoomMember;
import org.l2jdevs.gameserver.network.serverpackets.PartyMatchDetail;
import org.l2jdevs.gameserver.network.serverpackets.SystemMessage;

/**
 * Format: (ch) d
 * @author -Wooden-, Tryskell
 */
public final class AnswerJoinPartyRoom extends L2GameClientPacket
{
	private static final String _C__D0_30_ANSWERJOINPARTYROOM = "[C] D0:30 AnswerJoinPartyRoom";
	private int _answer; // 1 or 0
	
	@Override
	public String getType()
	{
		return _C__D0_30_ANSWERJOINPARTYROOM;
	}
	
	@Override
	protected void readImpl()
	{
		_answer = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getActiveChar();
		if (player == null)
		{
			return;
		}
		
		L2PcInstance partner = player.getActiveRequester();
		if (partner == null)
		{
			// Partner hasn't been found, cancel the invitation
			player.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			player.setActiveRequester(null);
			return;
		}
		else if (L2World.getInstance().getPlayer(partner.getObjectId()) == null)
		{
			// Partner hasn't been found, cancel the invitation
			player.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			player.setActiveRequester(null);
			return;
		}
		
		// If answer is positive, join the requester's PartyRoom.
		if ((_answer == 1) && !partner.isRequestExpired())
		{
			PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(partner.getPartyRoom());
			if (room == null)
			{
				return;
			}
			
			if ((player.getLevel() >= room.getMinLvl()) && (player.getLevel() <= room.getMaxLvl()))
			{
				// Remove from waiting list
				PartyMatchWaitingList.getInstance().removePlayer(player);
				
				player.setPartyRoom(partner.getPartyRoom());
				
				player.sendPacket(new PartyMatchDetail(player, room));
				player.sendPacket(new ExPartyRoomMember(player, room, 0));
				
				for (L2PcInstance member : room.getPartyMembers())
				{
					if (member == null)
					{
						continue;
					}
					
					member.sendPacket(new ExManagePartyRoomMember(player, room, 0));
					member.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.C1_ENTERED_PARTY_ROOM).addPcName(player));
				}
				room.addMember(player);
				
				// Info Broadcast
				player.broadcastUserInfo();
			}
			else
			{
				player.sendPacket(SystemMessageId.CANT_ENTER_PARTY_ROOM);
			}
		}
		else
		{
			partner.sendPacket(SystemMessageId.PARTY_MATCHING_REQUEST_NO_RESPONSE);
		}
		
		// reset transaction timers
		player.setActiveRequester(null);
		partner.onTransactionResponse();
	}
}