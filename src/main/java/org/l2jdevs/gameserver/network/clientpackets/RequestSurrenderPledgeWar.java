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

import org.l2jdevs.gameserver.data.sql.impl.ClanTable;
import org.l2jdevs.gameserver.datatables.LanguageData;
import org.l2jdevs.gameserver.model.L2Clan;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.network.SystemMessageId;
import org.l2jdevs.gameserver.network.serverpackets.ActionFailed;
import org.l2jdevs.gameserver.network.serverpackets.SystemMessage;

public final class RequestSurrenderPledgeWar extends L2GameClientPacket
{
	private static final String _C__07_REQUESTSURRENDERPLEDGEWAR = "[C] 07 RequestSurrenderPledgeWar";
	
	private String _pledgeName;
	
	@Override
	protected void readImpl()
	{
		_pledgeName = readS();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		L2Clan _clan = activeChar.getClan();
		if (_clan == null)
		{
			return;
		}
		L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);
		
		if (clan == null)
		{
			activeChar.sendMessage(LanguageData.getInstance().getMsg(activeChar, "clan_no_such"));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		_log.info("RequestSurrenderPledgeWar by " + getClient().getActiveChar().getClan().getName() + " with " + _pledgeName);
		
		if (!_clan.isAtWarWith(clan.getId()))
		{
			activeChar.sendMessage(LanguageData.getInstance().getMsg(activeChar, "clan_no_war"));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_SURRENDERED_TO_THE_S1_CLAN);
		msg.addString(_pledgeName);
		activeChar.sendPacket(msg);
		ClanTable.getInstance().deleteclanswars(_clan.getId(), clan.getId());
		// Zoey76: TODO: Implement or cleanup.
		// L2PcInstance leader = L2World.getInstance().getPlayer(clan.getLeaderName());
		// if ((leader != null) && (leader.isOnline() == 0))
		// {
		// player.sendMessage("Clan leader isn't online.");
		// player.sendPacket(ActionFailed.STATIC_PACKET);
		// return;
		// }
		//
		// if (leader.isTransactionInProgress())
		// {
		// SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER);
		// sm.addString(leader.getName());
		// player.sendPacket(sm);
		// return;
		// }
		//
		// leader.setTransactionRequester(player);
		// player.setTransactionRequester(leader);
		// leader.sendPacket(new SurrenderPledgeWar(_clan.getName(), player.getName()));
	}
	
	@Override
	public String getType()
	{
		return _C__07_REQUESTSURRENDERPLEDGEWAR;
	}
}