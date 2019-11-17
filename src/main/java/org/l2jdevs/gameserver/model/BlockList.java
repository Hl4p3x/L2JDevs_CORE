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
package org.l2jdevs.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jdevs.commons.database.pool.impl.ConnectionFactory;
import org.l2jdevs.gameserver.data.sql.impl.CharNameTable;
import org.l2jdevs.gameserver.datatables.LanguageData;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.network.SystemMessageId;
import org.l2jdevs.gameserver.network.serverpackets.SystemMessage;

public class BlockList
{
	private static Logger _log = Logger.getLogger(BlockList.class.getName());
	private static final Map<Integer, List<Integer>> OFFLINE_LIST = new ConcurrentHashMap<>();
	
	private final L2PcInstance _owner;
	private List<Integer> _blockList;
	
	public BlockList(L2PcInstance owner)
	{
		_owner = owner;
		_blockList = OFFLINE_LIST.get(owner.getObjectId());
		if (_blockList == null)
		{
			_blockList = loadList(_owner.getObjectId());
		}
	}
	
	public static void addToBlockList(L2PcInstance listOwner, int targetId)
	{
		if (listOwner == null)
		{
			return;
		}
		
		String charName = CharNameTable.getInstance().getNameById(targetId);
		
		if (listOwner.isFriend(targetId))
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_IN_FRIENDS_LIST);
			sm.addString(charName);
			listOwner.sendPacket(sm);
			return;
		}
		
		if (listOwner.getBlockList().getBlockList().contains(targetId))
		{
			listOwner.sendMessage(LanguageData.getInstance().getMsg(listOwner, "player_already_block"));
			return;
		}
		
		listOwner.getBlockList().addToBlockList(targetId);
		
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_WAS_ADDED_TO_YOUR_IGNORE_LIST);
		sm.addString(charName);
		listOwner.sendPacket(sm);
		
		L2PcInstance player = L2World.getInstance().getPlayer(targetId);
		
		if (player != null)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST);
			sm.addString(listOwner.getName());
			player.sendPacket(sm);
		}
	}
	
	public static boolean isBlocked(L2PcInstance listOwner, int targetId)
	{
		BlockList blockList = listOwner.getBlockList();
		return blockList.isBlockAll() || blockList.isInBlockList(targetId);
	}
	
	public static boolean isBlocked(L2PcInstance listOwner, L2PcInstance target)
	{
		BlockList blockList = listOwner.getBlockList();
		return blockList.isBlockAll() || blockList.isInBlockList(target);
	}
	
	/**
	 * @param ownerId object id of owner block list
	 * @param targetId object id of potential blocked player
	 * @return true if blocked
	 */
	public static boolean isInBlockList(int ownerId, int targetId)
	{
		L2PcInstance player = L2World.getInstance().getPlayer(ownerId);
		if (player != null)
		{
			return BlockList.isBlocked(player, targetId);
		}
		if (!OFFLINE_LIST.containsKey(ownerId))
		{
			OFFLINE_LIST.put(ownerId, loadList(ownerId));
		}
		return OFFLINE_LIST.get(ownerId).contains(targetId);
	}
	
	public static boolean isInBlockList(L2PcInstance listOwner, L2PcInstance target)
	{
		return listOwner.getBlockList().isInBlockList(target);
	}
	
	public static void removeFromBlockList(L2PcInstance listOwner, int targetId)
	{
		if (listOwner == null)
		{
			return;
		}
		
		SystemMessage sm;
		
		String charName = CharNameTable.getInstance().getNameById(targetId);
		
		if (!listOwner.getBlockList().getBlockList().contains(targetId))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT);
			listOwner.sendPacket(sm);
			return;
		}
		
		listOwner.getBlockList().removeFromBlockList(targetId);
		
		sm = SystemMessage.getSystemMessage(SystemMessageId.S1_WAS_REMOVED_FROM_YOUR_IGNORE_LIST);
		sm.addString(charName);
		listOwner.sendPacket(sm);
	}
	
	public static void sendListToOwner(L2PcInstance listOwner)
	{
		int i = 1;
		listOwner.sendPacket(SystemMessageId.BLOCK_LIST_HEADER);
		for (int playerId : listOwner.getBlockList().getBlockList())
		{
			listOwner.sendMessage((i++) + ". " + CharNameTable.getInstance().getNameById(playerId));
		}
		listOwner.sendPacket(SystemMessageId.FRIEND_LIST_FOOTER);
	}
	
	public static void setBlockAll(L2PcInstance listOwner, boolean newValue)
	{
		listOwner.getBlockList().setBlockAll(newValue);
	}
	
	private static List<Integer> loadList(int ObjId)
	{
		List<Integer> list = new ArrayList<>();
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT friendId FROM character_friends WHERE charId=? AND relation=1"))
		{
			ps.setInt(1, ObjId);
			try (ResultSet rs = ps.executeQuery())
			{
				int friendId;
				while (rs.next())
				{
					friendId = rs.getInt("friendId");
					if (friendId == ObjId)
					{
						continue;
					}
					list.add(friendId);
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error found in " + ObjId + " FriendList while loading BlockList: " + e.getMessage(), e);
		}
		return list;
	}
	
	public boolean isBlockAll(L2PcInstance listOwner)
	{
		return listOwner.getBlockList().isBlockAll();
	}
	
	public boolean isInBlockList(int targetId)
	{
		return _blockList.contains(targetId);
	}
	
	public boolean isInBlockList(L2PcInstance target)
	{
		return _blockList.contains(target.getObjectId());
	}
	
	public void playerLogout()
	{
		OFFLINE_LIST.put(_owner.getObjectId(), _blockList);
	}
	
	private void addToBlockList(int target)
	{
		_blockList.add(target);
		persistInDB(target);
	}
	
	private List<Integer> getBlockList()
	{
		return _blockList;
	}
	
	private boolean isBlockAll()
	{
		return _owner.getMessageRefusal();
	}
	
	private void persistInDB(int targetId)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO character_friends (charId, friendId, relation) VALUES (?, ?, 1)"))
		{
			ps.setInt(1, _owner.getObjectId());
			ps.setInt(2, targetId);
			ps.execute();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not add blocked player: " + e.getMessage(), e);
		}
	}
	
	private void removeFromBlockList(int target)
	{
		_blockList.remove(Integer.valueOf(target));
		removeFromDB(target);
	}
	
	private void removeFromDB(int targetId)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM character_friends WHERE charId=? AND friendId=? AND relation=1"))
		{
			ps.setInt(1, _owner.getObjectId());
			ps.setInt(2, targetId);
			ps.execute();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not remove blocked player: " + e.getMessage(), e);
		}
	}
	
	private void setBlockAll(boolean state)
	{
		_owner.setMessageRefusal(state);
	}
}
