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

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.SevenSignsFestival;
import org.l2jdevs.gameserver.datatables.LanguageData;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.entity.L2Event;
import org.l2jdevs.gameserver.network.SystemMessageId;
import org.l2jdevs.gameserver.network.serverpackets.ActionFailed;
import org.l2jdevs.gameserver.network.serverpackets.SystemMessage;
import org.l2jdevs.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * This class ...
 * @version $Revision: 1.9.4.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class Logout extends L2GameClientPacket
{
	private static final String _C__00_LOGOUT = "[C] 00 Logout";
	protected static final Logger _logAccounting = Logger.getLogger("accounting");
	
	@Override
	protected void readImpl()
	{
		
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if ((player.getActiveEnchantItemId() != L2PcInstance.ID_NONE) || (player.getActiveEnchantAttrItemId() != L2PcInstance.ID_NONE))
		{
			if (Config.DEBUG)
			{
				_log.fine("Player " + player.getName() + " tried to logout while enchanting.");
			}
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isLocked())
		{
			_log.warning("Player " + player.getName() + " tried to logout during class change.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Don't allow leaving if player is fighting
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player))
		{
			if (player.isGM() && Config.GM_RESTART_FIGHTING)
			{
				return;
			}
			
			if (Config.DEBUG)
			{
				_log.fine("Player " + player.getName() + " tried to logout while fighting.");
			}
			
			player.sendPacket(SystemMessageId.CANT_LOGOUT_WHILE_FIGHTING);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (L2Event.isParticipant(player))
		{
			player.sendMessage(LanguageData.getInstance().getMsg(player, "event_no_logout"));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Prevent player from logging out if they are a festival participant
		// and it is in progress, otherwise notify party members that the player
		// is not longer a participant.
		if (player.isFestivalParticipant())
		{
			if (SevenSignsFestival.getInstance().isFestivalInitialized())
			{
				player.sendMessage(LanguageData.getInstance().getMsg(player, "ss_no_logout_player"));
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (player.isInParty())
			{
				player.getParty().broadcastPacket(SystemMessage.sendString(player.getName() + " has been removed from the upcoming Festival."));
			}
		}
		
		// Remove player from Boss Zone
		player.removeFromBossZone();
		
		LogRecord record = new LogRecord(Level.INFO, "Disconnected");
		record.setParameters(new Object[]
		{
			getClient()
		});
		_logAccounting.log(record);
		
		player.logout();
	}
	
	@Override
	public String getType()
	{
		return _C__00_LOGOUT;
	}
}