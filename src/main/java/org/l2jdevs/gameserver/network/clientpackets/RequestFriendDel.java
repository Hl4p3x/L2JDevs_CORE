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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;

import org.l2jdevs.commons.database.pool.impl.ConnectionFactory;
import org.l2jdevs.gameserver.data.sql.impl.CharNameTable;
import org.l2jdevs.gameserver.model.L2World;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.network.SystemMessageId;
import org.l2jdevs.gameserver.network.serverpackets.FriendPacket;
import org.l2jdevs.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestFriendDel extends L2GameClientPacket
{
	
	private static final String _C__7A_REQUESTFRIENDDEL = "[C] 7A RequestFriendDel";
	
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
	}
	
	@Override
	protected void runImpl()
	{
		SystemMessage sm;
		
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		int id = CharNameTable.getInstance().getIdByName(_name);
		
		if (id == -1)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_NOT_ON_YOUR_FRIENDS_LIST);
			sm.addString(_name);
			activeChar.sendPacket(sm);
			return;
		}
		
		if (!activeChar.isFriend(id))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_NOT_ON_YOUR_FRIENDS_LIST);
			sm.addString(_name);
			activeChar.sendPacket(sm);
			return;
		}
		
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_friends WHERE (charId=? AND friendId=?) OR (charId=? AND friendId=?)"))
		{
			statement.setInt(1, activeChar.getObjectId());
			statement.setInt(2, id);
			statement.setInt(3, id);
			statement.setInt(4, activeChar.getObjectId());
			statement.execute();
			
			// Player deleted from your friend list
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST);
			sm.addString(_name);
			activeChar.sendPacket(sm);
			
			activeChar.removeFriend(id);
			activeChar.sendPacket(new FriendPacket(false, id));
			
			final L2PcInstance friend = L2World.getInstance().getPlayer(_name);
			if (friend != null)
			{
				friend.removeFriend(activeChar.getObjectId());
				friend.sendPacket(new FriendPacket(false, activeChar.getObjectId()));
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not del friend objectid: ", e);
		}
	}
	
	@Override
	public String getType()
	{
		return _C__7A_REQUESTFRIENDDEL;
	}
}