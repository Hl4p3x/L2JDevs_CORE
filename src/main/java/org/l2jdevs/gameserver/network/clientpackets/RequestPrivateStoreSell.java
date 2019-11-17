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

import static org.l2jdevs.gameserver.model.actor.L2Npc.INTERACTION_DISTANCE;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.datatables.LanguageData;
import org.l2jdevs.gameserver.enums.PrivateStoreType;
import org.l2jdevs.gameserver.model.ItemRequest;
import org.l2jdevs.gameserver.model.L2World;
import org.l2jdevs.gameserver.model.TradeList;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.network.serverpackets.ActionFailed;

/**
 * This class ...
 * @version $Revision: 1.2.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestPrivateStoreSell extends L2GameClientPacket
{
	private static final String _C__9F_REQUESTPRIVATESTORESELL = "[C] 9F RequestPrivateStoreSell";
	
	private static final int BATCH_LENGTH = 28; // length of the one item
	
	private int _storePlayerId;
	private ItemRequest[] _items = null;
	
	@Override
	public String getType()
	{
		return _C__9F_REQUESTPRIVATESTORESELL;
	}
	
	@Override
	protected void readImpl()
	{
		_storePlayerId = readD();
		int count = readD();
		if ((count <= 0) || (count > Config.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != _buf.remaining()))
		{
			return;
		}
		_items = new ItemRequest[count];
		
		for (int i = 0; i < count; i++)
		{
			int objectId = readD();
			int itemId = readD();
			readH(); // TODO analyse this
			readH(); // TODO analyse this
			long cnt = readQ();
			long price = readQ();
			
			if ((objectId < 1) || (itemId < 1) || (cnt < 1) || (price < 0))
			{
				_items = null;
				return;
			}
			_items[i] = new ItemRequest(objectId, itemId, cnt, price);
		}
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (_items == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!getClient().getFloodProtectors().getTransaction().tryPerformAction("privatestoresell"))
		{
			player.sendMessage(LanguageData.getInstance().getMsg(player, "selling_too_fast"));
			return;
		}
		
		L2PcInstance object = L2World.getInstance().getPlayer(_storePlayerId);
		if (object == null)
		{
			return;
		}
		
		L2PcInstance storePlayer = object;
		if (!player.isInsideRadius(storePlayer, INTERACTION_DISTANCE, true, false))
		{
			return;
		}
		
		if ((player.getInstanceId() != storePlayer.getInstanceId()) && (player.getInstanceId() != -1))
		{
			return;
		}
		
		if (storePlayer.getPrivateStoreType() != PrivateStoreType.BUY)
		{
			return;
		}
		
		if (player.isCursedWeaponEquipped())
		{
			return;
		}
		
		TradeList storeList = storePlayer.getBuyList();
		if (storeList == null)
		{
			return;
		}
		
		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage(LanguageData.getInstance().getMsg(player, "transactions_disabled"));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!storeList.privateStoreSell(player, _items))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			_log.warning("PrivateStore sell has failed due to invalid list or request. Player: " + player.getName() + ", Private store of: " + storePlayer.getName());
			return;
		}
		
		if (storeList.getItemCount() == 0)
		{
			storePlayer.setPrivateStoreType(PrivateStoreType.NONE);
			storePlayer.broadcastUserInfo();
		}
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}
